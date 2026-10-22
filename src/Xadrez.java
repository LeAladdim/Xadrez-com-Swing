import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class Xadrez extends JFrame {

    private final Partida partida = new Partida();
    private final MotorJava engine = new MotorJava();
    private final PainelTabuleiro tabuleiro = new PainelTabuleiro();

    private final JTextArea listaHistorico = new JTextArea();
    private final JLabel lblAvaliacao = new JLabel("Avaliação: +0.00");
    private final JLabel lblNomeAbertura = new JLabel("Posição Inicial");
    private final JLabel lblVarianteAbertura = new JLabel("Início da Partida");

    private final JComboBox<String> modoBox = new JComboBox<>(new String[]{"Vs Computador", "PvP Local (2 Jogadores)"});
    private boolean modoPvp = false;
    private boolean humanoBranco = true;
    private boolean tabuleiroInvertido = false;

    public Xadrez() {
        setTitle("Xadrez - PvP Local & Motor Integrado");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 780);
        setLocationRelativeTo(null);

        tabuleiro.setCallback(this::aoTentarJogar);

        JPanel pnlLateral = new JPanel();
        pnlLateral.setLayout(new BoxLayout(pnlLateral, BoxLayout.Y_AXIS));
        pnlLateral.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        pnlLateral.setPreferredSize(new Dimension(330, 0));
        pnlLateral.setBackground(new Color(0x1F2229));

        modoBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        modoBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        modoBox.addActionListener(e -> {
            modoPvp = modoBox.getSelectedIndex() == 1;
            iniciarAnalise();
        });

        JButton btnInverter = new JButton("Inverter Tabuleiro");
        btnInverter.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnInverter.addActionListener(e -> {
            tabuleiroInvertido = !tabuleiroInvertido;
            tabuleiro.atualizarMundo(partida.getAtual(), tabuleiroInvertido);
        });

        lblAvaliacao.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblAvaliacao.setForeground(Color.WHITE);
        lblAvaliacao.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblNomeAbertura.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblNomeAbertura.setForeground(new Color(0x7BD88F));
        lblNomeAbertura.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblVarianteAbertura.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblVarianteAbertura.setForeground(new Color(0x9AA1AE));
        lblVarianteAbertura.setAlignmentX(Component.LEFT_ALIGNMENT);

        pnlLateral.add(criarRotuloSecao("MODO DE JOGO"));
        pnlLateral.add(modoBox);
        pnlLateral.add(Box.createVerticalStrut(8));
        pnlLateral.add(btnInverter);
        pnlLateral.add(Box.createVerticalStrut(14));

        pnlLateral.add(criarRotuloSecao("AVALIAÇÃO DA POSIÇÃO"));
        pnlLateral.add(lblAvaliacao);
        pnlLateral.add(Box.createVerticalStrut(14));

        pnlLateral.add(criarRotuloSecao("ABERTURA DETECTADA"));
        pnlLateral.add(lblNomeAbertura);
        pnlLateral.add(lblVarianteAbertura);
        pnlLateral.add(Box.createVerticalStrut(14));

        pnlLateral.add(criarRotuloSecao("HISTÓRICO DE LANCES"));
        pnlLateral.add(Box.createVerticalStrut(4));

        listaHistorico.setEditable(false);
        listaHistorico.setBackground(new Color(0x15171C));
        listaHistorico.setForeground(Color.LIGHT_GRAY);
        listaHistorico.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scroll = new JScrollPane(listaHistorico);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlLateral.add(scroll);
        pnlLateral.add(Box.createVerticalStrut(10));

        JButton btnDesfazer = new JButton("Desfazer Lance");
        btnDesfazer.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnDesfazer.addActionListener(e -> {
            partida.desfazer();
            if (!modoPvp) partida.desfazer(); // Se for vs PC, desfaz a do computador também
            atualizarView();
            iniciarAnalise();
        });
        pnlLateral.add(btnDesfazer);

        setLayout(new BorderLayout());
        add(tabuleiro, BorderLayout.CENTER);
        add(pnlLateral, BorderLayout.EAST);

        engine.iniciar(new MotorJava.AnaliseCallback() {
            @Override
            public void aoReceberJogada(String lanceUci) {
                SwingUtilities.invokeLater(() -> {
                    int m = partida.getAtual().uciParaLance(lanceUci);
                    if (m != 0) {
                        partida.jogar(m);
                        atualizarView();
                        iniciarAnalise();
                    }
                });
            }

            @Override
            public void aoReceberAnalise(int scoreCp, boolean mate, String melhorLanceUci) {
                SwingUtilities.invokeLater(() -> {
                    tabuleiro.setSugestao(melhorLanceUci);
                    int absoluto = partida.getAtual().brancas ? scoreCp : -scoreCp;
                    if (mate) lblAvaliacao.setText("Mate forçado!");
                    else lblAvaliacao.setText(String.format("Avaliação: %+.2f", absoluto / 100.0));
                });
            }
        });

        atualizarView();
        iniciarAnalise();
    }

    private JLabel criarRotuloSecao(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(new Color(0x9AA1AE));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void aoTentarJogar(int lance) {
        if (partida.isFim()) return;

        boolean turnoBrancas = partida.getAtual().brancas;

        if (modoPvp) { // Modo 2 Jogadores na mesma máquina
            partida.jogar(lance);
            atualizarView();
            iniciarAnalise();
        } else { // Modo vs Computador
            if (turnoBrancas != humanoBranco) return;
            partida.jogar(lance);
            atualizarView();

            if (!partida.isFim()) {
                lblAvaliacao.setText("Computador pensando...");
                tabuleiro.setSugestao(null);
                engine.solicitarJogada(partida.getLancesUci(), 800);
            }
        }
    }

    private void iniciarAnalise() {
        if (!partida.isFim()) {
            engine.iniciarAnalise(partida.getLancesUci(), 5);
        }
    }

    private void atualizarView() {
        tabuleiro.atualizarMundo(partida.getAtual(), tabuleiroInvertido);

        AberturaDetector.InfoAbertura abertura = partida.getAberturaAtual();
        lblNomeAbertura.setText(abertura.nome);
        lblVarianteAbertura.setText(abertura.variante);

        StringBuilder sb = new StringBuilder();
        var san = partida.getHistoricoSan();
        for (int i = 0; i < san.size(); i++) {
            if (i % 2 == 0) sb.append(String.format("%3d. ", i / 2 + 1));
            sb.append(String.format("%-8s", san.get(i)));
            if (i % 2 != 0) sb.append("\n");
        }
        listaHistorico.setText(sb.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Xadrez().setVisible(true));
    }
}

