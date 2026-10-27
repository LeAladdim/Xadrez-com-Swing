package xadrez.motor;

/** Sinalizador de cancelamento: cada busca tem o seu, então uma não interfere na outra. */
public final class Cancelamento {
    private volatile boolean cancelado;

    public void cancelar() { cancelado = true; }

    public boolean isCancelado() { return cancelado; }
}
