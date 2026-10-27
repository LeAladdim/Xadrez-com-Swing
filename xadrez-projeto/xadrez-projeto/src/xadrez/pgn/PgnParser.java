package xadrez.pgn;

import xadrez.regras.Posicao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Leitor de PGN (formato do lichess, chess.com etc.). Valida cada lance no tabuleiro real.
 * Aceita comentários {...}, variações (...), códigos $n, anotações (!?), roque com zeros,
 * promoção sem "=", e também notação em português (C, T, D) ou coordenadas (e2e4).
 * Só a primeira partida do texto é lida.
 */
public final class PgnParser {
    private static final Pattern TAG = Pattern.compile("^\\s*\\[\\s*(\\w+)\\s+\"(.*)\"\\s*]\\s*$");
    private static final Pattern NUMERO_LANCE = Pattern.compile("^\\d+\\.+");
    private static final Pattern UCI = Pattern.compile("^[a-h][1-8][a-h][1-8][qrbnQRBN]?$");
    private static final String FEN_INICIAL = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w";

    private PgnParser() { }

    public static PartidaPgn analisar(String texto) throws PgnException {
        if (texto == null || texto.trim().isEmpty()) throw new PgnException("Nenhuma partida foi informada.");

        Map<String, String> tags = new LinkedHashMap<>();
        StringBuilder lances = new StringBuilder();
        String aviso = null;
        for (String linha : texto.replace("\uFEFF", "").replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            Matcher m = TAG.matcher(linha);
            if (m.matches()) {
                if (lances.toString().trim().length() > 0) { // começou outra partida
                    aviso = "O texto tem mais de uma partida; só a primeira foi analisada.";
                    break;
                }
                tags.put(m.group(1), m.group(2));
            } else if (!linha.trim().startsWith(";") && !linha.trim().startsWith("%")) {
                lances.append(linha).append(' ');
            }
        }
        String fen = tags.get("FEN");
        if (fen != null && !fen.startsWith(FEN_INICIAL)) {
            throw new PgnException("Partidas que começam de uma posição personalizada (tag FEN) ainda não são suportadas.");
        }

        String movetext = limpar(lances.toString());
        List<String> uci = new ArrayList<>();
        String resultado = tags.getOrDefault("Result", "*");
        Posicao pos = Posicao.inicial();
        for (String bruto : movetext.split("\\s+")) {
            String token = bruto;
            Matcher n = NUMERO_LANCE.matcher(token);
            if (n.find()) token = token.substring(n.end());
            if (token.isEmpty()) continue;
            if (token.equals("1-0") || token.equals("0-1") || token.equals("1/2-1/2") || token.equals("*")) {
                resultado = token;
                break;
            }
            token = token.replaceAll("[!?]+$", "");
            if (token.isEmpty()) continue;

            int lance = acharLance(pos, token);
            if (lance == 0) {
                int numero = uci.size() / 2 + 1;
                String lado = pos.brancas ? "brancas" : "pretas";
                if (uci.isEmpty()) throw new PgnException("Lance inválido ou ambíguo: \"" + token + "\" (lance 1, " + lado + ").");
                aviso = "Lance inválido ou ambíguo: \"" + token + "\" (lance " + numero + ", " + lado
                        + "). Foram analisados os " + uci.size() + " lances anteriores.";
                break;
            }
            uci.add(pos.lanceParaUci(lance));
            pos = pos.fazerLance(lance);
        }
        if (uci.isEmpty()) throw new PgnException("Nenhum lance foi encontrado no texto.");
        return new PartidaPgn(tags, uci, resultado, aviso);
    }

    /** Remove comentários, variações e códigos NAG. */
    private static String limpar(String s) {
        s = s.replaceAll("\\{[^}]*}", " ").replaceAll("\\$\\d+", " ");
        StringBuilder sb = new StringBuilder();
        int profundidade = 0;
        for (char c : s.toCharArray()) {
            if (c == '(') profundidade++;
            else if (c == ')') { if (profundidade > 0) profundidade--; }
            else if (profundidade == 0) sb.append(c);
        }
        return sb.toString();
    }

    private static int acharLance(Posicao pos, String token) {
        int[] legais = pos.lancesLegais();
        String alvo = normalizar(token);
        for (String[] letras : new String[][]{Posicao.LETRAS_EN, Posicao.LETRAS_PT}) { // inglês tem prioridade
            int achado = 0, quantos = 0;
            for (int m : legais) {
                if (alvo.equals(normalizar(pos.san(m, letras)))) { achado = m; quantos++; }
            }
            if (quantos == 1) return achado;
            if (quantos > 1) return 0; // ambíguo
        }
        if (UCI.matcher(token).matches()) return pos.uciParaLance(token.toLowerCase());
        return 0;
    }

    /** Ignora "+", "#", "=", "x" e escreve o roque sempre como O-O / O-O-O. */
    private static String normalizar(String san) {
        if (san.matches("[0Oo]([-–][0Oo]){1,2}[+#]?")) return san.replaceAll("[^0Oo]", "").length() == 3 ? "O-O-O" : "O-O";
        return san.replaceAll("[+#=x:]", "").replace("e.p.", "");
    }
}
