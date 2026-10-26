package xadrez;

import xadrez.ui.JanelaPrincipal;

import javax.swing.SwingUtilities;

/** Ponto de entrada do programa. */
public final class Xadrez {

    private Xadrez() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new JanelaPrincipal().setVisible(true));
    }
}
