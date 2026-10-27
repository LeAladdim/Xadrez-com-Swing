package xadrez.abertura;

import java.util.LinkedHashMap;
import java.util.Map;

/** Identifica o nome da abertura a partir da sequência de lances (notação UCI). */
public final class AberturaDetector {

    public static final class InfoAbertura {
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
        cadastrar("e2e4 e7e5", "Abertura do Peão do Rei", "1... e5");
        cadastrar("e2e4 e7e5 d2d4", "Jogo do Centro", "2. d4");
        cadastrar("e2e4 e7e5 g1f3", "Abertura do Cavalo do Rei", "2. Cf3");
        cadastrar("e2e4 e7e5 g1f3 b8c6", "Abertura do Cavalo do Rei", "2... Cc6");
        cadastrar("e2e4 e7e5 g1f3 b8c6 f1b5", "Abertura Ruy Lopez (Espanhola)", "3. Bb5");
        cadastrar("e2e4 e7e5 g1f3 b8c6 f1b5 a7a6", "Abertura Ruy Lopez (Espanhola)", "Defesa Morphy (3... a6)");
        cadastrar("e2e4 e7e5 g1f3 b8c6 f1c4", "Abertura Italiana", "3. Bc4");
        cadastrar("e2e4 e7e5 g1f3 b8c6 f1c4 f8c5", "Abertura Italiana", "Giuoco Piano (3... Bc5)");
        cadastrar("e2e4 e7e5 g1f3 b8c6 f1c4 g8f6", "Abertura Italiana", "Defesa dos Dois Cavalos (3... Cf6)");

        // Siciliana
        cadastrar("e2e4 c7c5", "Defesa Siciliana", "1... c5");
        cadastrar("e2e4 c7c5 g1f3", "Defesa Siciliana", "2. Cf3");
        cadastrar("e2e4 c7c5 g1f3 d7d6", "Defesa Siciliana", "2... d6");
        cadastrar("e2e4 c7c5 g1f3 d7d6 d2d4", "Defesa Siciliana", "Variante Aberta (3. d4)");
        cadastrar("e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6", "Defesa Siciliana", "Variante Najdorf");

        // Outras respostas a 1. e4
        cadastrar("e2e4 e7e6", "Defesa Francesa", "1... e6");
        cadastrar("e2e4 c7c6", "Defesa Caro-Kann", "1... c6");

        // 1. d4
        cadastrar("d2d4", "Abertura do Peão da Dama", "1. d4");
        cadastrar("d2d4 d7d5", "Abertura do Peão da Dama", "1... d5");
        cadastrar("d2d4 d7d5 c2c4", "Gambito da Dama", "2. c4");
        cadastrar("d2d4 d7d5 c1f4", "Sistema London", "2. Bf4");
        cadastrar("d2d4 g8f6", "Defesas Índias", "1... Cf6");

        // Outras primeiras jogadas
        cadastrar("c2c4", "Abertura Inglesa", "1. c4");
        cadastrar("g1f3", "Abertura Réti", "1. Cf3");
    }

    private AberturaDetector() { }

    private static void cadastrar(String sequenciaUci, String nome, String variante) {
        DICIONARIO.put(sequenciaUci.trim(), new InfoAbertura(nome, variante));
    }

    /** A sequência de lances é exatamente uma linha conhecida da teoria (ou do livro do computador)? */
    public static boolean eTeoria(String historicoUci) {
        String chave = historicoUci == null ? "" : historicoUci.trim();
        return !chave.isEmpty() && (DICIONARIO.containsKey(chave) || LivroAberturas.contem(chave));
    }

    /** Devolve a abertura conhecida mais longa que seja prefixo da sequência de lances. */
    public static InfoAbertura detectar(String historicoUci) {
        if (historicoUci == null || historicoUci.trim().isEmpty()) return DICIONARIO.get("");

        String[] lances = historicoUci.trim().split("\\s+");
        for (int i = lances.length; i > 0; i--) {
            String chave = String.join(" ", java.util.Arrays.copyOfRange(lances, 0, i));
            InfoAbertura info = DICIONARIO.get(chave);
            if (info != null) return info;
        }
        return new InfoAbertura("Jogo Fora da Teoria", "Linha Personalizada / Não Mapeada");
    }
}
