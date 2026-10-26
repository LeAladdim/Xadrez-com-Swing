package xadrez.analise;

import java.util.HashMap;
import java.util.Map;

/**
 * Guarda a avaliação de cada posição da partida e classifica os lances pela perda de avaliação.
 *
 * <p>A posição de índice {@code i} é a que existe depois de {@code i} lances (0 = inicial).
 * O lance {@code i} (a partir de 0) vai da posição {@code i} para a {@code i + 1}; quem o jogou
 * são as brancas quando {@code i} é par. Avaliações em centipeões, sempre do ponto de vista das brancas.
 */
public final class QualidadeLances {
    /** Valor usado para posições de xeque-mate (será limitado na exibição). */
    public static final int PONTOS_MATE = 100_000;
    private static final int LIMITE_CP = 1500;

    private final Map<Integer, Integer> avaliacoes = new HashMap<>();

    public void registrar(int posicao, int pontosBrancas) {
        avaliacoes.put(posicao, pontosBrancas);
    }

    /** Avaliação da posição, ou null se ainda não foi calculada. */
    public Integer avaliacao(int posicao) {
        return avaliacoes.get(posicao);
    }

    /** Esquece as avaliações de posições posteriores (usado ao desfazer lances). */
    public void descartarApos(int ultimaPosicao) {
        avaliacoes.keySet().removeIf(k -> k > ultimaPosicao);
    }

    public void limpar() {
        avaliacoes.clear();
    }

    /**
     * Classificação do lance: "" = bom, "?!" = imprecisão, "?" = erro, "??" = gafe.
     * Devolve null enquanto faltar a avaliação de uma das duas posições.
     */
    public String marca(int indiceLance) {
        Integer antes = avaliacoes.get(indiceLance), depois = avaliacoes.get(indiceLance + 1);
        if (antes == null || depois == null) return null;
        int sinal = (indiceLance % 2 == 0) ? 1 : -1;
        int perda = sinal * (limitar(antes) - limitar(depois));
        if (perda >= 300) return "??";
        if (perda >= 150) return "?";
        if (perda >= 60) return "?!";
        return "";
    }

    /** {bons, imprecisões, erros, gafes} dos lances de um lado que já têm avaliação. */
    public int[] resumo(boolean brancas, int totalLances) {
        int[] r = new int[4];
        for (int i = brancas ? 0 : 1; i < totalLances; i += 2) {
            String m = marca(i);
            if (m == null) continue;
            switch (m) {
                case "?!": r[1]++; break;
                case "?": r[2]++; break;
                case "??": r[3]++; break;
                default: r[0]++;
            }
        }
        return r;
    }

    // ------------------------------------------------------------ formatação (nunca fala em "mate em N")

    public static int limitar(int cp) {
        return Math.max(-LIMITE_CP, Math.min(LIMITE_CP, cp));
    }

    /** Ex.: "+0.32" (valores extremos são limitados a +-15.00). */
    public static String formatar(int cp) {
        return String.format("%+.2f", limitar(cp) / 100.0);
    }

    /** Ex.: "+0.3", para a lista de lances. */
    public static String formatarCurto(int cp) {
        return String.format("%+.1f", limitar(cp) / 100.0);
    }

    public static String descricao(int cp) {
        int a = Math.abs(cp);
        String lado = cp > 0 ? "das brancas" : "das pretas";
        if (a < 30) return "Posição equilibrada";
        if (a < 100) return "Leve vantagem " + lado;
        if (a < 300) return "Vantagem clara " + lado;
        return "Vantagem decisiva " + lado;
    }
}
