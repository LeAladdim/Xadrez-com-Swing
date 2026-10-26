package xadrez.ui;

import xadrez.analise.QualidadeLances;
import xadrez.motor.MotorJava;
import xadrez.motor.NivelDificuldade;
import xadrez.regras.Partida;
import xadrez.regras.Posicao;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/** Conteúdo da janela: monta a interface e coordena partida, tabuleiro, análise e motor. */
public class PainelPrincipal extends JPanel {

    private static final Color FUNDO_PAINEL = new Color(0x1F2229);

    private final Partida partida = new Partida();
    private final MotorJava engine = new MotorJava();
    private final QualidadeLances qualidade = new QualidadeLances();
    private final PainelTabuleiro tabuleiro = new PainelTabuleiro();
    private final PainelAnalise analise = new PainelAnalise();

    private final JLabel lblStatus = new JLabel(" ");
    private final JComboBox<String> modoBox = new JComboBox<>(new String[]{"Vs Computador", "PvP Local (2 Jogadores)"});
    private final JComboBox<String> ladoBox = new JComboBox<>(new String[]{"Brancas", "Pretas"});
    private final JComboBox<NivelDificuldade> nivelBox = new JComboBox<>(NivelDificuldade.values());
    private final JComboBox<TemaTabuleiro> temaBox = new JComboBox<>(TemaTabuleiro.values());
    private final JCheckBox chkSugestao = new JCheckBox("Mostrar melhor lance (seta azul)", true);

    private boolean modoPvp = false;
    private boolean humanoBranco = true;
    private boolean tabuleiroInvertido = false;
    private String ultimaSugestao;
    private Runnable aoAlternarTela = () -> { };
    private Runnable aoSairDaTelaCheia = () -> { };

    public PainelPrincipal() {
        super(new BorderLayout());
        nivelBox.setSelectedItem(NivelDificuldade.MEDIO);

        tabuleiro.setCallback(this::aoTentarJogar);
        tabuleiro.setJogadaPermitida(this::humanoPodeJogar);

        add(criarBarraSuperior(), BorderLayout.NORTH);
        add(tabuleiro, BorderLayout.CENTER);
        add(analise, BorderLayout.EAST);
        add(criarBarraStatus(), BorderLayout.SOUTH);
        configurarAtalhos();

        engine.iniciar(new MotorJava.AnaliseCallback() {
            @Override
            public void aoReceberJogada(String lanceUci, int pontosBrancas) {
                int lance = partida.getAtual().uciParaLance(lanceUci);
                if (lance == 0) return;
                qualidade.registrar(partida.getTotalLances(), pontosBrancas); // avaliação da posição antes do lance
                partida.jogar(lance);
                aposLance();
            }

            @Override
            public void aoReceberAnalise(int pontosBrancas, String melhorLanceUci) {
                qualidade.registrar(partida.getTotalLances(), pontosBrancas);
                ultimaSugestao = melhorLanceUci;
                if (chkSugestao.isSelected()) tabuleiro.setSugestao(melhorLanceUci);
                atualizarPainelAnalise();
                atualizarAvaliacao();
            }
        }, SwingUtilities::invokeLater); // callbacks sempre na thread da interface, já sem respostas obsoletas

        modoBox.addActionListener(e -> {
            modoPvp = modoBox.getSelectedIndex() == 1;
            ladoBox.setEnabled(!modoPvp);
            nivelBox.setEnabled(!modoPvp);
            aposMudanca(); // se for a vez do computador, ele passa a jogar
        });
        ladoBox.addActionListener(e -> {
            humanoBranco = ladoBox.getSelectedIndex() == 0;
            tabuleiroInvertido = !humanoBranco;
            novoJogo();
        });
        temaBox.addActionListener(e -> tabuleiro.setTema((TemaTabuleiro) temaBox.getSelectedItem()));
        chkSugestao.addActionListener(e -> tabuleiro.setSugestao(chkSugestao.isSelected() ? ultimaSugestao : null));

        aposMudanca();
    }

    /** Liga os botões/atalhos de tela cheia à janela que contém este painel. */
    public void setAcoesTela(Runnable alternarTelaCheia, Runnable sairDaTelaCheia) {
        this.aoAlternarTela = alternarTelaCheia;
        this.aoSairDaTelaCheia = sairDaTelaCheia;
    }

    // ------------------------------------------------------------ regras de turno

    private boolean humanoPodeJogar() {
        return !partida.isFim() && (modoPvp || partida.getAtual().brancas == humanoBranco);
    }

    private boolean turnoDoComputador() {
        return !modoPvp && !partida.isFim() && partida.getAtual().brancas != humanoBranco;
    }

    // ------------------------------------------------------------ ações

    private void aoTentarJogar(int lance) {
        if (!humanoPodeJogar()) return;
        partida.jogar(lance);
        aposLance();
    }

    private void novoJogo() {
        partida.reiniciar();
        qualidade.limpar();
        tabuleiro.limparSetas();
        aposMudanca();
    }

    private void desfazer() {
        // Contra o computador, volta até ser a vez do humano de novo.
        int quantos = (modoPvp || partida.getAtual().brancas != humanoBranco) ? 1 : 2;
        if (quantos > partida.getTotalLances()) return;
        for (int i = 0; i < quantos; i++) partida.desfazer();
        qualidade.descartarApos(partida.getTotalLances());
        aposMudanca();
    }

