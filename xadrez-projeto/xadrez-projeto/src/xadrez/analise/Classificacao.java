package xadrez.analise;

/** Classificação de um lance pela perda de chances de vitória em relação ao melhor lance do motor. */
public enum Classificacao {
    BRILHANTE("!!", "Brilhante", 0x1BACA6),
    MELHOR("!", "Melhor lance", 0x6CCB7A),
    BOM("", "Bom lance", 0xA3D977),
    TEORIA("", "Teoria da abertura", 0xC9A66B),
    IMPRECISAO("?!", "Imprecisão", 0xF0C24B),
    ERRO("?", "Erro", 0xF28C38),
    GAFE("??", "Gafe", 0xF05A5A);

    public final String simbolo, rotulo;
    public final int rgb;

    Classificacao(String simbolo, String rotulo, int rgb) {
        this.simbolo = simbolo;
        this.rotulo = rotulo;
        this.rgb = rgb;
    }

    /** Chances de vitória (0..100) para as brancas, dada a avaliação em centipeões. */
    public static double chancesDeVitoria(int cp) {
        return 50 + 50 * (2 / (1 + Math.exp(-0.00368208 * cp)) - 1);
    }

    /**
     * Classificação (sem brilhante/teoria) pela perda de chances de vitória, em pontos percentuais.
     * Perto do equilíbrio, 6 pontos ≈ 0,65 peão (imprecisão), 12 ≈ 1,35 (erro) e 20 ≈ 2,3 (gafe).
     * Em posições já decididas, a mesma perda em peões pesa menos, como deve ser.
     */
    public static Classificacao porPerda(double perdaPercentual) {
        if (perdaPercentual < 1.5) return MELHOR;
        if (perdaPercentual < 6) return BOM;
        if (perdaPercentual < 12) return IMPRECISAO;
        if (perdaPercentual < 20) return ERRO;
        return GAFE;
    }

    public boolean eRuim() {
        return this == IMPRECISAO || this == ERRO || this == GAFE;
    }
}
