package xadrez.motor;

import xadrez.abertura.LivroAberturas;
import xadrez.regras.Posicao;

import java.util.Random;
import java.util.concurrent.Executor;

/**
 * Interface assíncrona do motor: pede lances do computador e análises da posição.
 *
 * <p>Cada pedido tem seu próprio {@link Cancelamento}. Qualquer novo pedido ou {@link #cancelar()}
 * invalida os anteriores, e respostas antigas nunca são entregues (evita aplicar um lance
 * calculado para uma posição que já mudou).
 */
public class MotorJava {
    /** O computador sempre leva entre 2 e 3 segundos para jogar (inclusive lances de livro), em qualquer nível. */
    public static final int TEMPO_MIN_MS = 2000, TEMPO_MAX_MS = 3000;
    /** Profundidade máxima das buscas (nível "Máximo" e avaliação das posições). */
    public static final int PROFUNDIDADE_MAXIMA = 8;
    public static final int PROFUNDIDADE_ANALISE = 5, TEMPO_ANALISE_MS = 1200;
    private static final int TEMPO_NIVEL_FRACO_MS = 600, MARGEM_MS = 150;

    public interface AnaliseCallback {
        /**
         * Análise da posição atual (na vez do humano).
         *
         * @param pontosBrancas avaliação em centipeões do ponto de vista das BRANCAS
         */
        void aoReceberAnalise(int pontosBrancas, String melhorLanceUci);

        /**
         * Lance escolhido pelo computador.
         *
         * @param pontosBrancas avaliação (ponto de vista das BRANCAS) da posição ANTES do lance
         */
        void aoReceberJogada(String lanceUci, int pontosBrancas);
    }

    private final Random rnd = new Random();
    private volatile AnaliseCallback callback;
    private volatile Executor entrega = Runnable::run;
    private Cancelamento tokenJogada = new Cancelamento();
    private Cancelamento tokenAnalise = new Cancelamento();

    /**
     * @param entrega onde os callbacks são executados (na interface gráfica use {@code SwingUtilities::invokeLater}).
     *                O teste de cancelamento é refeito dentro dele, então nada obsoleto chega ao callback.
     */
    public void iniciar(AnaliseCallback callback, Executor entrega) {
        this.callback = callback;
        this.entrega = entrega != null ? entrega : Runnable::run;
    }

    public synchronized void cancelar() {
        tokenAnalise.cancelar();
        tokenJogada.cancelar();
    }

    private synchronized Cancelamento novoTokenJogada() {
        tokenAnalise.cancelar();
        tokenJogada.cancelar();
        return tokenJogada = new Cancelamento();
    }

    private synchronized Cancelamento novoTokenAnalise() {
        tokenAnalise.cancelar();
        return tokenAnalise = new Cancelamento();
    }

    /** Pede o lance do computador (a resposta chega em {@code aoReceberJogada}). */
    public void solicitarJogada(Posicao posicao, String historicoUci, NivelDificuldade nivel) {
        final Cancelamento token = novoTokenJogada();
        final long alvoMs = TEMPO_MIN_MS + rnd.nextInt(TEMPO_MAX_MS - TEMPO_MIN_MS + 1);
        iniciarThread("motor-jogada", () -> {
            final long inicio = System.nanoTime();
            final long limiteBusca = inicio + (alvoMs - MARGEM_MS) * 1_000_000L;
            String uci = LivroAberturas.sortear(historicoUci, rnd);
            if (uci != null && posicao.uciParaLance(uci) == 0) uci = null; // lance de livro inválido: ignora
            int pontos; // do ponto de vista de quem joga
            if (uci == null) {
                boolean plena = nivel.profundidade >= PROFUNDIDADE_MAXIMA && nivel.chanceAleatoria == 0;
                Busca busca = new Busca(token);
                long tempo = plena ? alvoMs - MARGEM_MS : TEMPO_NIVEL_FRACO_MS;
                int m = busca.melhorLance(posicao, nivel.profundidade, tempo, nivel.chanceAleatoria);
                if (token.isCancelado() || m == 0) return;
                uci = posicao.lanceParaUci(m);
                pontos = plena ? busca.getUltimaPontuacao() : avaliar(posicao, token, limiteBusca);
            } else {
                pontos = avaliar(posicao, token, limiteBusca);
            }
            if (token.isCancelado()) return;
            final int pontosBrancas = posicao.brancas ? pontos : -pontos;
            esperarAte(inicio + alvoMs * 1_000_000L, token);
            final String jogada = uci;
            entregar(token, () -> callback.aoReceberJogada(jogada, pontosBrancas));
        });
    }

    /** Analisa a posição em segundo plano (a resposta chega em {@code aoReceberAnalise}). */
    public void iniciarAnalise(Posicao posicao) {
        final Cancelamento token = novoTokenAnalise();
        iniciarThread("motor-analise", () -> {
            Busca busca = new Busca(token);
            int m = busca.melhorLance(posicao, PROFUNDIDADE_ANALISE, TEMPO_ANALISE_MS, 0);
            if (token.isCancelado() || m == 0) return;
            int cp = busca.getUltimaPontuacao(); // ponto de vista de quem joga
            final int pontosBrancas = posicao.brancas ? cp : -cp;
            final String uci = posicao.lanceParaUci(m);
            entregar(token, () -> callback.aoReceberAnalise(pontosBrancas, uci));
        });
    }

    /** Avalia a posição (de quem joga) com uma busca forte até o limite de tempo. */
    private static int avaliar(Posicao posicao, Cancelamento token, long limiteNanos) {
        long restanteMs = Math.max(200, (limiteNanos - System.nanoTime()) / 1_000_000L);
        Busca busca = new Busca(token);
        busca.melhorLance(posicao, PROFUNDIDADE_MAXIMA, restanteMs, 0);
        return busca.getUltimaPontuacao();
    }

    private void entregar(Cancelamento token, Runnable acao) {
        entrega.execute(() -> {
            if (!token.isCancelado() && callback != null) acao.run();
        });
    }

    private static void esperarAte(long alvoNanos, Cancelamento token) {
        try {
            while (!token.isCancelado() && System.nanoTime() < alvoNanos) Thread.sleep(25);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void iniciarThread(String nome, Runnable tarefa) {
        Thread t = new Thread(tarefa, nome);
        t.setDaemon(true);
        t.start();
    }
}
