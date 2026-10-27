package xadrez.ui;

import xadrez.analise.AnalisadorPartida;
import xadrez.analise.Classificacao;
import xadrez.analise.NivelAnalise;
import xadrez.analise.QualidadeLances;
import xadrez.motor.ResultadoBusca;
import xadrez.pgn.PartidaPgn;
import xadrez.regras.Material;
import xadrez.regras.Posicao;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tela de análise de uma partida: tabuleiro real com navegação lance a lance, gráfico de avaliação,
 * classificação e explicação de cada lance, resumo de precisão e "jogar a partir daqui".
 *
 * <p>O tabuleiro mostra a posição ANTES do lance selecionado, com setas do lance jogado e do melhor lance.
 */
public class PainelRevisao extends JPanel {

    public enum ModoContinuacao { BRANCAS_VS_COMPUTADOR, PRETAS_VS_COMPUTADOR, PVP }

    /** Chamada quando o usuário escolhe continuar jogando a partir de uma posição. */
    public interface AcaoContinuar {
        void continuar(List<String> lancesUci, int ate, ModoContinuacao modo, Map<Integer, Integer> avaliacoes);
    }

    private static final Color FUNDO = new Color(0x1F2229), SUAVE = new Color(0x9AA1AE);
    private static final Color COR_MELHOR = new Color(0x35C25A);

    private final PartidaPgn partida;
    private final AnalisadorPartida analisador;
    private final AcaoContinuar acaoContinuar;
    private final Runnable aoFechar;
    private final int total;

    private final PainelTabuleiro tabuleiro = new PainelTabuleiro();
    private final GraficoAvaliacao grafico;
    private final JList<Integer> lista = new JList<>();
    private final JLabel lblResumo = new JLabel(" ");
    private final JLabel lblTitulo = new JLabel(" ");
    private final JLabel lblAvaliacao = new JLabel(" ");
    private final JLabel lblMaterial = new JLabel(" ");
    private final JLabel lblProgresso = new JLabel(" ");
    private final JTextArea txtExplicacao = new JTextArea();
    private final JProgressBar barra = new JProgressBar();
    private final JCheckBox chkMelhor = new JCheckBox("Seta do melhor lance (verde)", true);
    private final JCheckBox chkJogado = new JCheckBox("Seta do lance jogado", true);

    private int atual;
    private boolean invertido;
    private boolean sincronizandoLista;

    public PainelRevisao(PartidaPgn partida, NivelAnalise nivel, AcaoContinuar acaoContinuar, Runnable aoFechar) {
        super(new BorderLayout());
        this.partida = partida;
        this.acaoContinuar = acaoContinuar;
        this.aoFechar = aoFechar;
        this.analisador = new AnalisadorPartida(partida.getLancesUci());
        this.total = analisador.totalLances();
        this.grafico = new GraficoAvaliacao(analisador, this::selecionar);
        setBackground(FUNDO);

        tabuleiro.setJogadaPermitida(() -> false); // aqui o tabuleiro só mostra; jogar é em "Jogar a partir daqui"

        add(criarBarraSuperior(), BorderLayout.NORTH);
        add(tabuleiro, BorderLayout.CENTER);
        add(criarPainelLateral(), BorderLayout.EAST);
        add(criarBarraProgresso(), BorderLayout.SOUTH);
        configurarAtalhos();

        selecionar(0);
        barra.setMaximum(total + 1);
        analisador.iniciar(nivel, SwingUtilities::invokeLater, new AnalisadorPartida.Ouvinte() {
            @Override
            public void aoAvaliar(int concluidas, int totalPosicoes) {
                barra.setValue(concluidas);
                lblProgresso.setText("Analisando posições... " + concluidas + " de " + totalPosicoes);
                atualizarTudo();
            }

            @Override
            public void aoConcluir() {
                barra.setVisible(false);
                lblProgresso.setText("Análise concluída. Use as setas ← → do teclado para navegar.");
                atualizarTudo();
            }
        });
    }

    /** Interrompe a análise em andamento (chamar ao fechar a janela). */
    public void encerrar() {
        analisador.cancelar();
    }

    public AnalisadorPartida getAnalisador() { return analisador; }

    // ------------------------------------------------------------ navegação

    public void selecionar(int posicao) {
        atual = Math.max(0, Math.min(total, posicao));
        atualizarTudo();
    }

    public int getPosicaoAtual() { return atual; }

