package xadrez.analise;

import xadrez.abertura.AberturaDetector;
import xadrez.motor.Cancelamento;
import xadrez.motor.MotorJava;
import xadrez.motor.ResultadoBusca;
import xadrez.regras.Posicao;
import xadrez.regras.Troca;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Analisa uma partida inteira: avalia todas as posições (em paralelo, no tempo escolhido por posição),
 * classifica cada lance e gera as explicações.
 *
 * <p>A posição de índice {@code i} é a que existe antes do lance {@code i} (0 = inicial, {@code totalLances()} = final).
 */
public final class AnalisadorPartida {

    public interface Ouvinte {
        /** Uma posição ficou pronta (chamado no executor de entrega, na interface gráfica). */
        void aoAvaliar(int concluidas, int total);

        void aoConcluir();
    }

    private final List<String> lancesUci;
    private final int[] lances;
    private final Posicao[] posicoes;
    private final Map<Integer, ResultadoBusca> resultados = new ConcurrentHashMap<>();
    private final AtomicInteger concluidas = new AtomicInteger();
    private volatile Cancelamento cancelamento = new Cancelamento();

    /** @throws IllegalArgumentException se algum lance for ilegal */
    public AnalisadorPartida(List<String> lancesUci) {
        this.lancesUci = new ArrayList<>(lancesUci);
        this.lances = new int[lancesUci.size()];
        this.posicoes = new Posicao[lancesUci.size() + 1];
        posicoes[0] = Posicao.inicial();
        for (int i = 0; i < lances.length; i++) {
            lances[i] = posicoes[i].uciParaLance(lancesUci.get(i));
            if (lances[i] == 0) throw new IllegalArgumentException("Lance ilegal: " + lancesUci.get(i) + " (lance " + (i + 1) + ")");
            posicoes[i + 1] = posicoes[i].fazerLance(lances[i]);
        }
    }

    // ------------------------------------------------------------ execução

