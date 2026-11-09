package xadrez.motor;

/**
 * Níveis do computador, calibrados por medição (posições equilibradas, comparando cada lance com uma busca de referência):
 * Iniciante ~61% de precisão, Fácil ~70%, Médio ~82%, Difícil ~92%, Máximo ~99%.
 * Os níveis abaixo do Máximo erram de propósito, como uma pessoa: sem busca de capturas (visaoTatica = false) e/ou com ruído nas notas.
 */
public enum NivelDificuldade {
    INICIANTE("Iniciante", 1, 20, false, 250),
    FACIL("Fácil", 2, 10, false, 200),
    MEDIO("Médio", 3, 0, true, 90),
    DIFICIL("Difícil", 4, 0, true, 35),
    MAXIMO("Máximo", 8, 0, true, 0);

    public final String rotulo;
    public final int profundidade;
    /** Percentual (0-100) de lances totalmente aleatórios. */
    public final int chanceAleatoria;
    /** false = busca de profundidade fixa, sem quiescência (não vê capturas depois do horizonte: "pendura" peças). */
    public final boolean visaoTatica;
    /** Desvio-padrão (centipeões) do erro simulado na nota de cada lance; 0 = sem erro. */
    public final int ruidoCp;

    NivelDificuldade(String rotulo, int profundidade, int chanceAleatoria, boolean visaoTatica, int ruidoCp) {
        this.rotulo = rotulo;
        this.profundidade = profundidade;
        this.chanceAleatoria = chanceAleatoria;
        this.visaoTatica = visaoTatica;
        this.ruidoCp = ruidoCp;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
