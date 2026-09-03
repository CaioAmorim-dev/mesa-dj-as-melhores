package br.com.cesar.dj;

/** Ponto de entrada da interface web local da mesa de DJ. */
public final class MainWeb {

    private MainWeb() {
    }

    public static void main(String[] args) {
        Console.logSistema("Inicializando interface web da Mesa de DJ...");

        Mixer mixer = new Mixer();
        mixer.carregarMusica(Musica.billieJean());

        ServidorWeb servidor = new ServidorWeb(mixer, 8080);
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::fechar, "Shutdown-Mesa-DJ"));
        servidor.iniciar();
        servidor.aguardarEncerramento();
    }
}
