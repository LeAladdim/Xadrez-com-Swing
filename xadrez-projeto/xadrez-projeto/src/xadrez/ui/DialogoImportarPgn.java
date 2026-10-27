package xadrez.ui;

import xadrez.analise.NivelAnalise;
import xadrez.pgn.PartidaPgn;
import xadrez.pgn.PgnException;
import xadrez.pgn.PgnParser;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.BiConsumer;

/** Diálogo para colar ou abrir um PGN (ou usar a partida atual) e escolher a profundidade da análise. */
public final class DialogoImportarPgn {

    private DialogoImportarPgn() { }

    /**
     * @param partidaAtual partida em andamento no jogo (null desativa o botão "Usar a partida atual")
     * @param aoAnalisar   recebe a partida pronta e o nível escolhido
     */
    public static void abrir(Window dono, PartidaPgn partidaAtual, BiConsumer<PartidaPgn, NivelAnalise> aoAnalisar) {
        JDialog dialogo = new JDialog(dono, "Analisar partida", JDialog.ModalityType.APPLICATION_MODAL);
        JTextArea texto = new JTextArea(16, 70);
        texto.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        texto.setLineWrap(true);
        JComboBox<NivelAnalise> nivelBox = new JComboBox<>(NivelAnalise.values());
        nivelBox.setSelectedItem(NivelAnalise.NORMAL);

        JButton arquivo = new JButton("Abrir arquivo PGN...");
        JButton atual = new JButton("Usar a partida atual");
        JButton analisar = new JButton("Analisar");
        JButton cancelar = new JButton("Cancelar");
        atual.setEnabled(partidaAtual != null);

        arquivo.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(dialogo) != JFileChooser.APPROVE_OPTION) return;
            try {
                texto.setText(lerArquivo(fc.getSelectedFile()));
                texto.setCaretPosition(0);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(dialogo, "Não foi possível ler o arquivo:\n" + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
        atual.addActionListener(e -> {
            dialogo.dispose();
            aoAnalisar.accept(partidaAtual, (NivelAnalise) nivelBox.getSelectedItem());
        });
        analisar.addActionListener(e -> {
            PartidaPgn partida;
            try {
                partida = PgnParser.analisar(texto.getText());
            } catch (PgnException ex) {
                JOptionPane.showMessageDialog(dialogo, ex.getMessage(), "Não foi possível ler a partida", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (partida.getAviso() != null) {
                JOptionPane.showMessageDialog(dialogo, partida.getAviso(), "Atenção", JOptionPane.INFORMATION_MESSAGE);
            }
            dialogo.dispose();
            aoAnalisar.accept(partida, (NivelAnalise) nivelBox.getSelectedItem());
        });
        cancelar.addActionListener(e -> dialogo.dispose());

        JPanel topo = new JPanel(new BorderLayout());
        topo.setBorder(BorderFactory.createEmptyBorder(10, 10, 4, 10));
        topo.add(new JLabel("Cole abaixo o PGN da partida (lichess, chess.com...) ou abra um arquivo:"), BorderLayout.CENTER);

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        rodape.add(arquivo);
        rodape.add(atual);
        rodape.add(new JLabel("Análise:"));
        rodape.add(nivelBox);
        rodape.add(analisar);
        rodape.add(cancelar);

        dialogo.add(topo, BorderLayout.NORTH);
        JScrollPane rolagem = new JScrollPane(texto);
        rolagem.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        dialogo.add(rolagem, BorderLayout.CENTER);
        dialogo.add(rodape, BorderLayout.SOUTH);
        dialogo.getRootPane().setDefaultButton(analisar);
        dialogo.setMinimumSize(new Dimension(760, 480));
        dialogo.pack();
        dialogo.setLocationRelativeTo(dono);
        dialogo.setVisible(true);
    }

    /** Lê o arquivo em UTF-8 (ou ISO-8859-1 se não for UTF-8 válido). */
    public static String lerArquivo(File arquivo) throws IOException {
        byte[] bytes = Files.readAllBytes(arquivo.toPath());
        String s = new String(bytes, StandardCharsets.UTF_8);
        return s.indexOf('\uFFFD') >= 0 ? new String(bytes, StandardCharsets.ISO_8859_1) : s;
    }
}
