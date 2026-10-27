package xadrez.analise;

/** Quanto tempo o motor dedica a cada posição da partida analisada. */
public enum NivelAnalise {
    RAPIDA("Rápida (~0,3 s por posição)", 300, 7),
    NORMAL("Normal (~1 s por posição)", 1000, 8),
    PROFUNDA("Profunda (~2,5 s por posição)", 2500, 10);

    public final String rotulo;
    public final long tempoMs;
    public final int profundidade;

    NivelAnalise(String rotulo, long tempoMs, int profundidade) {
        this.rotulo = rotulo;
        this.tempoMs = tempoMs;
        this.profundidade = profundidade;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
