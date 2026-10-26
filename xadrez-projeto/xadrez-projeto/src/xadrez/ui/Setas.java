package xadrez.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/** Desenho de setas (e círculos) sobre o tabuleiro. */
final class Setas {

    private Setas() { }

    /** Centro da casa (0..63) em coordenadas do painel. */
    static double[] centro(int casa, int bx, int by, int s, boolean invertido) {
        int c = casa & 7, r = casa >> 3;
        return new double[]{
                bx + (invertido ? 7 - c : c) * s + s / 2.0,
                by + (invertido ? 7 - r : r) * s + s / 2.0};
    }

    /**
     * Desenha uma seta de {@code de} até {@code para}. Se as casas forem iguais, desenha um círculo.
     * Lances de cavalo saem em "L", mostrando a trajetória do lance.
     */
    static void desenhar(Graphics2D g, int de, int para, Color cor, int bx, int by, int s, boolean invertido) {
        g.setColor(cor);
        double[] a = centro(de, bx, by, s, invertido);

        if (de == para) {
            float espessura = Math.max(3f, s * 0.07f);
            g.setStroke(new BasicStroke(espessura));
            int margem = Math.round(espessura / 2 + 2);
            g.drawOval((int) (a[0] - s / 2.0) + margem, (int) (a[1] - s / 2.0) + margem, s - 2 * margem, s - 2 * margem);
            return;
        }

        double[] b = centro(para, bx, by, s, invertido);
        int dc = Math.abs((de & 7) - (para & 7)), dl = Math.abs((de >> 3) - (para >> 3));
        List<double[]> pontos = new ArrayList<>();
        pontos.add(a);
        if ((dc == 1 && dl == 2) || (dc == 2 && dl == 1)) { // cavalo
            pontos.add(dl == 2 ? new double[]{a[0], b[1]} : new double[]{b[0], a[1]});
        }
        pontos.add(b);

        double[] ultimo = pontos.get(pontos.size() - 1), penultimo = pontos.get(pontos.size() - 2);
        double dx = ultimo[0] - penultimo[0], dy = ultimo[1] - penultimo[1], comprimento = Math.hypot(dx, dy);
        dx /= comprimento;
        dy /= comprimento;
        double largura = s * 0.17, cabeca = s * 0.40, meiaCabeca = s * 0.26, folga = s * 0.10;
        double pontaX = ultimo[0] - dx * folga, pontaY = ultimo[1] - dy * folga;
        double baseX = pontaX - dx * cabeca, baseY = pontaY - dy * cabeca;
        pontos.set(pontos.size() - 1, new double[]{baseX, baseY});

        Path2D.Double caminho = new Path2D.Double();
        caminho.moveTo(pontos.get(0)[0], pontos.get(0)[1]);
        for (int i = 1; i < pontos.size(); i++) caminho.lineTo(pontos.get(i)[0], pontos.get(i)[1]);
        Area forma = new Area(new BasicStroke((float) largura, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                .createStrokedShape(caminho));

        Path2D.Double ponta = new Path2D.Double();
        ponta.moveTo(pontaX, pontaY);
        ponta.lineTo(baseX - dy * meiaCabeca, baseY + dx * meiaCabeca);
        ponta.lineTo(baseX + dy * meiaCabeca, baseY - dx * meiaCabeca);
        ponta.closePath();
        forma.add(new Area(ponta));
        g.fill(forma); // uma só forma: a transparência não escurece nas junções
    }
}
