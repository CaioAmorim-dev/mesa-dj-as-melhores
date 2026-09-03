package br.com.cesar.dj;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Saída de áudio da mesa, usando o sintetizador MIDI que já vem embutido no Java
 * (javax.sound.midi) — sem nenhuma biblioteca externa.
 *
 * <p>É um <b>recurso compartilhado por todas as threads de instrumento</b>, portanto um clássico
 * ponto de seção crítica: várias faixas podem chamar {@link #tocar} no mesmo milissegundo. A
 * proteção é feita em duas camadas:</p>
 * <ul>
 *   <li>cada faixa recebe um <b>canal MIDI exclusivo</b>, então elas não disputam o mesmo timbre;</li>
 *   <li>o estado mutável de cada canal (quais notas estão soando) fica dentro de {@link Voz},
 *       cujos métodos são {@code synchronized}.</li>
 * </ul>
 *
 * <p>O mapa de vozes é um {@link ConcurrentHashMap} porque o comando {@code add} do DJ pode criar
 * uma faixa nova enquanto as outras estão tocando e escrevendo nesse mesmo mapa.</p>
 */
public class GerenciadorAudio {

    private static Synthesizer synth;
    private static MidiChannel[] canais;
    private static volatile boolean disponivel = false;

    /** O canal 9 é reservado pela especificação General MIDI para percussão. */
    private static final int CANAL_PERCUSSAO = 9;

    /** Canais melódicos livres. Acessado por threads diferentes, sempre sob o próprio monitor. */
    private static final Deque<Integer> canaisLivres = new ArrayDeque<>();

    private static final Map<Padrao, Voz> vozes = new ConcurrentHashMap<>();

    /**
     * Estado sonoro de uma faixa: seu canal MIDI e as notas que estão soando neste instante.
     */
    private static final class Voz {
        private final int canal;
        private final boolean percussao;
        private final boolean deixarSoar;
        private int[] notasSoando = Padrao.SILENCIO;

        Voz(int canal, boolean percussao, boolean deixarSoar) {
            this.canal = canal;
            this.percussao = percussao;
            this.deixarSoar = deixarSoar;
        }

        synchronized void tocar(MidiChannel ch, int[] notas, int velocidade) {
            if (notas == Padrao.SUSTENTA) {
                return; // Passo de sustentacao: deixa o acorde anterior soando.
            }
            if (notas.length == 0) {
                desligar(ch);
                return; // Passo de silencio.
            }
            // Numa faixa dedilhada (ou na percussao) a nota anterior continua decaindo:
            // e o que faz um arpejo soar como um violao, e nao como uma sequencia de notas soltas.
            if (!deixarSoar) {
                for (int nota : notasSoando) {
                    ch.noteOff(nota);
                }
            }
            for (int nota : notas) {
                ch.noteOn(nota, velocidade);
            }
            notasSoando = notas;
        }

        synchronized void desligar(MidiChannel ch) {
            // O canal e exclusivo desta faixa, entao silenciar o canal inteiro silencia
            // exatamente esta faixa - inclusive as notas que ainda estavam decaindo.
            if (!percussao) {
                ch.allNotesOff();
            }
            notasSoando = Padrao.SILENCIO;
        }
    }

    static {
        try {
            synth = MidiSystem.getSynthesizer();
            synth.open();
            canais = synth.getChannels();
            disponivel = (canais != null && canais.length > 0);

            synchronized (canaisLivres) {
                for (int i = 0; i < (canais == null ? 0 : canais.length) && i < 16; i++) {
                    if (i != CANAL_PERCUSSAO) {
                        canaisLivres.addLast(i);
                    }
                }
            }
        } catch (MidiUnavailableException e) {
            disponivel = false;
            Console.logErro("Sintetizador MIDI indisponível no sistema. A aplicação rodará em modo silencioso.");
        }
    }

    /**
     * Toca o passo indicado do padrão. Chamado pela thread do próprio instrumento, no ritmo do BPM.
     */
    public static void tocar(Padrao padrao, long indicePasso) {
        if (!disponivel || padrao == null) {
            return;
        }

        Voz voz = vozes.computeIfAbsent(padrao, GerenciadorAudio::criarVoz);
        MidiChannel canal = canalDe(voz);
        if (canal != null) {
            voz.tocar(canal, padrao.notasDoPasso(indicePasso), padrao.getVelocidade());
        }
    }

    /**
     * Cala imediatamente as notas de uma faixa (usado no {@code pause} e no {@code stop}),
     * para que um acorde sustentado não fique preso soando depois que a faixa parou.
     */
    public static void silenciar(Padrao padrao) {
        if (!disponivel || padrao == null) {
            return;
        }
        Voz voz = vozes.get(padrao);
        if (voz != null) {
            MidiChannel canal = canalDe(voz);
            if (canal != null) {
                voz.desligar(canal);
            }
        }
    }

    /**
     * Devolve o canal MIDI da faixa encerrada para o conjunto de canais livres.
     * Chamado pela própria thread do instrumento ao sair do loop, evitando corrida com {@link #tocar}.
     */
    public static void liberar(Padrao padrao) {
        if (padrao == null) {
            return;
        }
        silenciar(padrao);
        Voz voz = vozes.remove(padrao);
        if (voz != null && !voz.percussao) {
            synchronized (canaisLivres) {
                canaisLivres.addLast(voz.canal);
            }
        }
    }

    private static Voz criarVoz(Padrao padrao) {
        if (padrao.isPercussao()) {
            return new Voz(CANAL_PERCUSSAO, true, true);
        }
        int canal = alocarCanalMelodico();
        if (canal < canais.length && canais[canal] != null) {
            canais[canal].programChange(padrao.getPatch());
        }
        return new Voz(canal, false, padrao.isDeixarSoar());
    }

    private static int alocarCanalMelodico() {
        synchronized (canaisLivres) {
            Integer livre = canaisLivres.pollFirst();
            return livre != null ? livre : 0;
        }
    }

    private static MidiChannel canalDe(Voz voz) {
        return (voz.canal < canais.length) ? canais[voz.canal] : null;
    }

    /**
     * Encerra com segurança o sintetizador MIDI e libera recursos do SO.
     */
    public static void fechar() {
        if (synth != null && synth.isOpen()) {
            if (canais != null) {
                for (MidiChannel channel : canais) {
                    if (channel != null) {
                        channel.allNotesOff();
                    }
                }
            }
            synth.close();
        }
    }
}
