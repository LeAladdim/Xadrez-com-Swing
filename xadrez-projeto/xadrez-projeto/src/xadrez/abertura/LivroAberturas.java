package xadrez.abertura;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Pequeno livro de aberturas usado pelo computador nos primeiros lances. */
public final class LivroAberturas {

    private static final Map<String, String[]> LIVRO = new HashMap<>();

    static {
        LIVRO.put("", new String[]{"e2e4", "d2d4", "g1f3", "c2c4"});
        LIVRO.put("e2e4", new String[]{"c7c5", "e7e5", "e7e6", "c7c6"});
        LIVRO.put("e2e4 c7c5", new String[]{"g1f3", "b1c3"});
        LIVRO.put("e2e4 c7c5 g1f3", new String[]{"d7d6", "e7e6", "b8c6"});
        LIVRO.put("e2e4 e7e6", new String[]{"d2d4"});
        LIVRO.put("e2e4 e7e6 d2d4", new String[]{"d7d5"});
        LIVRO.put("e2e4 e7e5", new String[]{"g1f3", "f1c4", "d2d4"});
        LIVRO.put("e2e4 e7e5 g1f3", new String[]{"b8c6", "g8f6"});
        LIVRO.put("e2e4 e7e5 g1f3 b8c6", new String[]{"f1b5", "f1c4", "d2d4"});
        LIVRO.put("d2d4", new String[]{"d7d5", "g8f6", "e7e6"});
        LIVRO.put("d2d4 d7d5", new String[]{"c2c4", "c1f4", "g1f3"});
        LIVRO.put("d2d4 g8f6", new String[]{"c2c4", "g1f3"});
        LIVRO.put("c2c4", new String[]{"e7e5", "c7c5", "g8f6"});
    }

    private LivroAberturas() { }

    /** A sequência exata de lances é uma posição do livro (ou seja, um lance seguinte é conhecido)? */
    public static boolean contem(String historicoUci) {
        return LIVRO.containsKey(historicoUci.trim());
    }

    /** Sorteia um lance do livro para a sequência exata de lances, ou devolve null se não houver. */
    public static String sortear(String historicoUci, Random rnd) {
        String chave = historicoUci == null ? "" : historicoUci.trim();
        String[] opcoes = LIVRO.get(chave);
        return (opcoes == null || opcoes.length == 0) ? null : opcoes[rnd.nextInt(opcoes.length)];
    }
}
