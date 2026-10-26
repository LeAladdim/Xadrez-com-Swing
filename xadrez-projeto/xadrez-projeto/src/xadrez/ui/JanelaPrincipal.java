package xadrez.ui;

import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

/** Janela do programa: moldura do {@link PainelPrincipal}. Abre em tela cheia (F11 alterna, Esc sai). */
public class JanelaPrincipal extends JFrame {

    private boolean telaCheia;

    public JanelaPrincipal() {
        super("Xadrez");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        PainelPrincipal painel = new PainelPrincipal();
        painel.setAcoesTela(this::alternarTelaCheia, this::sairDaTelaCheia);
        setContentPane(painel);
        setMinimumSize(new Dimension(900, 640));
        aplicarModo(true);
    }

    private void alternarTelaCheia() {
        aplicarModo(!telaCheia);
    }

    private void sairDaTelaCheia() {
        if (telaCheia) aplicarModo(false);
    }

    /** Tela cheia = janela sem borda ocupando a tela inteira (funciona bem com caixas de diálogo). */
    private void aplicarModo(boolean cheia) {
        telaCheia = cheia;
        boolean estavaVisivel = isVisible();
        Rectangle tela = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        dispose(); // necessário para trocar entre janela com e sem borda
        setUndecorated(cheia);
        if (cheia) {
            setExtendedState(NORMAL);
            setBounds(tela);
        } else {
            setSize(Math.min(1280, tela.width - 80), Math.min(820, tela.height - 80));
            setLocationRelativeTo(null);
            setExtendedState(MAXIMIZED_BOTH);
        }
        if (estavaVisivel) setVisible(true);
    }
}
