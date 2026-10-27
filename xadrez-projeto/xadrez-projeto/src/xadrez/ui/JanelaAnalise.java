package xadrez.ui;

import xadrez.analise.NivelAnalise;
import xadrez.pgn.PartidaPgn;

import javax.swing.JDialog;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Janela da análise de partida (não bloqueia a janela do jogo). */
public final class JanelaAnalise extends JDialog {

    public JanelaAnalise(Window dono, PartidaPgn partida, NivelAnalise nivel, PainelRevisao.AcaoContinuar acao, Runnable aoFechar) {
        super(dono, "Análise da partida", ModalityType.MODELESS);
        PainelRevisao painel = new PainelRevisao(partida, nivel, acao, this::dispose);
        setContentPane(painel);
        setMinimumSize(new Dimension(1000, 700));
        if (dono != null && dono.isShowing()) {
            Rectangle area = dono.getBounds();
            setBounds(area);
        } else {
            setSize(1300, 850);
            setLocationRelativeTo(null);
        }
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                painel.encerrar();
                if (aoFechar != null) aoFechar.run();
            }
        });
    }
}
