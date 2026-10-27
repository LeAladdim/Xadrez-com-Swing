package xadrez.motor;

/** Níveis do computador: mudam a profundidade de cálculo e a chance de um lance aleatório. */
public enum NivelDificuldade {
    INICIANTE("Iniciante", 1, 40),
    FACIL("Fácil", 2, 10),
    MEDIO("Médio", 3, 0),
    DIFICIL("Difícil", 4, 0),
    MAXIMO("Máximo", 8, 0);

    public final String rotulo;
    public final int profundidade;
    /** Percentual (0-100) de lances aleatórios. */
    public final int chanceAleatoria;

    NivelDificuldade(String rotulo, int profundidade, int chanceAleatoria) {
        this.rotulo = rotulo;
        this.profundidade = profundidade;
        this.chanceAleatoria = chanceAleatoria;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
