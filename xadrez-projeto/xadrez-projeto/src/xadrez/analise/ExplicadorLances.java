package xadrez.analise;

import xadrez.motor.ResultadoBusca;
import xadrez.regras.Posicao;
import xadrez.regras.Troca;

import java.util.ArrayList;
import java.util.List;

/**
 * Gera, em português, a explicação de um lance a partir de fatos da posição real:
 * o que o motor preferia, material que se perde/ganha (avaliação de trocas), mates e ideias do melhor lance.
 * É uma explicação automática e aproximada: não substitui a análise de um jogador.
 */
final class ExplicadorLances {
    private static final String[] NOME = {"", "peão", "cavalo", "bispo", "torre", "dama", "rei"};
    private static final String[] COM_ARTIGO = {"", "o peão", "o cavalo", "o bispo", "a torre", "a dama", "o rei"};

    private ExplicadorLances() { }

    /**
     * @param antes     posição antes do lance
     * @param lance     lance jogado
     * @param melhor    avaliação e melhor lance do motor na posição ANTES do lance
     * @param depois    avaliação e melhor resposta do motor na posição DEPOIS do lance
     * @param abertura  nome da abertura (para lances de teoria)
     */
    static String explicar(Posicao antes, int lance, Classificacao cl, ResultadoBusca melhor, ResultadoBusca depois, String abertura) {
        Posicao posDepois = antes.fazerLance(lance);
        String sanJogado = antes.sanCompleto(lance);
        int melhorLance = melhor.melhorUci == null ? 0 : antes.uciParaLance(melhor.melhorUci);
        String sanMelhor = melhorLance == 0 ? null : antes.sanCompleto(melhorLance);
        boolean eMelhor = melhorLance == lance;
        int sinal = antes.brancas ? 1 : -1;
        int avalAntes = sinal * melhor.pontosBrancas, avalDepois = sinal * depois.pontosBrancas; // de quem jogou

        switch (cl) {
            case BRILHANTE:
                return "Sacrifício brilhante! A peça em " + casa((lance >> 6) & 63) + " pode ser capturada, mas o motor mostra que "
                        + "a posição continua " + descricao(avalDepois) + " para você. Lances assim abrem mão de material "
                        + "para conseguir algo maior.";
            case MELHOR:
                if (sanJogado.endsWith("#")) return "Xeque-mate! Fim da partida.";
                if (eMelhor) return "É o melhor lance do motor nesta posição." + (sanJogado.endsWith("+") ? " Além disso, dá xeque." : "");
                return "Equivale ao melhor lance do motor" + (sanMelhor != null ? " (" + sanMelhor + ")" : "") + ": praticamente a mesma avaliação.";
            case TEORIA:
                return "Lance de teoria: " + abertura + ". Bem conhecido e seguro.";
            case BOM:
                return "Lance sólido" + (sanMelhor != null ? ", mas " + sanMelhor + " era um pouco mais preciso (" + formatar(melhor) + ")." : ".");
            default:
                break;
        }

        // Imprecisão, erro ou gafe: explica o motivo e mostra o melhor lance
        StringBuilder t = new StringBuilder();
        t.append("A avaliação passou de ").append(formatar(melhor)).append(" para ").append(formatar(depois))
                .append(" (do ponto de vista das brancas).\n\n");

        int resposta = depois.melhorUci == null ? 0 : posDepois.uciParaLance(depois.melhorUci);
        String sanResposta = resposta == 0 ? null : posDepois.sanCompleto(resposta);
        String motivo = null;

        if (QualidadeLances.ehMate(depois.pontosBrancas) && avalDepois < 0) {
            int n = QualidadeLances.lancesAteMate(depois.pontosBrancas);
            motivo = "Permite xeque-mate em " + n + (n == 1 ? " lance" : " lances")
                    + (sanResposta != null ? ": o adversário responde " + sanResposta + "." : ".");
        } else if (QualidadeLances.ehMate(melhor.pontosBrancas) && avalAntes > 0 && sanMelhor != null) {
            int n = QualidadeLances.lancesAteMate(melhor.pontosBrancas);
            motivo = "Havia xeque-mate forçado em " + n + (n == 1 ? " lance" : " lances") + ", começando com " + sanMelhor + ".";
        } else if (resposta != 0 && capturaReal(posDepois, resposta)) {
            int ganho = Troca.see(posDepois, resposta);
            if (ganho >= 100) {
                int para = (resposta >> 6) & 63;
                int tipo = tipoCapturado(posDepois, resposta);
                int defensores = posDepois.atacantes(para, !posDepois.brancas, -1).length;
                motivo = "Depois de " + sanJogado + ", o adversário pode jogar " + sanResposta + " e capturar "
                        + COM_ARTIGO[tipo] + " em " + casa(para)
                        + (defensores == 0 ? ", que ficou sem proteção." : ", e mesmo com proteção a troca o favorece em cerca de "
                        + pontos(ganho) + ".");
            }
        }
        if (motivo == null && melhorLance != 0 && capturaReal(antes, melhorLance) && Troca.see(antes, melhorLance) >= 100) {
            int tipo = tipoCapturado(antes, melhorLance);
            motivo = "Perdeu a captura " + sanMelhor + ", que ganhava " + COM_ARTIGO[tipo] + " em "
                    + casa((melhorLance >> 6) & 63) + ".";
        }
        if (motivo != null) t.append(motivo).append("\n\n");

        if (sanMelhor != null) {
            t.append("Melhor era ").append(sanMelhor).append(" (").append(formatar(melhor)).append("). ")
                    .append(ideiaDoMelhor(antes, melhorLance, sanMelhor));
        }
        return t.toString().trim();
    }

