package xadrez.ui;

import xadrez.regras.Posicao;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Tabuleiro gráfico.
 * <ul>
 *   <li>Botão esquerdo: seleciona e move peças (mostrando os movimentos possíveis) e apaga as setas.</li>
 *   <li>Botão direito (arrastar): desenha uma seta; clicar numa casa faz um círculo; repetir apaga.
 *       As setas são só desenho: nunca afetam o jogo.</li>
 *   <li>Rei em xeque: casa avermelhada; xeque-mate: casa vermelha forte com borda.</li>
 * </ul>
 */
public class PainelTabuleiro extends JPanel {

    public interface InteracaoCallback {
        void aoTentarJogar(int lance);
    }

    /** Seta de análise (ex.: melhor lance em verde, lance jogado na cor da classificação). */
    public static final class SetaExtra {
        final String uci;
        final Color cor;

        public SetaExtra(String uci, Color cor) {
            this.uci = uci;
            this.cor = cor;
        }
    }

    private static final Color COR_SELECAO = new Color(60, 220, 110, 150);
    private static final Color COR_SETA = new Color(255, 160, 0, 205), COR_PREVIA = new Color(255, 160, 0, 115);
    private static final Color COR_SUGESTAO = new Color(0, 150, 255, 160);
    private static final Color COR_MARCADOR = new Color(0, 0, 0, 80);
    private static final int MARGEM = 64;

    private Posicao posicao = Posicao.inicial();
    private TemaTabuleiro tema = TemaTabuleiro.AZUL;
    private boolean invertido;
    private int selecionada = -1;
    private int casaReiEmXeque = -1;
    private boolean xequeMate;
    private final List<int[]> setasUsuario = new ArrayList<>(); // {origem, destino}; origem == destino = círculo
    private int setaOrigem = -1, setaAtual = -1;                // seta em construção (botão direito pressionado)
    private String sugestaoUci;
    private List<SetaExtra> extras = new ArrayList<>();
    private InteracaoCallback callback;
    private BooleanSupplier jogadaPermitida = () -> true;