class AberturaDetector {

    public static class InfoAbertura {
        public final String nome;
        public final String variante;

        public InfoAbertura(String nome, String variante) {
            this.nome = nome;
            this.variante = variante;
        }
    }

    private static final Map<String, InfoAbertura> DICIONARIO = new LinkedHashMap<>();

    static {
        cadastrar("", "Posição Inicial", "Início da Partida");

        // 1. e4
        cadastrar("e2e4", "Abertura do Peão do Rei", "1. e4");
        cadastrar("e2e4 e7e5", "Abertura do Peão do Rei", "2. ... e5");
        cadastrar("e2e4 e7e5 g1f3", "Abertura do Cavalo do Rei", "2. Nf3");
        cadastrar("e2e4 e7e5 g1f3 b8c6", "Abertura do Cavalo do Rei", "2... Nc6");
        cadastrar("e2e4 e7e5 g1f3 b8c6 f1b5", "Abertura Ruy Lopez (Espanhola)", "Linha Geral");
        cadastrar("e2e4 e7e5 g1f3 b8c6 f1b5 a7a6", "Abertura Ruy Lopez", "Defesa Morphy");
        cadastrar("e2e4 e7e5 g1f3 b8c6 f1c4", "Abertura Italiana", "Giuoco Piano");
        cadastrar("e2e4 e7e5 g1f3 b8c6 f1c4 f1c5", "Abertura Italiana", "Giuoco Pianissimo");
        cadastrar("e2e4 e7e5 g1f3 b8c6 f1c4 g8f6", "Abertura Italiana", "Defesa dos Dois Cavalos");

        // Siciliana
        cadastrar("e2e4 c7c5", "Defesa Siciliana", "Linha Geral");
        cadastrar("e2e4 c7c5 g1f3", "Defesa Siciliana", "Variante Aberta (Preparação)");
        cadastrar("e2e4 c7c5 g1f3 d7d6", "Defesa Siciliana", "Linha Principal");
        cadastrar("e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6", "Defesa Siciliana", "Variante Najdorf");

        // Outras
        cadastrar("e2e4 e7e6", "Defesa Francesa", "Linha Geral");
        cadastrar("e2e4 c7c6", "Defesa Caro-Kann", "Linha Geral");

        // 1. d4
        cadastrar("d2d4", "Abertura do Peão da Dama", "1. d4");
        cadastrar("d2d4 d7d5", "Abertura do Peão da Dama", "1... d5");
        cadastrar("d2d4 d7d5 c2c4", "Gambito da Dama", "Linha Geral");
        cadastrar("d2d4 d7d5 c1f4", "Sistema London", "Linha Principal");
        cadastrar("d2d4 g8f6", "Defesa Índia", "1... Nf6");
    }

    private static void cadastrar(String sequenciaUci, String nome, String variante) {
        DICIONARIO.put(sequenciaUci.trim(), new InfoAbertura(nome, variante));
    }

    public static InfoAbertura detectar(String historicoUci) {
        if (historicoUci == null || historicoUci.trim().isEmpty()) {
            return DICIONARIO.get("");
        }

        String[] lances = historicoUci.trim().split("\\s+");

        for (int i = lances.length; i > 0; i--) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < i; j++) {
                if (j > 0) sb.append(" ");
                sb.append(lances[j]);
            }
            String chave = sb.toString();
            if (DICIONARIO.containsKey(chave)) {
                return DICIONARIO.get(chave);
            }
        }

        return new InfoAbertura("Jogo Fora da Teoria", "Linha Personalizada / Não Mapeada");
    }
}

final class Posicao {
    public static final int P = 1, N = 2, B = 3, R = 4, Q = 5, K = 6;
    static final int[] DR = {-1, -1, -1, 0, 0, 1, 1, 1}, DC = {-1, 0, 1, -1, 1, -1, 0, 1};
    static final int[] NR = {-2, -2, -1, -1, 1, 1, 2, 2}, NC = {-1, 1, -2, 2, -2, 2, -1, 1};

    public final int[] b;
    public final boolean brancas;
    public final int cr, ep, half;

    private Posicao(int[] b, boolean brancas, int cr, int ep, int half) {
        this.b = b; this.brancas = brancas; this.cr = cr; this.ep = ep; this.half = half;
    }

