package xadrez.motor;

import xadrez.regras.Posicao;

import java.util.Random;

/** Busca alfa-beta (negamax) com aprofundamento iterativo, quiescência e limite de tempo. */
final class Busca {
    static final int MATE = 100_000;
    private static final int INF = 1_000_000;

    private static final class TempoEsgotado extends RuntimeException {
        TempoEsgotado() { super(null, null, false, false); }
    }

    private final Cancelamento cancelamento;
    private final Random rnd = new Random();
    private long prazo, nos;
    private int ultimaPontuacao;

    Busca(Cancelamento cancelamento) {
        this.cancelamento = cancelamento;
    }

    /** Pontuação (do ponto de vista de quem joga) da última profundidade completa. */
    int getUltimaPontuacao() { return ultimaPontuacao; }

    private int[] ordenar(Posicao p, int[] lances, int principal) {
        int n = lances.length;
        int[] pontos = new int[n];
        for (int i = 0; i < n; i++) {
            int m = lances[i], de = m & 63, para = (m >> 6) & 63;
            int vitima = Math.abs(p.b[para]);
            if (vitima == 0 && Math.abs(p.b[de]) == Posicao.P && para == p.ep) vitima = Posicao.P;
            int sc = vitima != 0 ? 1000 + 10 * vitima - Math.abs(p.b[de]) : 0;
            if ((m >> 12) != 0) sc += 900 + (m >> 12);
            if (m == principal) sc += 100000;
            pontos[i] = sc;
        }
        int[] o = lances.clone();
        for (int i = 1; i < n; i++) { // ordenação por inserção (decrescente)
            int m = o[i], sc = pontos[i], j = i - 1;
            while (j >= 0 && pontos[j] < sc) { o[j + 1] = o[j]; pontos[j + 1] = pontos[j]; j--; }
            o[j + 1] = m;
            pontos[j + 1] = sc;
        }
        return o;
    }

    private void verificarTempo() {
        if ((++nos & 2047) == 0 && (cancelamento.isCancelado() || System.nanoTime() > prazo)) {
            throw new TempoEsgotado();
        }
    }

    private int quiescencia(Posicao p, int alfa, int beta, int ply) {
        verificarTempo();
        int parado = Avaliador.avaliar(p) * (p.brancas ? 1 : -1);
        if (parado >= beta) return parado;
        if (parado > alfa) alfa = parado;
        if (ply >= 40) return parado;
        for (int m : ordenar(p, p.lancesLegais(true), -1)) {
            int sc = -quiescencia(p.fazerLance(m), -beta, -alfa, ply + 1);
            if (sc >= beta) return sc;
            if (sc > alfa) alfa = sc;
        }
        return alfa;
    }

    private int negamax(Posicao p, int profundidade, int alfa, int beta, int ply) {
        verificarTempo();
        if (p.half >= 100) return 0;
        boolean xeque = p.emXeque();
        if (xeque && ply < 40) profundidade++;
        if (profundidade <= 0) return quiescencia(p, alfa, beta, ply);
        int[] lances = p.lancesLegais();
        if (lances.length == 0) return xeque ? -MATE + ply : 0;
        int melhor = -INF;
        for (int m : ordenar(p, lances, -1)) {
            int sc = -negamax(p.fazerLance(m), profundidade - 1, -beta, -alfa, ply + 1);
            if (sc > melhor) melhor = sc;
            if (sc > alfa) alfa = sc;
            if (alfa >= beta) break;
        }
        return melhor;
    }

    /**
     * Procura o melhor lance até {@code profMax} ou até acabar o tempo/ser cancelada.
     * Devolve 0 se não houver lances.
     *
     * @param chanceAleatoria percentual (0-100) de jogar um lance aleatório em vez de calcular
     */
    int melhorLance(Posicao p, int profMax, long tempoMs, int chanceAleatoria) {
        int[] lances = p.lancesLegais();
        ultimaPontuacao = Avaliador.avaliar(p) * (p.brancas ? 1 : -1);
        if (lances.length <= 1) return lances.length == 1 ? lances[0] : 0;
        if (rnd.nextInt(100) < chanceAleatoria) return lances[rnd.nextInt(lances.length)];

        for (int i = lances.length - 1; i > 0; i--) { // embaralha para variar as partidas
            int j = rnd.nextInt(i + 1), tmp = lances[i];
            lances[i] = lances[j];
            lances[j] = tmp;
        }

        prazo = System.nanoTime() + tempoMs * 1_000_000L;
        int melhor = lances[0];
        try {
            for (int d = 1; d <= profMax; d++) {
                int melhorDaRodada = melhor, pontos = -INF;
                for (int m : ordenar(p, lances, melhor)) {
                    int sc = -negamax(p.fazerLance(m), d - 1, -INF, -Math.max(pontos, -INF + 1), 1);
                    if (sc > pontos) { pontos = sc; melhorDaRodada = m; }
                }
                melhor = melhorDaRodada;
                ultimaPontuacao = pontos;
                if (Math.abs(pontos) > MATE - 100) break;
            }
        } catch (TempoEsgotado e) {
            // usa o resultado da última profundidade concluída
        }
        return melhor;
    }
}