    /** Depois de qualquer lance: atualiza tudo e avisa se a partida acabou. */
    private void aposLance() {
        aposMudanca();
        if (partida.isFim()) {
            // invokeLater: o tabuleiro (com o rei em vermelho) é desenhado antes de o aviso abrir
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, partida.getTextoFim(),
                    "Fim da partida", JOptionPane.INFORMATION_MESSAGE));
        }
    }

    /** Chamado sempre que a posição ou o modo mudam: cancela trabalho antigo e decide o que o motor faz agora. */
    private void aposMudanca() {
        engine.cancelar();
        ultimaSugestao = null;
        if (partida.isFim()) qualidade.registrar(partida.getTotalLances(), valorDoFim());
        atualizarView();
        if (partida.isFim()) return;
        if (turnoDoComputador()) {
            engine.solicitarJogada(partida.getAtual(), partida.getLancesUci(), (NivelDificuldade) nivelBox.getSelectedItem());
        } else {
            engine.iniciarAnalise(partida.getAtual());
        }
    }

    /** Avaliação (brancas) de uma posição final: mate para quem deu o mate, empate = 0. */
    private int valorDoFim() {
        if (partida.getFim() != Partida.Fim.XEQUE_MATE) return 0;
        return partida.getAtual().brancas ? -QualidadeLances.PONTOS_MATE : QualidadeLances.PONTOS_MATE;
    }

    // ------------------------------------------------------------ atualização da tela

    private void atualizarView() {
        tabuleiro.atualizarMundo(partida.getAtual(), tabuleiroInvertido);
        tabuleiro.setSugestao(null);
        atualizarPainelAnalise();
        atualizarAvaliacao();
        lblStatus.setText(textoStatus());
    }

    private void atualizarPainelAnalise() {
        analise.atualizar(partida, qualidade, modoPvp, humanoBranco);
    }

    private void atualizarAvaliacao() {
        if (partida.isFim()) {
            analise.setAvaliacao("Fim de jogo", "", Color.WHITE);
        } else if (turnoDoComputador()) {
            analise.setAvaliacao("Computador pensando...", "", Color.WHITE);
        } else {
            Integer cp = qualidade.avaliacao(partida.getTotalLances());
            if (cp == null) {
                analise.setAvaliacao("Avaliação: calculando...", "", Color.WHITE);
            } else {
                Color cor = (modoPvp || Math.abs(cp) < 30) ? Color.WHITE
                        : ((cp > 0) == humanoBranco ? PainelAnalise.VERDE : PainelAnalise.VERMELHO);
                analise.setAvaliacao("Avaliação: " + QualidadeLances.formatar(cp), QualidadeLances.descricao(cp), cor);
            }
        }
    }

    private String textoStatus() {
        if (partida.isFim()) return partida.getTextoFim();
        Posicao p = partida.getAtual();
        String texto = p.brancas ? "Vez das brancas" : "Vez das pretas";
        if (p.emXeque()) texto += " — xeque!";
        if (!modoPvp) texto += turnoDoComputador() ? "  (computador pensando...)" : "  (sua vez)";
        return texto;
    }

    // ------------------------------------------------------------ montagem da interface

    private JComponent criarBarraSuperior() {
        JPanel linha1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        JPanel linha2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        for (JPanel l : new JPanel[]{linha1, linha2}) l.setBackground(FUNDO_PAINEL);

        for (JComponent c : new JComponent[]{modoBox, ladoBox, nivelBox, temaBox, chkSugestao}) c.setFocusable(false);
        chkSugestao.setOpaque(false);
        chkSugestao.setForeground(Color.LIGHT_GRAY);

        linha1.add(rotuloClaro("Modo:"));
        linha1.add(modoBox);
        linha1.add(rotuloClaro("Você joga de:"));
        linha1.add(ladoBox);
        linha1.add(rotuloClaro("Nível:"));
        linha1.add(nivelBox);
        linha1.add(rotuloClaro("Tema:"));
        linha1.add(temaBox);
        linha1.add(chkSugestao);

        linha2.add(botao("Novo jogo", this::novoJogo));
        linha2.add(botao("Desfazer lance", this::desfazer));
        linha2.add(botao("Inverter tabuleiro", () -> {
            tabuleiroInvertido = !tabuleiroInvertido;
            tabuleiro.atualizarMundo(partida.getAtual(), tabuleiroInvertido);
        }));
        linha2.add(botao("Limpar setas (C)", tabuleiro::limparSetas));
        linha2.add(botao("Tela cheia (F11)", () -> aoAlternarTela.run()));
        linha2.add(botao("Sair", () -> System.exit(0)));

        JPanel barra = new JPanel(new GridLayout(2, 1));
        barra.add(linha1);
        barra.add(linha2);
        return barra;
    }

    private JComponent criarBarraStatus() {
        lblStatus.setForeground(Color.WHITE);
        lblStatus.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblStatus.setOpaque(true);
        lblStatus.setBackground(FUNDO_PAINEL);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(8, 14, 10, 14));
        return lblStatus;
    }

    private void configurarAtalhos() {
        atalho(KeyEvent.VK_C, "limparSetas", tabuleiro::limparSetas);
        atalho(KeyEvent.VK_F11, "telaCheia", () -> aoAlternarTela.run());
        atalho(KeyEvent.VK_ESCAPE, "sairTelaCheia", () -> aoSairDaTelaCheia.run());
    }

    private void atalho(int tecla, String nome, Runnable acao) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(tecla, 0), nome);
        getActionMap().put(nome, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                acao.run();
            }
        });
    }

    private static JLabel rotuloClaro(String texto) {
        JLabel l = new JLabel(texto);
        l.setForeground(Color.LIGHT_GRAY);
        return l;
    }

    private static JButton botao(String texto, Runnable acao) {
        JButton b = new JButton(texto);
        b.setFocusable(false);
        b.addActionListener(e -> acao.run());
        return b;
    }
}