    public static Posicao inicial() {
        int[] b = new int[64];
        int[] fundo = {R, N, B, Q, K, B, N, R};
        for (int c = 0; c < 8; c++) {
            b[c] = -fundo[c]; b[8 + c] = -P; b[48 + c] = P; b[56 + c] = fundo[c];
        }
        return new Posicao(b, true, 15, -1, 0);
    }

    public static int criarLance(int f, int t, int p) { return f | t << 6 | p << 12; }

    public boolean atacada(int sq, boolean pelasBrancas) {
        int r = sq >> 3, c = sq & 7, s = pelasBrancas ? 1 : -1, pr = pelasBrancas ? r + 1 : r - 1;
        if (pr >= 0 && pr < 8) {
            for (int dc = -1; dc <= 1; dc += 2) {
                int cc = c + dc;
                if (cc >= 0 && cc < 8 && b[pr * 8 + cc] == s * P) return true;
            }
        }
        for (int i = 0; i < 8; i++) {
            int rr = r + NR[i], cc = c + NC[i];
            if (rr >= 0 && rr < 8 && cc >= 0 && cc < 8 && b[rr * 8 + cc] == s * N) return true;
            rr = r + DR[i]; cc = c + DC[i];
            if (rr >= 0 && rr < 8 && cc >= 0 && cc < 8 && b[rr * 8 + cc] == s * K) return true;
        }
        for (int d = 0; d < 8; d++) {
            boolean diag = DR[d] != 0 && DC[d] != 0;
            int rr = r + DR[d], cc = c + DC[d];
            while (rr >= 0 && rr < 8 && cc >= 0 && cc < 8) {
                int p = b[rr * 8 + cc];
                if (p != 0) {
                    if (p * s > 0) {
                        int t = Math.abs(p);
                        if (t == Q || t == (diag ? B : R)) return true;
                    }
                    break;
                }
                rr += DR[d]; cc += DC[d];
            }
        }
        return false;
    }

    public int casaRei(boolean branca) {
        int k = branca ? K : -K;
        for (int i = 0; i < 64; i++) if (b[i] == k) return i;
        return -1;
    }

    public boolean emXeque() { return atacada(casaRei(brancas), !brancas); }

    private int addP(int[] o, int n, int f, int t, boolean promo) {
        if (promo) { o[n++] = criarLance(f, t, Q); o[n++] = criarLance(f, t, R); o[n++] = criarLance(f, t, B); o[n++] = criarLance(f, t, N); }
        else o[n++] = criarLance(f, t, 0);
        return n;
    }

    private int gerarPseudo(int[] o, boolean caps) {
        int n = 0, s = brancas ? 1 : -1;
        for (int sq = 0; sq < 64; sq++) {
            int p = b[sq];
            if (p * s <= 0) continue;
            int t = Math.abs(p), r = sq >> 3, c = sq & 7;
            if (t == P) {
                int dr = brancas ? -1 : 1, start = brancas ? 6 : 1, last = brancas ? 0 : 7, r1 = r + dr;
                if (b[r1 * 8 + c] == 0) {
                    n = addP(o, n, sq, r1 * 8 + c, r1 == last);
                    if (r == start && b[(r + 2 * dr) * 8 + c] == 0) o[n++] = criarLance(sq, (r + 2 * dr) * 8 + c, 0);
                }
                for (int dc = -1; dc <= 1; dc += 2) {
                    int cc = c + dc;
                    if (cc < 0 || cc > 7) continue;
                    int to = r1 * 8 + cc;
                    if (b[to] * s < 0) n = addP(o, n, sq, to, r1 == last);
                    else if (to == ep) o[n++] = criarLance(sq, to, 0);
                }
            } else if (t == N || t == K) {
                for (int i = 0; i < 8; i++) {
                    int rr = r + (t == N ? NR[i] : DR[i]), cc = c + (t == N ? NC[i] : DC[i]);
                    if (rr < 0 || rr > 7 || cc < 0 || cc > 7) continue;
                    if (b[rr * 8 + cc] * s <= 0) o[n++] = criarLance(sq, rr * 8 + cc, 0);
                }
                if (t == K && !caps) {
                    if (brancas && sq == 60) {
                        if ((cr & 1) != 0 && b[61] == 0 && b[62] == 0 && b[63] == R && !atacada(60, false) && !atacada(61, false) && !atacada(62, false))
                            o[n++] = criarLance(60, 62, 0);
                        if ((cr & 2) != 0 && b[59] == 0 && b[58] == 0 && b[57] == 0 && b[56] == R && !atacada(60, false) && !atacada(59, false) && !atacada(58, false))
                            o[n++] = criarLance(60, 58, 0);
                    } else if (!brancas && sq == 4) {
                        if ((cr & 4) != 0 && b[5] == 0 && b[6] == 0 && b[7] == -R && !atacada(4, true) && !atacada(5, true) && !atacada(6, true))
                            o[n++] = criarLance(4, 6, 0);
                        if ((cr & 8) != 0 && b[3] == 0 && b[2] == 0 && b[1] == 0 && b[0] == -R && !atacada(4, true) && !atacada(3, true) && !atacada(2, true))
                            o[n++] = criarLance(4, 2, 0);
                    }
                }
            } else {
                for (int d = 0; d < 8; d++) {
                    boolean diag = DR[d] != 0 && DC[d] != 0;
                    if ((t == B && !diag) || (t == R && diag)) continue;
                    int rr = r + DR[d], cc = c + DC[d];
                    while (rr >= 0 && rr < 8 && cc >= 0 && cc < 8) {
                        int q = b[rr * 8 + cc];
                        if (q * s <= 0) o[n++] = criarLance(sq, rr * 8 + cc, 0);
                        if (q != 0) break;
                        rr += DR[d]; cc += DC[d];
                    }
                }
            }
        }
        return n;
    }

