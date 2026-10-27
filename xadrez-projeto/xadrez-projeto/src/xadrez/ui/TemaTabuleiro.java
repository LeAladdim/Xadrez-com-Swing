package xadrez.ui;

import java.awt.Color;

/** Esquemas de cores do tabuleiro. */
public enum TemaTabuleiro {
    AZUL("Azul", 0xEAE9D6, 0x4B7399),
    VERDE("Verde", 0xEEEED2, 0x5C8A47),
    MADEIRA("Madeira", 0xF3E1B8, 0x9A6A3F);

    public final String rotulo;
    final Color clara, escura;

    TemaTabuleiro(String rotulo, int clara, int escura) {
        this.rotulo = rotulo;
        this.clara = new Color(clara);
        this.escura = new Color(escura);
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
