package xadrez.regras;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Contagem de material (peão 1, cavalo/bispo 3, torre 5, dama 9) e peças capturadas de uma posição. */
public final class Material {
    private static final int[] PONTOS = {0, 1, 3, 3, 5, 9, 0};
    private static final int[] QUANTIDADE_INICIAL = {0, 8, 2, 2, 2, 1};

    public final int pontosBrancas, pontosPretas;
    /** Tipos (1..5) das peças PRETAS que as brancas já capturaram, da mais valiosa para a menos. */
    public final List<Integer> capturadasPelasBrancas;
    /** Tipos (1..5) das peças BRANCAS que as pretas já capturaram. */
    public final List<Integer> capturadasPelasPretas;

    private Material(int pontosBrancas, int pontosPretas, List<Integer> pelasBrancas, List<Integer> pelasPretas) {
        this.pontosBrancas = pontosBrancas;
        this.pontosPretas = pontosPretas;
        this.capturadasPelasBrancas = Collections.unmodifiableList(pelasBrancas);
        this.capturadasPelasPretas = Collections.unmodifiableList(pelasPretas);
    }

    /** Positivo = brancas à frente, negativo = pretas à frente. */
    public int saldo() {
        return pontosBrancas - pontosPretas;
    }

    /** As peças capturadas são a diferença para a posição inicial (após promoções pode ficar levemente distorcida). */
    public static Material calcular(Posicao p) {
        int[] contagem = new int[13]; // índice = tipo (+6 para pretas)
        int brancas = 0, pretas = 0;
        for (int v : p.b) {
            if (v == 0) continue;
            int tipo = Math.abs(v);
            contagem[tipo + (v > 0 ? 0 : 6)]++;
            if (v > 0) brancas += PONTOS[tipo]; else pretas += PONTOS[tipo];
        }
        List<Integer> pelasBrancas = new ArrayList<>(), pelasPretas = new ArrayList<>();
        for (int tipo = Posicao.Q; tipo >= Posicao.P; tipo--) {
            for (int i = 0; i < Math.max(0, QUANTIDADE_INICIAL[tipo] - contagem[tipo + 6]); i++) pelasBrancas.add(tipo);
            for (int i = 0; i < Math.max(0, QUANTIDADE_INICIAL[tipo] - contagem[tipo]); i++) pelasPretas.add(tipo);
        }
        return new Material(brancas, pretas, pelasBrancas, pelasPretas);
    }
}