    public boolean isCaptura(int m) {
        int f = m & 63, t = (m >> 6) & 63;
        return b[t] != 0 || (m >> 12) != 0 || (Math.abs(b[f]) == P && t == ep);
    }

    public int[] lancesLegais() {
        int[] o = new int[256];
        int n = gerarPseudo(o, false), k = 0;
        for (int i = 0; i < n; i++) {
            Posicao q = fazerLance(o[i]);
            if (!q.atacada(q.casaRei(brancas), !brancas)) o[k++] = o[i];
        }
        return Arrays.copyOf(o, k);
    }

    private static int mask(int sq) {
        switch (sq) {
            case 60: return 3; case 56: return 2; case 63: return 1;
            case 4: return 12; case 0: return 8; case 7: return 4;
            default: return 0;
        }
    }

    public Posicao fazerLance(int m) {
        int from = m & 63, to = (m >> 6) & 63, pr = m >> 12;
        int[] nb = b.clone();
        int p = nb[from], s = p > 0 ? 1 : -1, t = Math.abs(p), cap = nb[to];
        nb[to] = p; nb[from] = 0;
        int nep = -1, nh = (t == P || cap != 0) ? 0 : half + 1;
        if (t == P) {
            if (to == ep && cap == 0 && (from & 7) != (to & 7)) nb[to + (brancas ? 8 : -8)] = 0;
            if (Math.abs(to - from) == 16) nep = (from + to) / 2;
            if (pr != 0) nb[to] = s * pr;
        }
        if (t == K && Math.abs(to - from) == 2) {
            if (to > from) { nb[to - 1] = nb[to + 1]; nb[to + 1] = 0; }
            else { nb[to + 1] = nb[to - 2]; nb[to - 2] = 0; }
        }
        return new Posicao(nb, !brancas, cr & ~mask(from) & ~mask(to), nep, nh);
    }

    public String lanceParaUci(int m) {
        int f = m & 63, t = (m >> 6) & 63, pr = m >> 12;
        String uci = "" + (char)('a' + (f & 7)) + (8 - (f >> 3)) + (char)('a' + (t & 7)) + (8 - (t >> 3));
        if (pr != 0) { char[] prom = {' ', ' ', 'n', 'b', 'r', 'q'}; uci += prom[pr]; }
        return uci;
    }

    public int uciParaLance(String uci) {
        int fc = uci.charAt(0) - 'a', fr = 8 - (uci.charAt(1) - '0');
        int tc = uci.charAt(2) - 'a', tr = 8 - (uci.charAt(3) - '0');
        int targetF = fr * 8 + fc, targetT = tr * 8 + tc, targetPr = 0;
        if (uci.length() > 4) {
            char p = uci.charAt(4);
            if (p == 'q') targetPr = Q; else if (p == 'r') targetPr = R;
            else if (p == 'b') targetPr = B; else if (p == 'n') targetPr = N;
        }
        for (int m : lancesLegais()) {
            if ((m & 63) == targetF && ((m >> 6) & 63) == targetT && (m >> 12) == targetPr) return m;
        }
        return 0;
    }

    public String san(int m) {
        int f = m & 63, t = (m >> 6) & 63, pr = m >> 12, pc = Math.abs(b[f]);
        String dest = "" + (char) ('a' + (t & 7)) + (8 - (t >> 3));
        if (pc == K && Math.abs(t - f) == 2) return t > f ? "O-O" : "O-O-O";
        boolean cap = isCaptura(m);
        StringBuilder sb = new StringBuilder();
        if (pc == P) {
            if (cap) sb.append((char) ('a' + (f & 7))).append('x');
            sb.append(dest);
            if (pr != 0) sb.append('=').append(new String[]{"", "", "C", "B", "T", "D", "R"}[pr]);
        } else {
            sb.append(new String[]{"", "", "C", "B", "T", "D", "R"}[pc]);
            boolean outro = false, mesmaCol = false, mesmaLin = false;
            for (int o : lancesLegais()) {
                int of = o & 63;
                if (o == m || ((o >> 6) & 63) != t || of == f || Math.abs(b[of]) != pc) continue;
                outro = true;
                if ((of & 7) == (f & 7)) mesmaCol = true;
                if ((of >> 3) == (f >> 3)) mesmaLin = true;
            }
            if (outro) {
                if (!mesmaCol) sb.append((char) ('a' + (f & 7)));
                else if (!mesmaLin) sb.append(8 - (f >> 3));
                else sb.append((char) ('a' + (f & 7))).append(8 - (f >> 3));
            }
            if (cap) sb.append('x');
            sb

            sb.append(dest);
        }
        return sb.toString();
    }
}

class Partida {
    private Posicao atual;
    private final List<Posicao> historicoPosicoes = new ArrayList<>();
    private final List<Integer> historicoLances = new ArrayList<>();
    private final List<String> historicoUci = new ArrayList<>();
    private final List<String> historicoSan = new ArrayList<>();

    public Partida() {
        atual = Posicao.inicial();
    }

    public void jogar(int lance) {
        historicoPosicoes.add(atual);
        historicoLances.add(lance);
        historicoUci.add(atual.lanceParaUci(lance));

        String notacao = atual.san(lance);
        atual = atual.fazerLance(lance);

        if (atual.lancesLegais().length == 0) notacao += atual.emXeque() ? "#" : "";
        else if (atual.emXeque()) notacao += "+";

        historicoSan.add(notacao);
    }

