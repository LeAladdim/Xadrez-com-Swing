package xadrez;

import xadrez.analise.NivelAnalise;
import xadrez.pgn.PartidaPgn;
import xadrez.pgn.PgnException;
import xadrez.pgn.PgnParser;
import xadrez.ui.DialogoImportarPgn;
import xadrez.ui.JanelaAnalise;
import xadrez.ui.JanelaPrincipal;
import xadrez.ui.PainelRevisao;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Programa de análise de partidas. Uso: {@code java -cp Xadrez.jar xadrez.AnalisadorPgn [arquivo.pgn]}.
 * Sem argumento, abre uma janela para colar ou abrir o PGN.
 */
public final class AnalisadorPgn {

    private AnalisadorPgn() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            if (args.length > 0) {
                try {
                    PartidaPgn partida = PgnParser.analisar(DialogoImportarPgn.lerArquivo(new File(args[0])));
                    abrirAnalise(partida, NivelAnalise.NORMAL);
                } catch (IOException | PgnException e) {
                    JOptionPane.showMessageDialog(null, e.getMessage(), "Não foi possível abrir a partida", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            } else {
                DialogoImportarPgn.abrir(null, null, AnalisadorPgn::abrirAnalise);
                // se o diálogo foi cancelado, nenhuma janela de análise foi aberta
                if (java.awt.Window.getWindows().length == 0 || noWindowVisible()) System.exit(0);
            }
        });
    }

    private static boolean noWindowVisible() {
        for (java.awt.Window w : java.awt.Window.getWindows()) if (w.isVisible()) return false;
        return true;
    }

    private static void abrirAnalise(PartidaPgn partida, NivelAnalise nivel) {
        PainelRevisao.AcaoContinuar continuar = AnalisadorPgn::continuarJogando;
        new JanelaAnalise(null, partida, nivel, continuar, () -> {
            if (noWindowVisible()) System.exit(0);
        }).setVisible(true);
    }

    /** "Jogar a partir daqui": abre a janela do jogo já na posição escolhida. */
    private static void continuarJogando(List<String> lances, int ate, PainelRevisao.ModoContinuacao modo, Map<Integer, Integer> avaliacoes) {
        JanelaPrincipal janela = new JanelaPrincipal();
        janela.getPainel().continuarPartida(lances, ate, modo, avaliacoes);
        janela.setVisible(true);
    }
}
