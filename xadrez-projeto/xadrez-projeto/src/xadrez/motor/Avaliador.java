package xadrez.motor;

import xadrez.regras.Posicao;

/** Avaliação estática: material + bônus de posição (centro, avanço dos peões, segurança do rei). */
final class Avaliador {
    private static final int[] VALOR = {0, 100, 320, 330, 500, 900, 0};

    private Avaliador() { }

    /** Pontuação em centipeões do ponto de vista das brancas. */
    static int avaliar(Posicao p) {
        int pontos = 0, damas = 0;
        for (int sq = 0; sq < 64; sq++) if (Math.abs(p.b[sq]) == Posicao.Q) damas++;
        for (int sq = 0; sq < 64; sq++) {
            int v = p.b[sq];
            if (v == 0) continue;
            int tipo = Math.abs(v), lado = v > 0 ? 1 : -1, linha = sq >> 3, coluna = sq & 7;
            int centro = 14 - (Math.abs(2 * linha - 7) + Math.abs(2 * coluna - 7)); // 0..12
            int bonus = 0;
            switch (tipo) {
                case Posicao.P: bonus = centro + 6 * (v > 0 ? 6 - linha : linha - 1); break;
                case Posicao.N: bonus = 3 * centro; break;
                case Posicao.B: bonus = 2 * centro; break;
                case Posicao.K:
                    bonus = damas == 0 ? 4 * centro
                            : ((linha == (v > 0 ? 7 : 0) && (coluna <= 2 || coluna >= 6)) ? 30 : 0);
                    break;
                default: break;
            }
            pontos += lado * (VALOR[tipo] + bonus);
        }
        return pontos;
    }
}
