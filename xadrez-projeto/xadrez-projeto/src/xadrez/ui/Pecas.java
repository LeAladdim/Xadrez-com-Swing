package xadrez.ui;

import xadrez.regras.Posicao;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Peças desenhadas em vetor (espaço 100x100, depois escalado), sem depender de fontes instaladas.
 */
final class Pecas {
    private static final Shape[] FORMA = new Shape[7];
    private static final Shape[] DETALHE = new Shape[7];

    private static final BasicStroke TRACO_AURA = new BasicStroke(4.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke TRACO_CONTORNO = new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke TRACO_DETALHE = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    static {
        Area base = retangulo(27, 78, 46, 9, 6);
        base.add(retangulo(21, 86, 58, 9, 7));
        construirPeao(base);
        construirTorre(base);
        construirBispo(base);
        construirCavalo(base);
        construirDamaERei(base);
    }

    private Pecas() { }

    private static Area retangulo(double x, double y, double w, double h, double arredondamento) {
        return new Area(new RoundRectangle2D.Double(x, y, w, h, arredondamento, arredondamento));
    }

    private static Area elipse(double x, double y, double w, double h) {
        return new Area(new Ellipse2D.Double(x, y, w, h));
    }

    private static void construirPeao(Area base) {
        Path2D.Double corpo = new Path2D.Double();
        corpo.moveTo(34, 80);
        corpo.curveTo(38, 64, 43, 56, 44, 47);
        corpo.lineTo(56, 47);
        corpo.curveTo(57, 56, 62, 64, 66, 80);
        corpo.closePath();
        Area peao = new Area(corpo);
        peao.add(elipse(37.5, 19, 25, 25));
        peao.add(retangulo(35, 42, 30, 7, 5));
        peao.add(base);
        FORMA[Posicao.P] = peao;
        DETALHE[Posicao.P] = new Path2D.Double();
    }

    private static void construirTorre(Area base) {
        Area topo = retangulo(27, 19, 46, 20, 3);
        topo.subtract(new Area(new Rectangle2D.Double(37, 18, 7, 10)));
        topo.subtract(new Area(new Rectangle2D.Double(56, 18, 7, 10)));
        Path2D.Double corpo = new Path2D.Double();
        corpo.moveTo(33, 80);
        corpo.lineTo(36, 40);
        corpo.lineTo(64, 40);
        corpo.lineTo(67, 80);
        corpo.closePath();
        topo.add(new Area(corpo));
        topo.add(retangulo(31, 36, 38, 7, 3));
        topo.add(base);
        FORMA[Posicao.R] = topo;
        Path2D.Double detalhe = new Path2D.Double();
        detalhe.moveTo(34, 40);
        detalhe.lineTo(66, 40);
        DETALHE[Posicao.R] = detalhe;
    }

    private static void construirBispo(Area base) {
        Path2D.Double corpo = new Path2D.Double();
        corpo.moveTo(36, 80);
        corpo.curveTo(40, 66, 43, 58, 44, 50);
        corpo.lineTo(56, 50);
        corpo.curveTo(57, 58, 60, 66, 64, 80);
        corpo.closePath();
        Area bispo = new Area(corpo);
        bispo.add(elipse(38, 20, 24, 34));
        bispo.add(elipse(45.5, 12, 9, 9));
        bispo.add(retangulo(35, 48, 30, 7, 4));
        bispo.add(base);
        FORMA[Posicao.B] = bispo;
        Path2D.Double detalhe = new Path2D.Double();
        detalhe.moveTo(46.5, 34);
        detalhe.lineTo(55, 24);
        detalhe.moveTo(36, 51);
        detalhe.lineTo(64, 51);
        DETALHE[Posicao.B] = detalhe;
    }

    private static void construirCavalo(Area base) {
        Path2D.Double c = new Path2D.Double();
        c.moveTo(30, 80);
        c.curveTo(30, 68, 38, 62, 42, 54);
        c.lineTo(28, 54);
        c.curveTo(22, 54, 18, 50, 20, 46);
        c.curveTo(24, 36, 34, 24, 46, 18);
        c.lineTo(48, 9);
        c.lineTo(55, 17);
        c.curveTo(70, 22, 76, 40, 74, 56);
        c.curveTo(73, 66, 70, 74, 70, 80);
        c.closePath();
        Area cavalo = new Area(c);
        cavalo.add(base);
        FORMA[Posicao.N] = cavalo;
        Path2D.Double detalhe = new Path2D.Double();
        detalhe.append(new Ellipse2D.Double(35.5, 31.5, 3.4, 3.4), false); // olho
        detalhe.append(new Ellipse2D.Double(22.5, 47, 2, 2), false);       // narina
        detalhe.moveTo(58, 26);                                            // crina
        detalhe.quadTo(67, 40, 64, 58);
        DETALHE[Posicao.N] = detalhe;
    }

    private static void construirDamaERei(Area base) {
        Path2D.Double corpo = new Path2D.Double();
        corpo.moveTo(33, 80);
        corpo.curveTo(37, 66, 40, 58, 37, 49);
        corpo.lineTo(63, 49);
        corpo.curveTo(60, 58, 63, 66, 67, 80);
        corpo.closePath();

        // Dama: coroa com cinco pontas e bolinhas
        double[][] pontos = {{31, 50}, {26, 28}, {36, 42}, {39, 24}, {46, 42}, {50, 20}, {54, 42},
                {61, 24}, {64, 42}, {74, 28}, {69, 50}};
        Path2D.Double coroa = new Path2D.Double();
        coroa.moveTo(pontos[0][0], pontos[0][1]);
        for (int i = 1; i < pontos.length; i++) coroa.lineTo(pontos[i][0], pontos[i][1]);
        coroa.closePath();
        Area dama = new Area(corpo);
        dama.add(new Area(coroa));
        dama.add(retangulo(33, 47, 34, 7, 4));
        dama.add(base);
        double[][] bolas = {{26, 26}, {39, 22}, {50, 18}, {61, 22}, {74, 26}};
        for (double[] b : bolas) dama.add(elipse(b[0] - 3.8, b[1] - 3.8, 7.6, 7.6));
        FORMA[Posicao.Q] = dama;
        Path2D.Double detalheDama = new Path2D.Double();
        detalheDama.moveTo(34, 50);
        detalheDama.lineTo(66, 50);
        DETALHE[Posicao.Q] = detalheDama;

        // Rei: cabeça larga com cruz
        Area rei = new Area(corpo);
        rei.add(retangulo(30, 30, 40, 20, 10));
        rei.add(retangulo(33, 47, 34, 7, 4));
        rei.add(new Area(new Rectangle2D.Double(46, 5, 8, 27)));
        rei.add(new Area(new Rectangle2D.Double(39, 12, 22, 8)));
        rei.add(base);
        FORMA[Posicao.K] = rei;
        Path2D.Double detalheRei = new Path2D.Double();
        detalheRei.moveTo(34, 50);
        detalheRei.lineTo(66, 50);
        detalheRei.moveTo(34, 38);
        detalheRei.lineTo(66, 38);
        DETALHE[Posicao.K] = detalheRei;
    }

    /** Desenha a peça {@code tipo} (1..6) no quadrado de lado {@code tamanho} com canto superior esquerdo (x, y). */
    static void desenhar(Graphics2D g, int tipo, boolean branca, double x, double y, double tamanho) {
        AffineTransform original = g.getTransform();
        g.translate(x, y);
        g.scale(tamanho / 100.0, tamanho / 100.0);

        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new Ellipse2D.Double(18, 88, 64, 9)); // sombra
        Shape forma = FORMA[tipo];
        g.setPaint(branca
                ? new GradientPaint(0, 8, new Color(0xFFFFFF), 0, 96, new Color(0xC4C9D2))
                : new GradientPaint(0, 8, new Color(0x6A6F7A), 0, 96, new Color(0x121418)));
        g.fill(forma);
        if (!branca) { // aro claro para a peça escura se destacar nas casas escuras
            g.setColor(new Color(255, 255, 255, 120));
            g.setStroke(TRACO_AURA);
            g.draw(forma);
        }
        g.setColor(branca ? new Color(0x1B1D22) : new Color(0x040405));
        g.setStroke(TRACO_CONTORNO);
        g.draw(forma);
        g.setColor(branca ? new Color(0x2A2D34) : new Color(0xE6E8EC));
        g.setStroke(TRACO_DETALHE);
        g.draw(DETALHE[tipo]);

        g.setTransform(original);
    }
}
