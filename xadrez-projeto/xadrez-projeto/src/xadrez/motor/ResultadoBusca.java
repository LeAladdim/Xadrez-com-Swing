package xadrez.motor;

/** Resultado da análise de uma posição: melhor lance e avaliação (sempre do ponto de vista das brancas). */
public final class ResultadoBusca {
    /** Melhor lance em notação UCI, ou null se a posição não tem lances (mate/afogamento). */
    public final String melhorUci;
    /** Centipeões; positivo = brancas melhor. Mate = +-(100000 - lances). */
    public final int pontosBrancas;

    public ResultadoBusca(String melhorUci, int pontosBrancas) {
        this.melhorUci = melhorUci;
        this.pontosBrancas = pontosBrancas;
    }
}
