package xadrez.pgn;

/** Erro ao ler uma partida em PGN (a mensagem é mostrada ao usuário). */
public class PgnException extends Exception {
    private static final long serialVersionUID = 1L;

    public PgnException(String mensagem) {
        super(mensagem);
    }
}
