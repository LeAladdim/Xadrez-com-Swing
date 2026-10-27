package xadrez.pgn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Uma partida lida de um PGN: cabeçalhos, lances (validados, em notação UCI) e resultado. */
public final class PartidaPgn {
    private final Map<String, String> tags = new LinkedHashMap<>();
    private final List<String> lancesUci = new ArrayList<>();
    private String resultado = "*";
    private String aviso;

    public PartidaPgn() { }

    public PartidaPgn(Map<String, String> tags, List<String> lancesUci, String resultado, String aviso) {
        this.tags.putAll(tags);
        this.lancesUci.addAll(lancesUci);
        this.resultado = resultado;
        this.aviso = aviso;
    }

    public String tag(String nome, String padrao) {
        String v = tags.get(nome);
        return v == null || v.trim().isEmpty() || v.equals("?") ? padrao : v.trim();
    }

    public Map<String, String> getTags() { return Collections.unmodifiableMap(tags); }

    public List<String> getLancesUci() { return Collections.unmodifiableList(lancesUci); }

    public String getResultado() { return resultado; }

    /** Observação do leitor (ex.: partida cortada num lance inválido); null se tudo certo. */
    public String getAviso() { return aviso; }

    public String getBrancas() { return tag("White", "Brancas"); }

    public String getPretas() { return tag("Black", "Pretas"); }
}