    public PainelTabuleiro() {
        setBackground(new Color(0x16181D));
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (botaoDireito(e)) {
                    setaOrigem = setaAtual = casaEm(e.getX(), e.getY());
                    repaint();
                } else if (e.getButton() == MouseEvent.BUTTON1) {
                    if (casaEm(e.getX(), e.getY()) >= 0) limparSetas(); // clique esquerdo no tabuleiro remove as setas
                    clique(e.getX(), e.getY());
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (setaOrigem >= 0) {
                    setaAtual = casaEm(e.getX(), e.getY());
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!botaoDireito(e) || setaOrigem < 0) return;
                int destino = casaEm(e.getX(), e.getY());
                if (destino >= 0) alternarSeta(setaOrigem, destino);
                setaOrigem = setaAtual = -1;
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private static boolean botaoDireito(MouseEvent e) {
        return e.getButton() == MouseEvent.BUTTON3 || SwingUtilities.isRightMouseButton(e);
    }

    // ------------------------------------------------------------ API pública

    public void atualizarMundo(Posicao novaPosicao, boolean tabuleiroInvertido) {
        this.posicao = novaPosicao;
        this.invertido = tabuleiroInvertido;
        this.selecionada = -1;
        boolean xeque = novaPosicao.emXeque();
        this.casaReiEmXeque = xeque ? novaPosicao.casaRei(novaPosicao.brancas) : -1;
        this.xequeMate = xeque && novaPosicao.lancesLegais().length == 0;
        repaint();
    }

    public void setTema(TemaTabuleiro novoTema) {
        this.tema = novoTema;
        repaint();
    }

    /** Seta azul com o melhor lance sugerido pela análise (ou null para esconder). */
    public void setSugestao(String uci) {
        this.sugestaoUci = uci;
        repaint();
    }

    /** Setas de análise desenhadas por baixo das setas do usuário. */
    public void setSetasExtras(List<SetaExtra> novas) {
        this.extras = new ArrayList<>(novas);
        repaint();
    }

    public void setCallback(InteracaoCallback callback) { this.callback = callback; }

    /** Define quando o usuário pode mover peças com o botão esquerdo (ex.: não na vez do computador). */
    public void setJogadaPermitida(BooleanSupplier jogadaPermitida) { this.jogadaPermitida = jogadaPermitida; }

    public void limparSetas() {
        setasUsuario.clear();
        setaOrigem = setaAtual = -1;
        repaint();
    }

    /** Centro da casa (0..63) em coordenadas do painel (útil para testes). */
    public Point pontoDaCasa(int casa) {
        double[] c = Setas.centro(casa, bx(), by(), S(), invertido);
        return new Point((int) c[0], (int) c[1]);
    }

    // ------------------------------------------------------------ geometria

    private int S() { return Math.max(16, (Math.min(getWidth(), getHeight()) - MARGEM) / 8); }

    private int bx() { return (getWidth() - 8 * S()) / 2; }

    private int by() { return (getHeight() - 8 * S()) / 2; }

    private int casaEm(int x, int y) {
        int s = S();
        int c = Math.floorDiv(x - bx(), s), r = Math.floorDiv(y - by(), s);
        if (c < 0 || c > 7 || r < 0 || r > 7) return -1;
        return (invertido ? 7 - r : r) * 8 + (invertido ? 7 - c : c);
    }

    // ------------------------------------------------------------ interação

    private void clique(int x, int y) {
        int casa = casaEm(x, y);
        if (casa < 0) return;
        if (callback == null || !jogadaPermitida.getAsBoolean()) {
            selecionada = -1;
            repaint();
            return;
        }
        if (selecionada >= 0) {
            List<Integer> candidatos = new ArrayList<>();
            for (int m : posicao.lancesLegais()) {
                if ((m & 63) == selecionada && ((m >> 6) & 63) == casa) candidatos.add(m);
            }
            if (!candidatos.isEmpty()) {
                int lance = escolherPromocao(candidatos);
                selecionada = -1;
                repaint();
                callback.aoTentarJogar(lance);
                return;
            }
        }
        int peca = posicao.b[casa];
        selecionada = (peca != 0 && (peca > 0) == posicao.brancas) ? casa : -1;
        repaint();
    }

    /** Se houver várias promoções possíveis, pergunta qual peça o usuário quer. */
    private int escolherPromocao(List<Integer> candidatos) {
        if (candidatos.size() == 1) return candidatos.get(0);
        String[] nomes = {"Dama", "Torre", "Bispo", "Cavalo"};
        int[] tipos = {Posicao.Q, Posicao.R, Posicao.B, Posicao.N};
        int r = JOptionPane.showOptionDialog(this, "Promover o peão para:", "Promoção",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, nomes, nomes[0]);
        int tipo = r < 0 ? Posicao.Q : tipos[r];
        for (int m : candidatos) if ((m >> 12) == tipo) return m;
        return candidatos.get(0);
    }

    /** Desenhar a mesma seta de novo a remove (como no chess.com). */
    private void alternarSeta(int de, int para) {
        for (int i = 0; i < setasUsuario.size(); i++) {
            int[] a = setasUsuario.get(i);
            if (a[0] == de && a[1] == para) {
                setasUsuario.remove(i);
                return;
            }
        }
        setasUsuario.add(new int[]{de, para});
    }

    // ------------------------------------------------------------ desenho

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int s = S(), bx = bx(), by = by();

        // Movimentos possíveis da peça selecionada
        Set<Integer> destinos = new HashSet<>(), capturas = new HashSet<>();
        if (selecionada >= 0) {
            for (int m : posicao.lancesLegais()) {
                if ((m & 63) != selecionada) continue;
                destinos.add((m >> 6) & 63);
                if (posicao.isCaptura(m)) capturas.add((m >> 6) & 63);
            }
        }

        for (int sq = 0; sq < 64; sq++) {
            int r = sq >> 3, c = sq & 7;
            int x = bx + (invertido ? 7 - c : c) * s, y = by + (invertido ? 7 - r : r) * s;
            g.setColor((r + c) % 2 == 1 ? tema.escura : tema.clara);
            g.fillRect(x, y, s, s);
            if (sq == selecionada) {
                g.setColor(COR_SELECAO);
                g.fillRect(x, y, s, s);
            }
            if (sq == casaReiEmXeque) pintarXeque(g, x, y, s);

            int p = posicao.b[sq];
            if (p != 0) Pecas.desenhar(g, Math.abs(p), p > 0, x, y, s);

            if (destinos.contains(sq)) {
                g.setColor(COR_MARCADOR);
                if (capturas.contains(sq)) { // anel em volta da peça que pode ser capturada
                    g.setStroke(new BasicStroke(Math.max(3f, s / 14f)));
                    g.drawOval(x + 4, y + 4, s - 8, s - 8);
                } else {                     // bolinha na casa vazia
                    int raio = s / 7;
                    g.fillOval(x + s / 2 - raio, y + s / 2 - raio, 2 * raio, 2 * raio);
                }
            }
        }

        if (sugestaoUci != null && sugestaoUci.length() >= 4) {
            int de = (8 - (sugestaoUci.charAt(1) - '0')) * 8 + (sugestaoUci.charAt(0) - 'a');
            int para = (8 - (sugestaoUci.charAt(3) - '0')) * 8 + (sugestaoUci.charAt(2) - 'a');
            Setas.desenhar(g, de, para, COR_SUGESTAO, bx, by, s, invertido);
        }
        for (SetaExtra e : extras) {
            if (e.uci == null || e.uci.length() < 4) continue;
            int de = (8 - (e.uci.charAt(1) - '0')) * 8 + (e.uci.charAt(0) - 'a');
            int para = (8 - (e.uci.charAt(3) - '0')) * 8 + (e.uci.charAt(2) - 'a');
            Setas.desenhar(g, de, para, e.cor, bx, by, s, invertido);
        }
        for (int[] a : setasUsuario) Setas.desenhar(g, a[0], a[1], COR_SETA, bx, by, s, invertido);
        if (setaOrigem >= 0 && setaAtual >= 0) Setas.desenhar(g, setaOrigem, setaAtual, COR_PREVIA, bx, by, s, invertido);
    }

    /** Xeque: brilho vermelho na casa do rei. Xeque-mate: casa vermelha forte com borda escura. */
    private void pintarXeque(Graphics2D g, int x, int y, int s) {
        if (xequeMate) {
            g.setColor(new Color(200, 0, 0, 215));
            g.fillRect(x, y, s, s);
            g.setColor(new Color(110, 0, 0));
            g.setStroke(new BasicStroke(Math.max(3f, s / 16f)));
            g.drawRect(x + 2, y + 2, s - 4, s - 4);
        } else {
            g.setPaint(new RadialGradientPaint(x + s / 2f, y + s / 2f, s * 0.62f, new float[]{0f, 0.7f, 1f},
                    new Color[]{new Color(255, 40, 40, 230), new Color(255, 40, 40, 140), new Color(255, 40, 40, 0)}));
            g.fillRect(x, y, s, s);
        }
    }
}