    private void atualizarTudo() {
        tabuleiro.atualizarMundo(analisador.posicao(atual), invertido);
        tabuleiro.setSetasExtras(montarSetas());
        grafico.setAtual(atual);
        atualizarCartao();
        atualizarResumo();
        Posicao pos = analisador.posicao(atual);
        Material m = Material.calcular(pos);
        lblMaterial.setText(m.saldo() == 0 ? "Material igual" : (m.saldo() > 0 ? "Brancas +" : "Pretas +") + Math.abs(m.saldo())
                + "  (" + m.pontosBrancas + " × " + m.pontosPretas + ")");
        sincronizandoLista = true;
        lista.setSelectedIndex(atual);
        lista.ensureIndexIsVisible(atual);
        lista.repaint();
        sincronizandoLista = false;
    }

    private List<PainelTabuleiro.SetaExtra> montarSetas() {
        List<PainelTabuleiro.SetaExtra> setas = new ArrayList<>();
        if (atual >= total) return setas;
        String jogado = analisador.lancesUci().get(atual);
        ResultadoBusca r = analisador.resultado(atual);
        String melhor = r == null ? null : r.melhorUci;
        Classificacao c = analisador.classificar(atual);
        boolean igual = jogado.equals(melhor);
        if (chkMelhor.isSelected() && melhor != null && !igual) setas.add(new PainelTabuleiro.SetaExtra(melhor, withAlpha(COR_MELHOR, 200)));
        if (chkJogado.isSelected()) {
            Color cor = igual ? COR_MELHOR : c == null ? new Color(255, 160, 0) : new Color(c.rgb);
            setas.add(new PainelTabuleiro.SetaExtra(jogado, withAlpha(cor, 210)));
        }
        return setas;
    }

