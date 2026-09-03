package br.com.cesar.dj;

/**
 * Ponto de entrada da aplicação Mesa de DJ Multithread.
 *
 * <p>A mesa sobe com a primeira música do repertório carregada, porém <b>em silêncio absoluto</b>:
 * todas as faixas já existem, cada uma com sua thread viva, mas todas dormindo em {@code wait()}.
 * A música é montada ao vivo pelo DJ, faixa por faixa, com o comando {@code play} — que é
 * exatamente a demonstração de {@code wait()} / {@code notifyAll()} pedida na atividade.</p>
 */
public class Main {

    public static void main(String[] args) {
        Console.logSistema("Inicializando Mesa de DJ Multithread...");

        Mixer mixer = new Mixer();
        mixer.carregarMusica(Musica.billieJean());

        Console console = new Console();
        console.iniciar(mixer);
    }
}