    /** Uma frase sobre a ideia do melhor lance. */
    private static String ideiaDoMelhor(Posicao antes, int melhor, String san) {
        int de = melhor & 63, para = (melhor >> 6) & 63, tipo = Math.abs(antes.b[de]);
        if (san.startsWith("O-O")) return "O roque deixa o rei mais seguro e coloca a torre em jogo.";
        if (san.endsWith("#")) return "Dá xeque-mate.";
        if (capturaReal(antes, melhor)) return "Captura " + COM_ARTIGO[tipoCapturado(antes, melhor)] + " em " + casa(para) + ".";
        if (san.endsWith("+")) return "Dá xeque e mantém a iniciativa.";
        int linhaCasa = antes.brancas ? 7 : 0;
        if ((tipo == Posicao.N || tipo == Posicao.B) && (de >> 3) == linhaCasa) return "Desenvolve " + COM_ARTIGO[tipo] + " para uma casa ativa.";
        if (tipo == Posicao.P && (para == 27 || para == 28 || para == 35 || para == 36)) return "Ocupa o centro com o peão.";
        return "É o lance que o motor considera mais forte aqui, mantendo melhor a coordenação das peças.";
    }

    static boolean capturaReal(Posicao p, int m) {
        int de = m & 63, para = (m >> 6) & 63;
        return p.b[para] != 0 || (Math.abs(p.b[de]) == Posicao.P && para == p.ep && (de & 7) != (para & 7));
    }

    private static int tipoCapturado(Posicao p, int m) {
        int t = Math.abs(p.b[(m >> 6) & 63]);
        return t == 0 ? Posicao.P : t; // en passant
    }

    private static String casa(int sq) {
        return "" + (char) ('a' + (sq & 7)) + (8 - (sq >> 3));
    }

    private static String formatar(ResultadoBusca r) {
        return QualidadeLances.formatar(r.pontosBrancas);
    }

    private static String pontos(int cp) {
        return String.format("%.1f", cp / 100.0).replace(".0", "") + (cp >= 200 ? " pontos" : " ponto");
    }

    private static String descricao(int cpDeQuemJogou) {
        if (cpDeQuemJogou > 300) return "muito favorável";
        if (cpDeQuemJogou > 100) return "claramente melhor";
        if (cpDeQuemJogou > 30) return "levemente melhor";
        if (cpDeQuemJogou > -30) return "equilibrada";
        return "defensável";
    }

    /** Nome da peça (sem artigo), para uso externo. */
    static String nome(int tipo) {
        return NOME[tipo];
    }
}
