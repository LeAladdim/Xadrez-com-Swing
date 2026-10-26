package xadrez.regras;

import java.util.Arrays;

/**
 * Posição de xadrez imutável, com geração de lances legais.
 *
 * <p>Casas 0..63: linha 0 = 8ª fileira (pretas), coluna 0 = coluna "a".
 * Peças no tabuleiro: valor positivo = brancas, negativo = pretas.
 * Um lance é um int: {@code origem | destino << 6 | promoção << 12}.
 */
public final class Posicao {
    public static final int P = 1, N = 2, B = 3, R = 4, Q = 5, K = 6;

    private static final int[] DR = {-1, -1, -1, 0, 0, 1, 1, 1}, DC = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] NR = {-2, -2, -1, -1, 1, 1, 2, 2}, NC = {-1, 1, -2, 2, -2, 2, -1, 1};
    private static final String[] LETRAS = {"", "", "C", "B", "T", "D", "R"};

    /** Tabuleiro (somente leitura): positivo = brancas, negativo = pretas. */
    public final int[] b;
    /** Vez das brancas? */
    public final boolean brancas;
    /** Direitos de roque (1 = O-O brancas, 2 = O-O-O brancas, 4 = O-O pretas, 8 = O-O-O pretas). */
    public final int cr;
    /** Casa de captura en passant (ou -1). */
    public final int ep;
    /** Meios-lances desde a última captura/lance de peão (regra dos 50 lances). */
    public final int half;

    private Posicao(int[] b, boolean brancas, int cr, int ep, int half) {
        this.b = b;
        this.brancas = brancas;
        this.cr = cr;
        this.ep = ep;
        this.half = half;
    }

    public static Posicao inicial() {
        int[] b = new int[64];
        int[] fundo = {R, N, B, Q, K, B, N, R};
        for (int c = 0; c < 8; c++) {
            b[c] = -fundo[c];
            b[8 + c] = -P;
            b[48 + c] = P;
            b[56 + c] = fundo[c];
        }
        return new Posicao(b, true, 15, -1, 0);
    }

    /** Cria uma posição a partir de uma notação FEN (útil para testes e estudos). */
    public static Posicao deFen(String fen) {
        String[] t = fen.trim().split("\\s+");
        int[] b = new int[64];
        int sq = 0;
        for (char ch : t[0].toCharArray()) {
            if (ch == '/') continue;
            if (Character.isDigit(ch)) { sq += ch - '0'; continue; }
            int tipo = "pnbrqk".indexOf(Character.toLowerCase(ch)) + 1;
            b[sq++] = Character.isUpperCase(ch) ? tipo : -tipo;
        }
        boolean brancas = t.length < 2 || t[1].equals("w");
        String roques = t.length > 2 ? t[2] : "-";
        int cr = (roques.contains("K") ? 1 : 0) | (roques.contains("Q") ? 2 : 0)
                | (roques.contains("k") ? 4 : 0) | (roques.contains("q") ? 8 : 0);
        int ep = (t.length > 3 && !t[3].equals("-")) ? (8 - (t[3].charAt(1) - '0')) * 8 + (t[3].charAt(0) - 'a') : -1;
        int half = t.length > 4 ? Integer.parseInt(t[4]) : 0;
        return new Posicao(b, brancas, cr, ep, half);
    }

    public static int criarLance(int de, int para, int promocao) {
        return de | para << 6 | promocao << 12;
    }

    // ------------------------------------------------------------ ataques e xeque

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
            rr = r + DR[i];
            cc = c + DC[i];
            if (rr >= 0 && rr < 8 && cc >= 0 && cc < 8 && b[rr * 8 + cc] == s * K) return true;
        }
        for (int d = 0; d < 8; d++) {
            boolean diagonal = DR[d] != 0 && DC[d] != 0;
            int rr = r + DR[d], cc = c + DC[d];
            while (rr >= 0 && rr < 8 && cc >= 0 && cc < 8) {
                int p = b[rr * 8 + cc];
                if (p != 0) {
                    if (p * s > 0) {
                        int t = Math.abs(p);
                        if (t == Q || t == (diagonal ? B : R)) return true;
                    }
                    break;
                }
                rr += DR[d];
                cc += DC[d];
            }
        }
        return false;
    }

    public int casaRei(boolean branca) {
        int k = branca ? K : -K;
        for (int i = 0; i < 64; i++) if (b[i] == k) return i;
        return -1;
    }

    public boolean emXeque() {
        return atacada(casaRei(brancas), !brancas);
    }

    // ------------------------------------------------------------ geração de lances

    private int adicionarPeao(int[] o, int n, int de, int para, boolean promocao) {
        if (promocao) {
            o[n++] = criarLance(de, para, Q);
            o[n++] = criarLance(de, para, R);
            o[n++] = criarLance(de, para, B);
            o[n++] = criarLance(de, para, N);
        } else {
            o[n++] = criarLance(de, para, 0);
        }
        return n;
    }

    /** Lances pseudo-legais (podem deixar o próprio rei em xeque). */
    private int gerarPseudo(int[] o, boolean apenasCapturas) {
        int n = 0, s = brancas ? 1 : -1;
        for (int sq = 0; sq < 64; sq++) {
            int p = b[sq];
            if (p * s <= 0) continue;
            int t = Math.abs(p), r = sq >> 3, c = sq & 7;
            if (t == P) {
                int dr = brancas ? -1 : 1, inicio = brancas ? 6 : 1, ultima = brancas ? 0 : 7, r1 = r + dr;
                if (b[r1 * 8 + c] == 0) {
                    n = adicionarPeao(o, n, sq, r1 * 8 + c, r1 == ultima);
                    if (r == inicio && b[(r + 2 * dr) * 8 + c] == 0) o[n++] = criarLance(sq, (r + 2 * dr) * 8 + c, 0);
                }
                for (int dc = -1; dc <= 1; dc += 2) {
                    int cc = c + dc;
                    if (cc < 0 || cc > 7) continue;
                    int para = r1 * 8 + cc;
                    if (b[para] * s < 0) n = adicionarPeao(o, n, sq, para, r1 == ultima);
                    else if (para == ep) o[n++] = criarLance(sq, para, 0);
                }
            } else if (t == N || t == K) {
                for (int i = 0; i < 8; i++) {
                    int rr = r + (t == N ? NR[i] : DR[i]), cc = c + (t == N ? NC[i] : DC[i]);
                    if (rr < 0 || rr > 7 || cc < 0 || cc > 7) continue;
                    if (b[rr * 8 + cc] * s <= 0) o[n++] = criarLance(sq, rr * 8 + cc, 0);
                }
                if (t == K && !apenasCapturas) n = adicionarRoques(o, n, sq);
            } else {
                for (int d = 0; d < 8; d++) {
                    boolean diagonal = DR[d] != 0 && DC[d] != 0;
                    if ((t == B && !diagonal) || (t == R && diagonal)) continue;
                    int rr = r + DR[d], cc = c + DC[d];
                    while (rr >= 0 && rr < 8 && cc >= 0 && cc < 8) {
                        int q = b[rr * 8 + cc];
                        if (q * s <= 0) o[n++] = criarLance(sq, rr * 8 + cc, 0);
                        if (q != 0) break;
                        rr += DR[d];
                        cc += DC[d];
                    }
                }
            }
        }
        return n;
    }

    private int adicionarRoques(int[] o, int n, int sq) {
        if (brancas && sq == 60) {
            if ((cr & 1) != 0 && b[61] == 0 && b[62] == 0 && b[63] == R
                    && !atacada(60, false) && !atacada(61, false) && !atacada(62, false))
                o[n++] = criarLance(60, 62, 0);
            if ((cr & 2) != 0 && b[59] == 0 && b[58] == 0 && b[57] == 0 && b[56] == R
                    && !atacada(60, false) && !atacada(59, false) && !atacada(58, false))
                o[n++] = criarLance(60, 58, 0);
        } else if (!brancas && sq == 4) {
            if ((cr & 4) != 0 && b[5] == 0 && b[6] == 0 && b[7] == -R
                    && !atacada(4, true) && !atacada(5, true) && !atacada(6, true))
                o[n++] = criarLance(4, 6, 0);
            if ((cr & 8) != 0 && b[3] == 0 && b[2] == 0 && b[1] == 0 && b[0] == -R
                    && !atacada(4, true) && !atacada(3, true) && !atacada(2, true))
                o[n++] = criarLance(4, 2, 0);
        }
        return n;
    }

    public boolean isCaptura(int m) {
        int de = m & 63, para = (m >> 6) & 63;
        return b[para] != 0 || (m >> 12) != 0 || (Math.abs(b[de]) == P && para == ep);
    }

    public int[] lancesLegais() {
        return lancesLegais(false);
    }

    /** @param apenasCapturas se true, devolve só capturas e promoções (usado na busca de quiescência). */
    public int[] lancesLegais(boolean apenasCapturas) {
        int[] o = new int[256];
        int n = gerarPseudo(o, apenasCapturas), k = 0;
        for (int i = 0; i < n; i++) {
            int m = o[i];
            if (apenasCapturas && !isCaptura(m)) continue;
            Posicao q = fazerLance(m);
            if (!q.atacada(q.casaRei(brancas), !brancas)) o[k++] = m;
        }
        return Arrays.copyOf(o, k);
    }

    private static int mascaraRoque(int sq) {
        switch (sq) {
            case 60: return 3;
            case 56: return 2;
            case 63: return 1;
            case 4: return 12;
            case 0: return 8;
            case 7: return 4;
            default: return 0;
        }
    }

    public Posicao fazerLance(int m) {
        int de = m & 63, para = (m >> 6) & 63, promocao = m >> 12;
        int[] nb = b.clone();
        int p = nb[de], s = p > 0 ? 1 : -1, t = Math.abs(p), captura = nb[para];
        nb[para] = p;
        nb[de] = 0;
        int novoEp = -1, novoRelogio = (t == P || captura != 0) ? 0 : half + 1;
        if (t == P) {
            if (para == ep && captura == 0 && (de & 7) != (para & 7)) nb[para + (brancas ? 8 : -8)] = 0;
            if (Math.abs(para - de) == 16) novoEp = (de + para) / 2;
            if (promocao != 0) nb[para] = s * promocao;
        }
        if (t == K && Math.abs(para - de) == 2) {
            if (para > de) { nb[para - 1] = nb[para + 1]; nb[para + 1] = 0; }
            else { nb[para + 1] = nb[para - 2]; nb[para - 2] = 0; }
        }
        return new Posicao(nb, !brancas, cr & ~mascaraRoque(de) & ~mascaraRoque(para), novoEp, novoRelogio);
    }

    // ------------------------------------------------------------ utilidades

    /** Identifica a posição para detectar repetições. */
    public String chave() {
        return Arrays.toString(b) + brancas + cr + ep;
    }

    /** Rei x Rei, Rei+Cavalo x Rei ou Rei+Bispo x Rei. */
    public boolean materialInsuficiente() {
        int menores = 0;
        for (int v : b) {
            int t = Math.abs(v);
            if (t == P || t == R || t == Q) return false;
            if (t == N || t == B) menores++;
        }
        return menores <= 1;
    }

    // ------------------------------------------------------------ notações

    public String lanceParaUci(int m) {
        int de = m & 63, para = (m >> 6) & 63, promocao = m >> 12;
        String uci = "" + (char) ('a' + (de & 7)) + (8 - (de >> 3)) + (char) ('a' + (para & 7)) + (8 - (para >> 3));
        if (promocao != 0) uci += "  nbrq".charAt(promocao); // índice = código da peça (N=2, B=3, R=4, Q=5)
        return uci;
    }

    /** Converte "e2e4" (ou "e7e8q") no lance legal correspondente; devolve 0 se não existir. */
    public int uciParaLance(String uci) {
        if (uci == null || uci.length() < 4) return 0;
        int de = (8 - (uci.charAt(1) - '0')) * 8 + (uci.charAt(0) - 'a');
        int para = (8 - (uci.charAt(3) - '0')) * 8 + (uci.charAt(2) - 'a');
        int promocao = 0;
        if (uci.length() > 4) {
            switch (uci.charAt(4)) {
                case 'q': promocao = Q; break;
                case 'r': promocao = R; break;
                case 'b': promocao = B; break;
                case 'n': promocao = N; break;
                default: break;
            }
        }
        for (int m : lancesLegais()) {
            if ((m & 63) == de && ((m >> 6) & 63) == para && (m >> 12) == promocao) return m;
        }
        return 0;
    }

    /** Notação algébrica em português (C = cavalo, B = bispo, T = torre, D = dama, R = rei), sem "+" / "#". */
    public String san(int m) {
        int de = m & 63, para = (m >> 6) & 63, promocao = m >> 12, peca = Math.abs(b[de]);
        String destino = "" + (char) ('a' + (para & 7)) + (8 - (para >> 3));
        if (peca == K && Math.abs(para - de) == 2) return para > de ? "O-O" : "O-O-O";
        boolean captura = isCaptura(m);
        StringBuilder sb = new StringBuilder();
        if (peca == P) {
            if (captura) sb.append((char) ('a' + (de & 7))).append('x');
            sb.append(destino);
            if (promocao != 0) sb.append('=').append(LETRAS[promocao]);
        } else {
            sb.append(LETRAS[peca]);
            boolean outro = false, mesmaColuna = false, mesmaLinha = false;
            for (int o : lancesLegais()) {
                int origemO = o & 63;
                if (o == m || ((o >> 6) & 63) != para || origemO == de || Math.abs(b[origemO]) != peca) continue;
                outro = true;
                if ((origemO & 7) == (de & 7)) mesmaColuna = true;
                if ((origemO >> 3) == (de >> 3)) mesmaLinha = true;
            }
            if (outro) {
                if (!mesmaColuna) sb.append((char) ('a' + (de & 7)));
                else if (!mesmaLinha) sb.append(8 - (de >> 3));
                else sb.append((char) ('a' + (de & 7))).append(8 - (de >> 3));
            }
            if (captura) sb.append('x');
            sb.append(destino);
        }
        return sb.toString();
    }
}
