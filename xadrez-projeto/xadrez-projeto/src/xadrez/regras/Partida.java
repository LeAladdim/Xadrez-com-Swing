package xadrez.regras;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Estado de uma partida: posição atual, histórico de lances (para desfazer) e detecção de fim de jogo. */
public class Partida {

    public enum Fim { NENHUM, XEQUE_MATE, AFOGAMENTO, CINQUENTA_LANCES, REPETICAO, MATERIAL_INSUFICIENTE }

    private Posicao atual = Posicao.inicial();
    private final List<Posicao> posicoesAnteriores = new ArrayList<>();
    private final List<String> historicoUci = new ArrayList<>();
    private final List<String> historicoSan = new ArrayList<>();
    private Fim fim = Fim.NENHUM;

    public void jogar(int lance) {
        String notacao = atual.san(lance);
        posicoesAnteriores.add(atual);
        historicoUci.add(atual.lanceParaUci(lance));
        atual = atual.fazerLance(lance);
        if (atual.lancesLegais().length == 0) {
            if (atual.emXeque()) notacao += "#";
        } else if (atual.emXeque()) {
            notacao += "+";
        }
        historicoSan.add(notacao);
        atualizarFim();
    }

    public void desfazer() {
        if (posicoesAnteriores.isEmpty()) return;
        int ultimo = posicoesAnteriores.size() - 1;
        atual = posicoesAnteriores.remove(ultimo);
        historicoUci.remove(ultimo);
        historicoSan.remove(ultimo);
        atualizarFim();
    }

    public void reiniciar() {
        atual = Posicao.inicial();
        posicoesAnteriores.clear();
        historicoUci.clear();
        historicoSan.clear();
        fim = Fim.NENHUM;
    }

    private void atualizarFim() {
        if (atual.lancesLegais().length == 0) {
            fim = atual.emXeque() ? Fim.XEQUE_MATE : Fim.AFOGAMENTO;
        } else if (atual.half >= 100) {
            fim = Fim.CINQUENTA_LANCES;
        } else if (atual.materialInsuficiente()) {
            fim = Fim.MATERIAL_INSUFICIENTE;
        } else {
            int repeticoes = 0;
            String chave = atual.chave();
            for (Posicao p : posicoesAnteriores) if (p.chave().equals(chave)) repeticoes++;
            fim = repeticoes >= 2 ? Fim.REPETICAO : Fim.NENHUM;
        }
    }

    public boolean isFim() { return fim != Fim.NENHUM; }

    public Fim getFim() { return fim; }

    public String getTextoFim() {
        switch (fim) {
            case XEQUE_MATE: return "Xeque-mate! Vitória das " + (atual.brancas ? "Pretas" : "Brancas") + ".";
            case AFOGAMENTO: return "Empate por afogamento.";
            case CINQUENTA_LANCES: return "Empate pela regra dos 50 lances.";
            case REPETICAO: return "Empate por tripla repetição.";
            case MATERIAL_INSUFICIENTE: return "Empate por material insuficiente.";
            default: return "";
        }
    }

    public Posicao getAtual() { return atual; }

    public int getTotalLances() { return historicoUci.size(); }

    /** Lances em notação UCI separados por espaço (ex.: "e2e4 e7e5"). */
    public String getLancesUci() { return String.join(" ", historicoUci); }

    public List<String> getHistoricoSan() { return Collections.unmodifiableList(historicoSan); }
}