    public void iniciar(NivelAnalise nivel, Executor entrega, Ouvinte ouvinte) {
        final Cancelamento token = cancelamento;
        final int total = posicoes.length;
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        ExecutorService pool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "analise-partida");
            t.setDaemon(true);
            return t;
        });
        for (int i = 0; i < total; i++) {
            final int indice = i;
            pool.submit(() -> {
                if (token.isCancelado()) return;
                ResultadoBusca r = MotorJava.analisar(posicoes[indice], nivel.profundidade, nivel.tempoMs, token);
                if (token.isCancelado()) return;
                resultados.put(indice, r);
                final int feitas = concluidas.incrementAndGet();
                entrega.execute(() -> {
                    if (token.isCancelado()) return;
                    ouvinte.aoAvaliar(feitas, total);
                    if (feitas == total) ouvinte.aoConcluir();
                });
            });
        }
        pool.shutdown();
    }

    public void cancelar() {
        cancelamento.cancelar();
    }

    // ------------------------------------------------------------ dados da partida

    public int totalLances() { return lances.length; }

    public Posicao posicao(int i) { return posicoes[i]; }

    public List<String> lancesUci() { return lancesUci; }

    public String sanPt(int lance) { return posicoes[lance].sanCompleto(lances[lance]); }

    public ResultadoBusca resultado(int posicao) { return resultados.get(posicao); }

    /** Avaliação (centipeões, brancas) da posição, ou null se ainda não foi calculada. */
    public Integer avaliacao(int posicao) {
        ResultadoBusca r = resultados.get(posicao);
        return r == null ? null : r.pontosBrancas;
    }

    public boolean pronto(int lance) {
        return resultados.containsKey(lance) && resultados.containsKey(lance + 1);
    }

    public int concluidas() { return concluidas.get(); }

    public String melhorSan(int lance) {
        ResultadoBusca r = resultados.get(lance);
        if (r == null || r.melhorUci == null) return null;
        int m = posicoes[lance].uciParaLance(r.melhorUci);
        return m == 0 ? null : posicoes[lance].sanCompleto(m);
    }

    // ------------------------------------------------------------ classificação

    /** Perda de chances de vitória (pontos percentuais, do ponto de vista de quem jogou), nunca negativa. */
    public double perdaPercentual(int lance) {
        ResultadoBusca a = resultados.get(lance), d = resultados.get(lance + 1);
        if (a == null || d == null) return 0;
        if (lancesUci.get(lance).equals(a.melhorUci)) return 0;
        double antes = vitoriaDeQuemJoga(lance, a.pontosBrancas), depois = vitoriaDeQuemJoga(lance, d.pontosBrancas);
        return Math.max(0, antes - depois);
    }

    private double vitoriaDeQuemJoga(int lance, int pontosBrancas) {
        double brancas = Classificacao.chancesDeVitoria(QualidadeLances.limitar(pontosBrancas));
        return posicoes[lance].brancas ? brancas : 100 - brancas;
    }

    /** Classificação do lance, ou null enquanto faltar avaliação. */
    public Classificacao classificar(int lance) {
        if (!pronto(lance)) return null;
        ResultadoBusca a = resultados.get(lance), d = resultados.get(lance + 1);
        double perda = perdaPercentual(lance);
        int sinal = posicoes[lance].brancas ? 1 : -1;

        if (perda < 2 && ehSacrificio(lance, sinal * a.pontosBrancas, sinal * d.pontosBrancas)) return Classificacao.BRILHANTE;
        if (perda < 6 && emTeoria(lance)) return Classificacao.TEORIA;
        return Classificacao.porPerda(perda);
    }

    /** Entrega uma peça (de cavalo para cima) numa troca desfavorável e, mesmo assim, é o melhor lance e não perde. */
    private boolean ehSacrificio(int lance, int avalAntesDeQuemJoga, int avalDepoisDeQuemJoga) {
        Posicao p = posicoes[lance];
        int tipo = Math.abs(p.b[lances[lance] & 63]);
        if (Troca.valor(tipo) < 300 || tipo == Posicao.K) return false;
        boolean dahMate = posicoes[lance + 1].emXeque() && posicoes[lance + 1].lancesLegais().length == 0;
        if (dahMate) return false;
        return Troca.see(p, lances[lance]) <= -200 && avalAntesDeQuemJoga < 600 && avalDepoisDeQuemJoga > -150;
    }

    private boolean emTeoria(int lance) {
        return AberturaDetector.eTeoria(String.join(" ", lancesUci.subList(0, lance + 1)));
    }

    /** Explicação em português do lance (null enquanto faltar avaliação). */
    public String explicar(int lance) {
        Classificacao c = classificar(lance);
        if (c == null) return null;
        String abertura = AberturaDetector.detectar(String.join(" ", lancesUci.subList(0, lance + 1))).nome;
        return ExplicadorLances.explicar(posicoes[lance], lances[lance], c, resultados.get(lance), resultados.get(lance + 1), abertura);
    }

    // ------------------------------------------------------------ resumo

    /** Quantidade de lances de cada classificação (indexado por {@link Classificacao#ordinal()}). */
    public int[] contagem(boolean brancas) {
        int[] r = new int[Classificacao.values().length];
        for (int i = brancas ? 0 : 1; i < lances.length; i += 2) {
            Classificacao c = classificar(i);
            if (c != null) r[c.ordinal()]++;
        }
        return r;
    }

    /** Precisão estimada (0-100), pela fórmula de precisão baseada em chances de vitória; NaN sem dados. */
    public double precisao(boolean brancas) {
        double soma = 0;
        int n = 0;
        for (int i = brancas ? 0 : 1; i < lances.length; i += 2) {
            if (!pronto(i)) continue;
            double perda = perdaPercentual(i);
            soma += Math.max(0, Math.min(100, 103.1668 * Math.exp(-0.04354 * perda) - 3.1669));
            n++;
        }
        return n == 0 ? Double.NaN : soma / n;
    }
}
