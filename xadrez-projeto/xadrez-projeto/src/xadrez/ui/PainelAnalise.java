package xadrez.ui;

import xadrez.abertura.AberturaDetector;
import xadrez.analise.QualidadeLances;
import xadrez.regras.Material;
import xadrez.regras.Partida;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

/** Painel lateral: avaliação, material, peças capturadas, qualidade dos lances, abertura e histórico. */
class PainelAnalise extends JPanel {
    static final Color VERDE = new Color(0x7BD88F), VERMELHO = new Color(0xFF7B7B);
    private static final Color FUNDO = new Color(0x1F2229), SUAVE = new Color(0x9AA1AE);

    private final JLabel lblAvaliacao = rotulo("Avaliação: +0.00", 22, true, Color.WHITE);
    private final JLabel lblDescricao = rotulo("Posição equilibrada", 13, false, SUAVE);
    private final JLabel lblMaterial = rotulo("Material igual", 20, true, Color.WHITE);
    private final JLabel lblMaterialDetalhe = rotulo(" ", 13, false, SUAVE);
    private final PainelCapturas capturadasPelasBrancas = new PainelCapturas(false); // peças pretas capturadas
    private final PainelCapturas capturadasPelasPretas = new PainelCapturas(true);   // peças brancas capturadas
    private final JLabel lblResumo = rotulo(" ", 13, false, Color.WHITE);
    private final JLabel lblAbertura = rotulo("Posição Inicial", 15, true, VERDE);
    private final JLabel lblVariante = rotulo("Início da Partida", 13, false, SUAVE);
    private final JTextArea lista = new JTextArea();

    PainelAnalise() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setPreferredSize(new Dimension(350, 0));
        setBackground(FUNDO);

        add(secao("AVALIAÇÃO DA POSIÇÃO"));
        add(lblAvaliacao);
        add(lblDescricao);
        add(Box.createVerticalStrut(12));

        add(secao("VANTAGEM DE MATERIAL"));
        add(lblMaterial);
        add(lblMaterialDetalhe);
        add(Box.createVerticalStrut(4));
        add(rotulo("Capturadas pelas brancas", 12, false, SUAVE));
        add(capturadasPelasBrancas);
        add(rotulo("Capturadas pelas pretas", 12, false, SUAVE));
        add(capturadasPelasPretas);
        add(Box.createVerticalStrut(8));

        add(secao("QUALIDADE DOS LANCES"));
        add(lblResumo);
        add(Box.createVerticalStrut(10));

        add(secao("ABERTURA DETECTADA"));
        add(lblAbertura);
        add(lblVariante);
        add(Box.createVerticalStrut(10));

        add(secao("LANCES  (avaliação após cada lance)"));
        add(Box.createVerticalStrut(4));
        lista.setEditable(false);
        lista.setBackground(new Color(0x15171C));
        lista.setForeground(Color.LIGHT_GRAY);
        lista.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        lista.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane rolagem = new JScrollPane(lista);
        rolagem.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(rolagem);
        add(rotulo("?! imprecisão  ? erro  ?? gafe", 12, false, SUAVE));
    }

    void setAvaliacao(String texto, String descricao, Color cor) {
        lblAvaliacao.setText(texto);
        lblAvaliacao.setForeground(cor);
        lblDescricao.setText(descricao.isEmpty() ? " " : descricao);
    }

    /**
     * @param humanoBranco lado do usuário contra o computador (usado para colorir verde/vermelho)
     */
    void atualizar(Partida partida, QualidadeLances qualidade, boolean pvp, boolean humanoBranco) {
        // Material e peças capturadas
        Material material = Material.calcular(partida.getAtual());
        int saldo = material.saldo();
        lblMaterial.setText(saldo == 0 ? "Material igual" : (saldo > 0 ? "Brancas +" : "Pretas +") + Math.abs(saldo));
        lblMaterial.setForeground(saldo == 0 || pvp ? Color.WHITE : ((saldo > 0) == humanoBranco ? VERDE : VERMELHO));
        lblMaterialDetalhe.setText("Brancas " + material.pontosBrancas + " pts   ×   " + material.pontosPretas + " pts Pretas");
        capturadasPelasBrancas.setPecas(material.capturadasPelasBrancas);
        capturadasPelasPretas.setPecas(material.capturadasPelasPretas);

        // Qualidade dos lances
        int total = partida.getTotalLances();
        if (pvp) {
            lblResumo.setText("<html>Brancas: " + resumo(qualidade.resumo(true, total))
                    + "<br>Pretas: " + resumo(qualidade.resumo(false, total)) + "</html>");
        } else {
            lblResumo.setText("<html>Seus lances: " + resumo(qualidade.resumo(humanoBranco, total)) + "</html>");
        }

        // Abertura
        AberturaDetector.InfoAbertura abertura = AberturaDetector.detectar(partida.getLancesUci());
        lblAbertura.setText(abertura.nome);
        lblVariante.setText(abertura.variante);

        // Lista de lances com marca de qualidade e avaliação
        StringBuilder sb = new StringBuilder();
        List<String> san = partida.getHistoricoSan();
        for (int i = 0; i < san.size(); i++) {
            if (i % 2 == 0) sb.append(String.format("%3d. ", i / 2 + 1));
            String marca = qualidade.marca(i);
            Integer cp = qualidade.avaliacao(i + 1);
            String avaliacao = cp == null ? "..." : QualidadeLances.formatarCurto(cp);
            sb.append(String.format("%-8s%-6s", san.get(i) + (marca == null ? "" : marca), avaliacao));
            sb.append(i % 2 == 0 ? " " : "\n");
        }
        lista.setText(sb.toString());
        lista.setCaretPosition(lista.getDocument().getLength());
    }

    private static String resumo(int[] r) {
        return "<b>" + r[0] + "</b> bons · <b>" + r[1] + "</b> imprecisões · <b>" + r[2] + "</b> erros · <b>" + r[3] + "</b> gafes";
    }

    private static JLabel secao(String texto) {
        return rotulo(texto, 11, true, SUAVE);
    }

    private static JLabel rotulo(String texto, int tamanho, boolean negrito, Color cor) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("SansSerif", negrito ? Font.BOLD : Font.PLAIN, tamanho));
        l.setForeground(cor);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
}
