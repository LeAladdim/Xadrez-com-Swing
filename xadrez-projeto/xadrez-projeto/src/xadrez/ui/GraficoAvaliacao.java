package xadrez.ui;

import xadrez.analise.AnalisadorPartida;
import xadrez.analise.Classificacao;
import xadrez.analise.QualidadeLances;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.function.IntConsumer;

/** Gráfico da avaliação ao longo da partida (área clara = vantagem das brancas). Clique para ir a uma posição. */
final class GraficoAvaliacao extends JPanel {
    private static final int MARGEM = 8;
    private final AnalisadorPartida analisador;
    private int atual;

    GraficoAvaliacao(AnalisadorPartida analisador, IntConsumer aoSelecionar) {
        this.analisador = analisador;
        setPreferredSize(new Dimension(400, 110));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        setBackground(new Color(0x2A2D34));
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { selecionar(e.getX()); }
            @Override public void mouseDragged(MouseEvent e) { selecionar(e.getX()); }
            private void selecionar(int x) {
                int n = analisador.totalLances();
                int i = (int) Math.round((x - MARGEM) / passo());
                aoSelecionar.accept(Math.max(0, Math.min(n, i)));
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    void setAtual(int posicao) {
        this.atual = posicao;
        repaint();
    }

    private double passo() {
        return Math.max(1, (getWidth() - 2.0 * MARGEM) / Math.max(1, analisador.totalLances()));
    }

    private double y(int cp) {
        double v = 2 / (1 + Math.exp(-0.00368208 * QualidadeLances.limitar(cp))) - 1;
        return getHeight() / 2.0 - v * (getHeight() / 2.0 - MARGEM);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int n = analisador.totalLances(), h = getHeight();
        double passo = passo();

        Path2D.Double area = new Path2D.Double(), linha = new Path2D.Double();
        double primeiroX = -1, ultimoX = -1;
        boolean comeco = true;
        for (int i = 0; i <= n; i++) {
            Integer cp = analisador.avaliacao(i);
            if (cp == null) continue;
            double x = MARGEM + i * passo, yy = y(cp);
            if (comeco) { area.moveTo(x, h); area.lineTo(x, yy); linha.moveTo(x, yy); primeiroX = x; comeco = false; }
            else { area.lineTo(x, yy); linha.lineTo(x, yy); }
            ultimoX = x;
        }
        if (!comeco) {
            area.lineTo(ultimoX, h);
            area.closePath();
            g.setColor(new Color(0xE6E8EC));
            g.fill(area);
            g.setColor(new Color(0x8A90A0));
            g.draw(linha);
        }
        g.setColor(new Color(220, 70, 70, 160)); // linha do equilíbrio
        g.drawLine(MARGEM, h / 2, getWidth() - MARGEM, h / 2);

        for (int i = 0; i < n; i++) { // pontos dos lances notáveis
            Classificacao c = analisador.classificar(i);
            Integer cp = analisador.avaliacao(i + 1);
            if (c == null || cp == null || !(c.eRuim() || c == Classificacao.BRILHANTE)) continue;
            g.setColor(new Color(c.rgb));
            g.fillOval((int) (MARGEM + (i + 1) * passo) - 3, (int) y(cp) - 3, 7, 7);
        }
        g.setColor(new Color(255, 170, 0));
        int xa = (int) (MARGEM + atual * passo);
        g.drawLine(xa, 0, xa, h);
    }
}
