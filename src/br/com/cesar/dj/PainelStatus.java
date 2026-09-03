package br.com.cesar.dj;

/**
 * Thread auxiliar (Daemon) que pode monitorar periodicamente a situação das faixas.
 * Sendo uma daemon thread, ela não impede a JVM de encerrar quando a thread principal terminar.
 */
public class PainelStatus implements Runnable {

    private final Mixer mixer;
    private final int intervaloSegundos;
    private volatile boolean ativo = true;

    public PainelStatus(Mixer mixer, int intervaloSegundos) {
        this.mixer = mixer;
        this.intervaloSegundos = intervaloSegundos;
    }

    public void iniciar() {
        Thread thread = new Thread(this, "Thread-PainelStatus");
        thread.setDaemon(true); // Thread daemon: encerra automaticamente quando o programa principal finaliza
        thread.start();
    }

    @Override
    public void run() {
        while (ativo) {
            try {
                Thread.sleep(intervaloSegundos * 1000L);
                // Pode ser usado para logging periódico do estado
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void parar() {
        this.ativo = false;
    }
}