    public void desfazer() {
        if (historicoPosicoes.isEmpty()) return;
        atual = historicoPosicoes.remove(historicoPosicoes.size() - 1);
        historicoLances.remove(historicoLances.size() - 1);
        historicoUci.remove(historicoUci.size() - 1);
        historicoSan.remove(historicoSan.size() - 1);
    }

    public String getLancesUci() {
        return String.join(" ", historicoUci);
    }

    public AberturaDetector.InfoAbertura getAberturaAtual() {
        return AberturaDetector.detectar(getLancesUci());
    }

    public Posicao getAtual() { return atual; }
    public boolean isFim() { return atual.lancesLegais().length == 0; }
    public List<String> getHistoricoSan() { return historicoSan; }
}

class MotorJava {
    private static final int INF = 1_000_000, MATE = 100_000;
    private static final int[] VAL = {0, 100, 320, 330, 500, 900, 0};

    private AnaliseCallback callback;
    private final Map<String, String[]> livroDeAberturas = new HashMap<>();
    private final Random rnd = new Random();
    private volatile boolean pararBusca = false;

    public interface AnaliseCallback {
        void aoReceberAnalise(int scoreCp, boolean mate, String melhorLanceUci);
        void aoReceberJogada(String lanceUci);
    }

    public MotorJava() {
        carregarLivroDeAberturas();
    }

    public void iniciar(AnaliseCallback callback) {
        this.callback = callback;
    }

    public void parar() {
        pararBusca = true;
    }

