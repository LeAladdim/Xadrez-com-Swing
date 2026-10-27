package xadrez.regras;

/** Avaliação estática de trocas (SEE): quanto material um lance ganha ou perde numa sequência de capturas na casa. */
public final class Troca {
    /** Valores em centipeões (rei muito alto: só captura por último). */
    private static final int[] VALOR = {0, 100, 320, 330, 500, 900, 20000};

    private Troca() { }

    public static int valor(int tipoPeca) {
        return VALOR[tipoPeca];
    }

    /**
     * Resultado material (em centipeões, do ponto de vista de quem joga) da sequência de capturas na casa de destino.
     * Positivo = ganha material, negativo = perde. Aproximado: ignora raios-X e cravadas.
     */
    public static int see(Posicao p, int lance) {
        int de = lance & 63, para = (lance >> 6) & 63, promocao = lance >> 12;
        int movido = Math.abs(p.b[de]), capturado = Math.abs(p.b[para]);
        if (capturado == 0 && movido == Posicao.P && para == p.ep && (de & 7) != (para & 7)) capturado = Posicao.P;

        int[] meus = p.atacantes(para, p.brancas, de);
        int[] deles = p.atacantes(para, !p.brancas, -1);
        int[] ganho = new int[40];
        int d = 0;
        ganho[0] = VALOR[capturado] + (promocao != 0 ? VALOR[promocao] - VALOR[Posicao.P] : 0);
        int naCasa = promocao != 0 ? VALOR[promocao] : VALOR[movido];
        int iMeus = 0, iDeles = 0;
        boolean vezDeles = true;
        while (d < 38) {
            if (vezDeles) {
                if (iDeles >= deles.length) break;
                d++;
                ganho[d] = naCasa - ganho[d - 1];
                naCasa = VALOR[deles[iDeles++]];
            } else {
                if (iMeus >= meus.length) break;
                d++;
                ganho[d] = naCasa - ganho[d - 1];
                naCasa = VALOR[meus[iMeus++]];
            }
            vezDeles = !vezDeles;
        }
        for (int k = d; k >= 1; k--) ganho[k - 1] = -Math.max(-ganho[k - 1], ganho[k]);
        return ganho[0];
    }
}