    private static Color withAlpha(Color c, int alfa) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alfa);
    }

    // ------------------------------------------------------------ cartão do lance

    private void atualizarCartao() {
        if (atual >= total) {
            lblTitulo.setText("Fim da partida");
            lblTitulo.setForeground(Color.WHITE);
            Posicao fim = analisador.posicao(total);
            String motivo = fim.lancesLegais().length == 0 ? (fim.emXeque() ? "Xeque-mate." : "Afogamento.") : "";
            lblAvaliacao.setText("Resultado: " + partida.getResultado());
            txtExplicacao.setText(motivo + (motivo.isEmpty() ? "" : "\n\n") + "Volte com ← para rever os lances, ou clique em \"Jogar a partir daqui\" para continuar de qualquer posição.");
            return;
        }
        String numero = (atual / 2 + 1) + (atual % 2 == 0 ? ". " : "... ");
        Classificacao c = analisador.classificar(atual);
        if (c == null) {
            lblTitulo.setText("Lance " + numero + analisador.sanPt(atual));
            lblTitulo.setForeground(Color.WHITE);
            lblAvaliacao.setText(" ");
            txtExplicacao.setText("Este lance ainda está sendo avaliado...");
            return;
        }
        lblTitulo.setText(numero + analisador.sanPt(atual) + c.simbolo + "  —  " + c.rotulo);
        lblTitulo.setForeground(new Color(c.rgb));
        Integer antes = analisador.avaliacao(atual), depois = analisador.avaliacao(atual + 1);
        String melhor = analisador.melhorSan(atual);
        lblAvaliacao.setText("Avaliação (brancas): " + QualidadeLances.formatar(antes) + " → " + QualidadeLances.formatar(depois)
                + (melhor != null && !melhor.equals(analisador.sanPt(atual)) ? "   |   Melhor: " + melhor : ""));
        txtExplicacao.setText(analisador.explicar(atual));
        txtExplicacao.setCaretPosition(0);
    }

    private void atualizarResumo() {
        StringBuilder sb = new StringBuilder("<html><table cellspacing='0' cellpadding='1'>");
        sb.append("<tr><td></td><td><b>&nbsp;Brancas</b></td><td><b>&nbsp;Pretas</b></td></tr>");
        sb.append("<tr><td>Precisão (estimada)&nbsp;</td><td>&nbsp;").append(porcento(analisador.precisao(true)))
                .append("</td><td>&nbsp;").append(porcento(analisador.precisao(false))).append("</td></tr>");
        int[] b = analisador.contagem(true), p = analisador.contagem(false);
        for (Classificacao c : Classificacao.values()) {
            if (c == Classificacao.BOM) continue;
            sb.append("<tr><td><font color='#").append(String.format("%06X", c.rgb)).append("'>").append(c.rotulo)
                    .append("</font></td><td>&nbsp;").append(b[c.ordinal()]).append("</td><td>&nbsp;").append(p[c.ordinal()]).append("</td></tr>");
        }
        sb.append("</table></html>");
        lblResumo.setText(sb.toString());
    }

    private static String porcento(double v) {
        return Double.isNaN(v) ? "—" : String.format("%.1f%%", v);
    }

    // ------------------------------------------------------------ jogar a partir daqui

    /** Pergunta como continuar e abre o jogo na posição atual. */
    private void perguntarEContinuar() {
        boolean vezBrancas = analisador.posicao(atual).brancas;
        String[] opcoes = {"Jogar de brancas contra o computador", "Jogar de pretas contra o computador", "Dois jogadores (PvP)", "Cancelar"};
        int r = JOptionPane.showOptionDialog(this, "Continuar a partida a partir desta posição (depois de " + atual + " lances):",
                "Jogar a partir daqui", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opcoes, opcoes[vezBrancas ? 0 : 1]);
        if (r == 0) continuarCom(ModoContinuacao.BRANCAS_VS_COMPUTADOR);
        else if (r == 1) continuarCom(ModoContinuacao.PRETAS_VS_COMPUTADOR);
        else if (r == 2) continuarCom(ModoContinuacao.PVP);
    }

    /** Abre o jogo na posição atual (reaproveita as avaliações já calculadas) e fecha a análise. */
    public void continuarCom(ModoContinuacao modo) {
        Map<Integer, Integer> avaliacoes = new HashMap<>();
        for (int i = 0; i <= atual; i++) {
            Integer cp = analisador.avaliacao(i);
            if (cp != null) avaliacoes.put(i, cp);
        }
        List<String> lances = new ArrayList<>(analisador.lancesUci().subList(0, atual));
        encerrar();
        acaoContinuar.continuar(lances, atual, modo, avaliacoes);
        if (aoFechar != null) aoFechar.run();
    }

    // ------------------------------------------------------------ montagem da interface

    private JComponent criarBarraSuperior() {
        JPanel barraSup = new JPanel(new BorderLayout());
        barraSup.setBackground(FUNDO);
        JLabel cabecalho = new JLabel(partida.getBrancas() + " (brancas)  x  " + partida.getPretas() + " (pretas)   —   Resultado "
                + partida.getResultado() + "   —   " + partida.tag("Date", "").replace('.', '/'));
        cabecalho.setForeground(Color.WHITE);
        cabecalho.setFont(new Font("SansSerif", Font.BOLD, 15));
        cabecalho.setBorder(BorderFactory.createEmptyBorder(8, 12, 2, 12));
        barraSup.add(cabecalho, BorderLayout.NORTH);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        botoes.setBackground(FUNDO);
        botoes.add(botao("|<", () -> selecionar(0)));
        botoes.add(botao("<  Anterior", () -> selecionar(atual - 1)));
        botoes.add(botao("Próximo  >", () -> selecionar(atual + 1)));
        botoes.add(botao(">|", () -> selecionar(total)));
        botoes.add(botao("Girar tabuleiro", () -> { invertido = !invertido; atualizarTudo(); }));
        for (JCheckBox c : new JCheckBox[]{chkMelhor, chkJogado}) {
            c.setOpaque(false);
            c.setForeground(Color.LIGHT_GRAY);
            c.setFocusable(false);
            c.addActionListener(e -> tabuleiro.setSetasExtras(montarSetas()));
            botoes.add(c);
        }
        botoes.add(botao("Jogar a partir daqui", this::perguntarEContinuar));
        botoes.add(botao("Fechar", () -> { encerrar(); if (aoFechar != null) aoFechar.run(); }));
        barraSup.add(botoes, BorderLayout.CENTER);
        return barraSup;
    }

    private JComponent criarPainelLateral() {
        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setBackground(FUNDO);
        painel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        painel.setPreferredSize(new Dimension(430, 0));

        grafico.setAlignmentX(Component.LEFT_ALIGNMENT);
        painel.add(secao("AVALIAÇÃO (clique para navegar)"));
        painel.add(grafico);
        painel.add(Box.createVerticalStrut(8));

        painel.add(secao("RESUMO"));
        estilo(lblResumo, 13, false, Color.WHITE);
        painel.add(lblResumo);
        painel.add(Box.createVerticalStrut(8));

        painel.add(secao("LANCE SELECIONADO"));
        estilo(lblTitulo, 17, true, Color.WHITE);
        estilo(lblAvaliacao, 12, false, SUAVE);
        estilo(lblMaterial, 12, false, SUAVE);
        painel.add(lblTitulo);
        painel.add(lblAvaliacao);
        txtExplicacao.setEditable(false);
        txtExplicacao.setLineWrap(true);
        txtExplicacao.setWrapStyleWord(true);
        txtExplicacao.setBackground(new Color(0x15171C));
        txtExplicacao.setForeground(Color.LIGHT_GRAY);
        txtExplicacao.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtExplicacao.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane rolagemTexto = new JScrollPane(txtExplicacao);
        rolagemTexto.setAlignmentX(Component.LEFT_ALIGNMENT);
        rolagemTexto.setPreferredSize(new Dimension(100, 190));
        rolagemTexto.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        painel.add(Box.createVerticalStrut(4));
        painel.add(rolagemTexto);
        painel.add(lblMaterial);
        painel.add(Box.createVerticalStrut(8));

        painel.add(secao("LANCES   (!! brilhante  ! melhor  ?! imprecisão  ? erro  ?? gafe)"));
        DefaultListModel<Integer> modelo = new DefaultListModel<>();
        for (int i = 0; i <= total; i++) modelo.addElement(i);
        lista.setModel(modelo);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setBackground(new Color(0x15171C));
        lista.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        lista.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object valor, int indice, boolean selecionado, boolean foco) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, linha(indice), indice, selecionado, foco);
                c.setBackground(selecionado ? new Color(0x3A4A66) : new Color(0x15171C));
                Classificacao cl = indice < total ? analisador.classificar(indice) : null;
                boolean notavel = cl != null && (cl.eRuim() || cl == Classificacao.BRILHANTE);
                c.setForeground(notavel ? new Color(cl.rgb) : Color.LIGHT_GRAY);
                return c;
            }
        });
        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !sincronizandoLista && lista.getSelectedIndex() >= 0) selecionar(lista.getSelectedIndex());
        });
        JScrollPane rolagemLista = new JScrollPane(lista);
        rolagemLista.setAlignmentX(Component.LEFT_ALIGNMENT);
        painel.add(rolagemLista);
        return painel;
    }

    private String linha(int i) {
        if (i >= total) return "Fim da partida  " + partida.getResultado();
        String numero = i % 2 == 0 ? (i / 2 + 1) + ". " : (i / 2 + 1) + "... ";
        Classificacao c = analisador.classificar(i);
        Integer ev = analisador.avaliacao(i + 1);
        return String.format("%-7s%-9s%-3s%s", numero, analisador.sanPt(i), c == null ? "" : c.simbolo,
                ev == null ? "..." : QualidadeLances.formatarCurto(ev));
    }

    private JComponent criarBarraProgresso() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(FUNDO);
        p.setBorder(BorderFactory.createEmptyBorder(6, 12, 8, 12));
        estilo(lblProgresso, 13, false, Color.LIGHT_GRAY);
        lblProgresso.setText("Analisando posições... 0 de " + (total + 1));
        p.add(lblProgresso, BorderLayout.CENTER);
        p.add(barra, BorderLayout.EAST);
        return p;
    }

    private void configurarAtalhos() {
        atalho(KeyEvent.VK_LEFT, "anterior", () -> selecionar(atual - 1));
        atalho(KeyEvent.VK_RIGHT, "proximo", () -> selecionar(atual + 1));
        atalho(KeyEvent.VK_HOME, "inicio", () -> selecionar(0));
        atalho(KeyEvent.VK_END, "fim", () -> selecionar(total));
    }

    private void atalho(int tecla, String nome, Runnable acao) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(tecla, 0), nome);
        getActionMap().put(nome, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                acao.run();
            }
        });
    }

    private static JLabel secao(String texto) {
        JLabel l = new JLabel(texto);
        estilo(l, 11, true, SUAVE);
        return l;
    }

    private static void estilo(JLabel l, int tamanho, boolean negrito, Color cor) {
        l.setFont(new Font("SansSerif", negrito ? Font.BOLD : Font.PLAIN, tamanho));
        l.setForeground(cor);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private static JButton botao(String texto, Runnable acao) {
        JButton b = new JButton(texto);
        b.setFocusable(false);
        b.addActionListener(e -> acao.run());
        return b;
    }
}