    public void solicitarJogada(String historicoUci, int tempoMs) {
        new Thread(() -> {
            String historicoLimpo = historicoUci != null ? historicoUci.trim() : "";

            String[] respostasLivro = livroDeAberturas.get(historicoLimpo);
            if (respostasLivro != null && respostasLivro.length > 0) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                String lanceSorteado = respostasLivro[rnd.nextInt(respostasLivro.length)];
                if (callback != null) callback.aoReceberJogada(lanceSorteado);
                return;
            }

            Posicao p = reconstruirPosicao(historicoLimpo);
            Searcher s = new Searcher();
            pararBusca = false;
            int m = s.best(p, 5, tempoMs, 5);

            if (callback != null && m != 0) {
                callback.aoReceberJogada(p.lanceParaUci(m));
            }
        }).start();
    }

    public void iniciarAnalise(String historicoUci, int profundidade) {
        pararBusca = true;
        new Thread(() -> {
            try { Thread.sleep(80); } catch (InterruptedException ignored) {}
            pararBusca = false;

            String historicoLimpo = historicoUci != null ? historicoUci.trim() : "";
            Posicao p = reconstruirPosicao(historicoLimpo);

            Searcher s = new Searcher();
            int m = s.best(p, profundidade, 1200, 0);

            if (!pararBusca && callback != null && m != 0) {
                int cp = s.lastScore;
                boolean mate = Math.abs(cp) > MATE - 100;
                if (!p.brancas) cp = -cp;
                callback.aoReceberAnalise(cp, mate, p.lanceParaUci(m));
            }
        }).start();
    }

    private Posicao reconstruirPosicao(String historicoUci) {
        Posicao p = Posicao.inicial();
        if (historicoUci.isEmpty()) return p;
        for (String uci : historicoUci.split(" ")) {
            int m = p.uciParaLance(uci);
            if (m != 0) p = p.fazerLance(m);
        }
        return p;
    }

    private void carregarLivroDeAberturas() {
        livroDeAberturas.put("", new String[]{"e2e4", "d2d4", "g1f3", "c2c4"});
        livroDeAberturas.put("e2e4", new String[]{"c7c5", "e7e5", "e7e6", "c7c6"});
        livroDeAberturas.put("e2e4 c7c5", new String[]{"g1f3", "b1c3"});
        livroDeAberturas.put("e2e4 c7c5 g1f3", new String[]{"d7d6", "e7e6", "b8c6"});
        livroDeAberturas.put("e2e4 e7e6", new String[]{"d2d4"});
        livroDeAberturas.put("e2e4 e7e6 d2d4", new String[]{"d7d5"});
        livroDeAberturas.put("e2e4 e7e5", new String[]{"g1f3", "f1c4", "d2d4"});
        livroDeAberturas.put("e2e4 e7e5 g1f3", new String[]{"b8c6", "g8f6"});
        livroDeAberturas.put("e2e4 e7e5 g1f3 b8c6", new String[]{"f1b5", "f1c4", "d2d4"});
        livroDeAberturas.put("d2d4", new String[]{"d7d5", "g8f6", "e7e6"});
        livroDeAberturas.put("d2d4 d7d5", new String[]{"c2c4", "c1f4", "g1f3"});
        livroDeAberturas.put("d2d4 g8f6", new String[]{"c2c4", "g1f3"});
        livroDeAberturas.put("c2c4", new String[]{"e7e5", "c7c5", "g8f6"});
    }

    private int avaliar(Posicao p) {
        int sc = 0, queens = 0;
        for (int sq = 0; sq < 64; sq++) if (Math.abs(p.b[sq]) == Posicao.Q) queens++;
        for (int sq = 0; sq < 64; sq++) {
            int v = p.b[sq];
            if (v == 0) continue;
            int t = Math.abs(v), s = v > 0 ? 1 : -1, r = sq >> 3, c = sq & 7;
            int cb = 14 - (Math.abs(2 * r - 7) + Math.abs(2 * c - 7));
            int bonus = 0;
            switch (t) {
                case Posicao.P: bonus = cb + 6 * (v > 0 ? 6 - r : r - 1); break;
                case Posicao.N: bonus = 3 * cb; break;
                case Posicao.B: bonus = 2 * cb; break;
                case Posicao.K: bonus = queens == 0 ? 4 * cb : ((r == (v > 0 ? 7 : 0) && (c <= 2 || c >= 6)) ? 30 : 0); break;
            }
            sc += s * (VAL[t] + bonus);
        }
        return sc;
    }

    private class Searcher {
        long deadline;
        int lastScore;
        private int[] ordenar(Posicao p, int[] ms, int principal) {
            int n = ms.length;
            int[] score = new int[n];
            for (int i = 0; i < n; i++) {
                int m = ms[i], f = m & 63, t = (m >> 6) & 63;
                int vitima = Math.abs(p.b[t]);
                if (vitima == 0 && Math.abs(p.b[f]) == Posicao.P && t == p.ep) vitima = Posicao.P;
                int sc = vitima != 0 ? 1000 + 10 * vitima - Math.abs(p.b[f]) : 0;
                if ((m >> 12) != 0) sc += 900 + (m >> 12);
                if (m == principal) sc += 100000;
                score[i] = sc;
            }
            int[] o = ms.clone();
            for (int i = 1; i < n; i++) {
                int m = o[i], sc = score[i], j = i - 1;
                while (j >= 0 && score[j] < sc) { o[j + 1] = o[j]; score[j + 1] = score[j]; j--; }
                o[j + 1] = m; score[j + 1] = sc;
            }
            return o;
        }

        private int quiesce(Posicao p, int alpha, int beta, int ply) {
            if (pararBusca) return 0;
            int stand = avaliar(p) * (p.brancas ? 1 : -1);
            if (stand >= beta) return stand;
            if (stand > alpha) alpha = stand;
            if (ply >= 40) return stand;

            for (int m : ordenar(p, p.lancesLegais(), -1)) {
                if (!p.isCaptura(m)) continue;
                int sc = -quiesce(p.fazerLance(m), -beta, -alpha, ply + 1);
                if (sc >= beta) return sc;
                if (sc > alpha) alpha = sc;
            }
            return alpha;
        }

        private int negamax(Posicao p, int depth, int alpha, int beta, int ply) {
            if (pararBusca || System.nanoTime() > deadline) return 0;
            if (p.half >= 100) return 0;
            boolean chk = p.emXeque();
            if (chk && ply < 40) depth++;
            if (depth <= 0) return quiesce(p, alpha, beta, ply);

            int[] ms = p.lancesLegais();
            if (ms.length == 0) return chk ? -MATE + ply : 0;

            int best = -INF;
            for (int m : ordenar(p, ms, -1)) {
                int sc = -negamax(p.fazerLance(m), depth - 1, -beta, -alpha, ply + 1);
                if (sc > best) best = sc;
                if (sc > alpha) alpha = sc;
                if (alpha >= beta) break;
            }
            return best;
        }

        public int best(Posicao p, int maxDepth, long millis, int variacaoRandomica) {
            int[] moves = p.lancesLegais();
            lastScore = avaliar(p) * (p.brancas ? 1 : -1);
            if (moves.length <= 1) return moves.length == 1 ? moves[0] : 0;

            if (rnd.nextInt(100) < variacaoRandomica) return moves[rnd.nextInt(moves.length)];

            for (int i = moves.length - 1; i > 0; i--) {
                int j = rnd.nextInt(i + 1), tmp = moves[i]; moves[i] = moves[j]; moves[j] = tmp;
            }

            deadline = System.nanoTime() + millis * 1_000_000L;
            int bestMove = moves[0];

            for (int d = 1; d <= maxDepth; d++) {
                int bm = bestMove, bs = -INF;
                for (int m : ordenar(p, moves, bestMove)) {
                    int sc = -negamax(p.fazerLance(m), d - 1, -INF, -Math.max(bs, -INF + 1), 1);
                    if (pararBusca || System.nanoTime() > deadline) return bestMove;
                    if (sc > bs) { bs = sc; bm = m; }
                }
                bestMove = bm; lastScore = bs;
                if (Math.abs(bs) > MATE - 100) break;
            }
            return bestMove;
        }
    }
}

class PainelTabuleiro extends JPanel {
    private Posicao posicao = Posicao.inicial();
    private boolean invertido = false;
    private int sel = -1;
    private final List<int[]> setasUsuario = new ArrayList<>();
    private int setaOrigem = -1, setaAtual = -1;
    private String sugestaoUci = null;

    public interface InteracaoCallback { void aoTentarJogar(int lance); }
    private InteracaoCallback callback;

