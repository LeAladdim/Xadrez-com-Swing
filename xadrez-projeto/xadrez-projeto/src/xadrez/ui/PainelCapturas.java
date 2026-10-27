package xadrez.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Collections;
import java.util.List;

/** Mostra, em miniatura, as peças capturadas de uma cor. */
final class PainelCapturas extends JPanel {
    private final boolean pecasBrancas;
    private List<Integer> pecas = Collections.emptyList();

    PainelCapturas(boolean pecasBrancas) {
        this.pecasBrancas = pecasBrancas;
        setPreferredSize(new Dimension(100, 40));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        setBackground(new Color(0x1F2229));
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    void setPecas(List<Integer> pecas) {
        this.pecas = pecas;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double tamanho = Math.min(getHeight() - 4, 32), x = 4;
        for (int tipo : pecas) {
            Pecas.desenhar(g, tipo, pecasBrancas, x, (getHeight() - tamanho) / 2, tamanho);
            x += tamanho * 0.62;
        }
    }
}