    static final Shape[] FORMA = new Shape[7], DETALHE = new Shape[7];
    static {
        Area base = new Area(new RoundRectangle2D.Double(27, 78, 46, 9, 6, 6));
        base.add(new Area(new RoundRectangle2D.Double(21, 86, 58, 9, 7, 7)));

        Path2D.Double cp = new Path2D.Double(); cp.moveTo(34, 80); cp.curveTo(38, 64, 43, 56, 44, 47); cp.lineTo(56, 47); cp.curveTo(57, 56, 62, 64, 66, 80); cp.closePath();
        Area p = new Area(cp); p.add(new Area(new Ellipse2D.Double(37.5, 19, 25, 25))); p.add(new Area(new RoundRectangle2D.Double(35, 42, 30, 7, 5, 5))); p.add(base); FORMA[1] = p; DETALHE[1] = new Path2D.Double();

        Path2D.Double cv = new Path2D.Double(); cv.moveTo(30, 80); cv.curveTo(30, 68, 38, 62, 42, 54); cv.lineTo(28, 54); cv.curveTo(22, 54, 18, 50, 20, 46); cv.curveTo(24, 36, 34, 24, 46, 18); cv.lineTo(48, 9); cv.lineTo(55, 17); cv.curveTo(70, 22, 76, 40, 74, 56); cv.curveTo(73, 66, 70, 74, 70, 80); cv.closePath();
        Area ca = new Area(cv); ca.add(base); FORMA[2] = ca; Path2D.Double dc = new Path2D.Double(); dc.append(new Ellipse2D.Double(35.5, 31.5, 3.4, 3.4), false); dc.append(new Ellipse2D.Double(22.5, 47, 2, 2), false); dc.moveTo(58, 26); dc.quadTo(67, 40, 64, 58); DETALHE[2] = dc;

        Path2D.Double bc = new Path2D.Double(); bc.moveTo(36, 80); bc.curveTo(40, 66, 43, 58, 44, 50); bc.lineTo(56, 50); bc.curveTo(57, 58, 60, 66, 64, 80); bc.closePath();
        Area bi = new Area(bc); bi.add(new Area(new Ellipse2D.Double(38, 20, 24, 34))); bi.add(new Area(new Ellipse2D.Double(45.5, 12, 9, 9))); bi.add(new Area(new RoundRectangle2D.Double(35, 48, 30, 7, 4, 4))); bi.add(base); FORMA[3] = bi; Path2D.Double db = new Path2D.Double(); db.moveTo(46.5, 34); db.lineTo(55, 24); db.moveTo(36, 51); db.lineTo(64, 51); DETALHE[3] = db;

        Area topo = new Area(new RoundRectangle2D.Double(27, 19, 46, 20, 3, 3)); topo.subtract(new Area(new Rectangle2D.Double(37, 18, 7, 10))); topo.subtract(new Area(new Rectangle2D.Double(56, 18, 7, 10))); Path2D.Double tc = new Path2D.Double(); tc.moveTo(33, 80); tc.lineTo(36, 40); tc.lineTo(64, 40); tc.lineTo(67, 80); tc.closePath(); topo.add(new Area(tc)); topo.add(new Area(new RoundRectangle2D.Double(31, 36, 38, 7, 3, 3))); topo.add(base); FORMA[4] = topo; Path2D.Double dt = new Path2D.Double(); dt.moveTo(34, 40); dt.lineTo(66, 40); DETALHE[4] = dt;

        Path2D.Double qc = new Path2D.Double(); qc.moveTo(33, 80); qc.curveTo(37, 66, 40, 58, 37, 49); qc.lineTo(63, 49); qc.curveTo(60, 58, 63, 66, 67, 80); qc.closePath(); Path2D.Double coroa = new Path2D.Double(); double[][] pts = {{31, 50}, {26, 28}, {36, 42}, {39, 24}, {46, 42}, {50, 20}, {54, 42}, {61, 24}, {64, 42}, {74, 28}, {69, 50}}; coroa.moveTo(pts[0][0], pts[0][1]); for(int i=1;i<pts.length;i++) coroa.lineTo(pts[i][0], pts[i][1]); coroa.closePath(); Area d = new Area(qc); d.add(new Area(coroa)); d.add(new Area(new RoundRectangle2D.Double(33, 47, 34, 7, 4, 4))); d.add(base); double[][] bolas = {{26, 26}, {39, 22}, {50, 18}, {61, 22}, {74, 26}}; for(double[] bo : bolas) d.add(new Area(new Ellipse2D.Double(bo[0] - 3.8, bo[1] - 3.8, 7.6, 7.6))); FORMA[5] = d; Path2D.Double dq = new Path2D.Double(); dq.moveTo(34, 50); dq.lineTo(66, 50); DETALHE[5] = dq;

        Area rei = new Area(qc); rei.add(new Area(new RoundRectangle2D.Double(30, 30, 40, 20, 10, 10))); rei.add(new Area(new RoundRectangle2D.Double(33, 47, 34, 7, 4, 4))); rei.add(new Area(new Rectangle2D.Double(46, 5, 8, 27))); rei.add(new Area(new Rectangle2D.Double(39, 12, 22, 8))); rei.add(base); FORMA[6] = rei; Path2D.Double dr = new Path2D.Double(); dr.moveTo(34, 50); dr.lineTo(66, 50); dr.moveTo(34, 38); dr.lineTo(66, 38); DETALHE[6] = dr;
    }

    public PainelTabuleiro() {
        setBackground(new Color(0x16181D));
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    setasUsuario.clear();
                    clique(e.getX(), e.getY());
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    setaOrigem = setaAtual = casaEm(e.getX(), e.getY()); sel = -1; repaint();
                }
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (setaOrigem >= 0) { setaAtual = casaEm(e.getX(), e.getY()); repaint(); }
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3 && setaOrigem >= 0) {
                    int dest = casaEm(e.getX(), e.getY());
                    if (dest >= 0) alternarSeta(setaOrigem, dest);
                    setaOrigem = setaAtual = -1; repaint();
                }
            }
        };
        addMouseListener(mouse); addMouseMotionListener(mouse);
    }

    public void atualizarMundo(Posicao pos, boolean inv) {
        this.posicao = pos; this.invertido = inv; repaint();
    }

    public void setSugestao(String uci) { this.sugestaoUci = uci; repaint(); }
    public void setCallback(InteracaoCallback cb) { this.callback = cb; }

    private void clique(int x, int y) {
        int sq = casaEm(x, y);
        if (sq < 0) return;
        if (sel >= 0 && callback != null) {
            for (int m : posicao.lancesLegais()) {
                if ((m & 63) == sel && ((m >> 6) & 63) == sq) {
                    callback.aoTentarJogar(m);
                    sel = -1; repaint(); return;
                }
            }
        }
        int p = posicao.b[sq];
        sel = (p != 0 && (p > 0) == posicao.brancas) ? sq : -1;
        repaint();
    }

    private int S() { return Math.max(16, (Math.min(getWidth(), getHeight()) - 64) / 8); }
    private int bx() { return (getWidth() - 8 * S()) / 2; }
    private int by() { return (getHeight() - 8 * S()) / 2; }

    private int casaEm(int x, int y) {
        int c = Math.floorDiv(x - bx(), S()), r = Math.floorDiv(y - by(), S());
        if (c < 0 || c > 7 || r < 0 || r > 7) return -1;
        return (invertido ? 7 - r : r) * 8 + (invertido ? 7 - c : c);
    }

    private void alternarSeta(int de, int para) {
        setasUsuario.removeIf(a -> a[0] == de && a[1] == para);
        setasUsuario.add(new int[]{de, para});
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int s = S(), bx = bx(), by = by();

        for (int sq = 0; sq < 64; sq++) {
            int r = sq >> 3, c = sq & 7;
            int x = bx + (invertido ? 7 - c : c) * s, y = by + (invertido ? 7 - r : r) * s;
            g.setColor((r + c) % 2 == 1 ? new Color(0x4B7399) : new Color(0xEAE9D6));
            g.fillRect(x, y, s, s);
            if (sq == sel) { g.setColor(new Color(60, 220, 110, 150)); g.fillRect(x, y, s, s); }
            int p = posicao.b[sq];
            if (p != 0) desenharPeca(g, Math.abs(p), p > 0, x, y, s);
        }

        if (sugestaoUci != null && sugestaoUci.length() >= 4) {
            int fc = sugestaoUci.charAt(0) - 'a', fr = 8 - (sugestaoUci.charAt(1) - '0');
            int tc = sugestaoUci.charAt(2) - 'a', tr = 8 - (sugestaoUci.charAt(3) - '0');
            desenharSeta(g, fr * 8 + fc, tr * 8 + tc, new Color(0, 150, 255, 160));
        }
        for (int[] a : setasUsuario) desenharSeta(g, a[0], a[1], new Color(255, 160, 0, 205));
        if (setaOrigem >= 0 && setaAtual >= 0) desenharSeta(g, setaOrigem, setaAtual, new Color(255, 160, 0, 115));
    }

    private void desenharSeta(Graphics2D g, int de, int para, Color cor) {
        int s = S(), bx = bx(), by = by();
        g.setColor(cor);
        double[] c1 = {bx + (invertido ? 7 - (de & 7) : de & 7) * s + s / 2.0, by + (invertido ? 7 - (de >> 3) : de >> 3) * s + s / 2.0};
        double[] c2 = {bx + (invertido ? 7 - (para & 7) : para & 7) * s + s / 2.0, by + (invertido ? 7 - (para >> 3) : para >> 3) * s + s / 2.0};
        if (de == para) { g.setStroke(new BasicStroke(Math.max(3f, s * 0.07f))); g.drawOval((int)c1[0]-s/3, (int)c1[1]-s/3, s/3*2, s/3*2); return; }
        double dx = c2[0] - c1[0], dy = c2[1] - c1[1], len = Math.hypot(dx, dy); dx /= len; dy /= len;
        double cx = c2[0] - dx * s * 0.4, cy = c2[1] - dy * s * 0.4;
        Path2D.Double linha = new Path2D.Double(); linha.moveTo(c1[0], c1[1]); linha.lineTo(cx, cy);
        Area a = new Area(new BasicStroke((float)(s * 0.17), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(linha));
        Path2D.Double ponta = new Path2D.Double(); ponta.moveTo(c2[0] - dx*s*0.1, c2[1] - dy*s*0.1);
        ponta.lineTo(cx - dy*s*0.26, cy + dx*s*0.26); ponta.lineTo(cx + dy*s*0.26, cy - dx*s*0.26); ponta.closePath();
        a.add(new Area(ponta)); g.fill(a);
    }

    private void desenharPeca(Graphics2D g, int tipo, boolean branca, double x, double y, double s) {
        AffineTransform velho = g.getTransform(); g.translate(x, y); g.scale(s / 100.0, s / 100.0);
        g.setColor(new Color(0, 0, 0, 70)); g.fill(new Ellipse2D.Double(18, 88, 64, 9));
        Shape f = FORMA[tipo];
        g.setPaint(branca ? new GradientPaint(0, 8, new Color(0xFFFFFF), 0, 96, new Color(0xC4C9D2)) : new GradientPaint(0, 8, new Color(0x6A6F7A), 0, 96, new Color(0x121418)));
        g.fill(f);
        if (!branca) { g.setColor(new Color(255, 255, 255, 120)); g.setStroke(new BasicStroke(4.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(f); }
        g.setColor(branca ? new Color(0x1B1D22) : new Color(0x040405)); g.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(f);
        g.setColor(branca ? new Color(0x2A2D34) : new Color(0xE6E8EC)); g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(DETALHE[tipo]);
        g.setTransform(velho);
    }