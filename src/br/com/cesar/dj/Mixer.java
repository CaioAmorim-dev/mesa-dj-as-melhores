package br.com.cesar.dj;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mesa de mixagem responsável pelo registro, controle e ciclo de vida de todas as faixas.
 * Utiliza ConcurrentHashMap para garantir acesso e iteração thread-safe entre comandos
 * do console e outras threads concorrentes.
 */
public class Mixer {

    private final Map<String, Instrumento> faixas = new ConcurrentHashMap<>();

    /**
     * Música carregada no momento. É volatile porque a thread do console a troca (comando de
     * repertório) enquanto ela é lida para criar faixas novas.
     */
    private volatile Musica musicaAtual;

    /**
     * Registra e inicia imediatamente a thread do instrumento.
     */
    public void adicionarInstrumento(Instrumento instrumento) {
        String chave = instrumento.getNome().toLowerCase();
        if (faixas.containsKey(chave)) {
            Console.logErro("Já existe uma faixa com o nome '" + instrumento.getNome() + "'.");
            return;
        }

        faixas.put(chave, instrumento);
        instrumento.iniciar();
    }

    /**
     * Sobe o arranjo completo de uma música com TODAS as faixas em silêncio.
     *
     * <p>As threads são criadas e iniciadas normalmente, mas nascem em {@code PAUSADO}: elas caem
     * direto no {@code wait()} e não emitem uma única nota até o DJ chamar {@code play}. É o que
     * permite montar a música ao vivo, faixa por faixa, durante a apresentação.</p>
     */
    public void carregarMusica(Musica musica) {
        pararTodas();
        RelogioMestre.sincronizar();
        this.musicaAtual = musica;

        for (Musica.Faixa faixa : musica.getFaixas()) {
            adicionarInstrumento(new Instrumento(faixa.nome(), faixa.som(), musica.getBpm(),
                    faixa.padrao(), EstadoFaixa.PAUSADO));
        }

        Console.logSistema("Carregado: " + musica.getTitulo()
                + " | " + musica.getBpm() + " BPM | " + musica.getTonalidade() + ".");
        Console.logSistema("Todas as faixas estão no ar, porém em silêncio (threads em wait). "
                + "Use 'play <faixa>' para montar a música.");
    }

    /**
     * Cria uma faixa nova em tempo de execução, já tocando.
     * O padrão vem da música atual: extras previstos no arranjo mantêm o arranjo original e
     * qualquer outro nome vira um improviso na tonalidade da música.
     */
    public void adicionarFaixaExtra(String nome, String som, int bpm) {
        Musica musica = this.musicaAtual;
        if (musica == null) {
            Console.logErro("Nenhuma música carregada. Use 'setlist' para ver o repertório.");
            return;
        }
        // Se o nome fizer parte do arranjo previsto para a musica, a faixa entra com o
        // arranjo original; qualquer outro nome entra improvisando na tonalidade da musica.
        Musica.Faixa prevista = musica.faixaPorNome(nome);
        String nomeFinal = (prevista != null) ? prevista.nome() : nome;
        String somFinal = (prevista != null) ? prevista.som() : som;
        Padrao padrao = (prevista != null) ? prevista.padrao() : musica.padraoImproviso();

        adicionarInstrumento(new Instrumento(nomeFinal, somFinal, bpm, padrao, EstadoFaixa.TOCANDO));
        Console.logSistema("Faixa '" + nomeFinal + "' criada e tocando (" + padrao.getDescricao() + ").");
    }

    /**
     * Realinha todas as faixas no compasso (reinicia a grade rítmica compartilhada).
     */
    public void sincronizar() {
        RelogioMestre.sincronizar();
        Console.logSistema("Grade rítmica realinhada: todas as faixas voltam ao início do compasso.");
    }

    /**
     * Retoma a reprodução da faixa indicada.
     */
    public void play(String nome) {
        Instrumento inst = buscarInstrumento(nome);
        if (inst != null) {
            if (inst.getEstado() == EstadoFaixa.TOCANDO) {
                Console.logSistema("Faixa '" + inst.getNome() + "' já está tocando.");
            } else {
                inst.play();
                Console.logSistema("Faixa '" + inst.getNome() + "' entrou (PLAY).");
            }
        }
    }

    /**
     * Pausa a reprodução da faixa indicada.
     */
    public void pause(String nome) {
        Instrumento inst = buscarInstrumento(nome);
        if (inst != null) {
            inst.pause();
            Console.logSistema("Faixa '" + inst.getNome() + "' pausada (PAUSE).");
        }
    }

    /**
     * Coloca todas as faixas para tocar de uma vez.
     */
    public void tocarTodas() {
        if (semFaixas()) {
            return;
        }
        for (Instrumento inst : faixas.values()) {
            inst.play();
        }
        Console.logSistema("Todas as faixas entraram (PLAY geral).");
    }

    /**
     * Pausa todas as faixas de uma vez, sem encerrar nenhuma thread.
     */
    public void pausarTodas() {
        if (semFaixas()) {
            return;
        }
        for (Instrumento inst : faixas.values()) {
            inst.pause();
        }
        Console.logSistema("Silêncio: todas as faixas pausadas (as threads continuam vivas em wait).");
    }

    /**
     * Deixa tocando apenas a faixa indicada, pausando as demais.
     */
    public void solo(String nome) {
        Instrumento alvo = buscarInstrumento(nome);
        if (alvo == null) {
            return;
        }
        for (Instrumento inst : faixas.values()) {
            if (inst == alvo) {
                inst.play();
            } else {
                inst.pause();
            }
        }
        Console.logSistema("SOLO em '" + alvo.getNome() + "': as outras faixas ficaram em wait.");
    }

    /**
     * Encerra a thread da faixa indicada.
     */
    public void stop(String nome) {
        String chave = nome.toLowerCase();
        Instrumento inst = faixas.get(chave);
        if (inst != null) {
            inst.stop();
            faixas.remove(chave);
            Console.logSistema("Faixa '" + inst.getNome() + "' encerrada e removida (STOP).");
        } else {
            Console.logErro("Instrumento '" + nome + "' não encontrado.");
        }
    }

    /**
     * Altera o BPM da faixa indicada.
     */
    public void alterarBpm(String nome, int novoBpm) {
        Instrumento inst = buscarInstrumento(nome);
        if (inst != null) {
            try {
                inst.setBpm(novoBpm);
                Console.logSistema("BPM da faixa '" + inst.getNome() + "' alterado para " + novoBpm + ".");
            } catch (IllegalArgumentException e) {
                Console.logErro(e.getMessage());
            }
        }
    }

    /**
     * Altera o BPM de todas as faixas de uma vez, mantendo o arranjo junto.
     */
    public void alterarBpmDeTodas(int novoBpm) {
        if (semFaixas()) {
            return;
        }
        for (Instrumento inst : faixas.values()) {
            try {
                inst.setBpm(novoBpm);
            } catch (IllegalArgumentException e) {
                Console.logErro(e.getMessage());
                return;
            }
        }
        RelogioMestre.sincronizar();
        Console.logSistema("Andamento da mesa alterado para " + novoBpm + " BPM (todas as faixas).");
    }

    /**
     * Imprime a tabela com o estado atual de todas as faixas.
     */
    public void listarFaixas() {
        if (faixas.isEmpty()) {
            Console.logSistema("Nenhuma faixa registrada no momento. Use 'setlist' para ver o repertório.");
            return;
        }

        Musica musica = this.musicaAtual;
        String cabecalho = (musica == null) ? "MESA" : musica.getTitulo();

        StringBuilder sb = new StringBuilder();
        sb.append("\n--- ").append(cabecalho).append(" ").append("-".repeat(Math.max(3, 84 - cabecalho.length()))).append("\n");
        sb.append(String.format("%-12s | %-10s | %-9s | %-5s | %-6s | %-9s | %s\n",
                "FAIXA", "SOM", "ESTADO", "BPM", "GRADE", "PASSO", "PADRÃO"));
        sb.append("-".repeat(90)).append("\n");

        for (Instrumento inst : faixas.values()) {
            sb.append(String.format("%-12s | %-10s | %-9s | %-5d | %-6s | %-7dms | %s\n",
                    inst.getNome(),
                    inst.getSom(),
                    inst.getEstado(),
                    inst.getBpm(),
                    inst.getPadrao().getGrade(),
                    inst.calcularIntervaloMs(),
                    inst.getPadrao().getDescricao()));
        }
        sb.append("-".repeat(90));

        if (musica != null && !musica.getExtras().isEmpty()) {
            StringBuilder extras = new StringBuilder();
            for (Musica.Faixa f : musica.getExtras()) {
                if (!faixas.containsKey(f.nome().toLowerCase())) {
                    extras.append(extras.length() == 0 ? "" : ", ").append(f.nome().toLowerCase());
                }
            }
            if (extras.length() > 0) {
                sb.append("\nExtras disponíveis nesta música (comando add): ").append(extras);
            }
        }

        Console.log(sb.toString());
    }

    /**
     * Imprime o repertório disponível na mesa.
     */
    public void listarRepertorio() {
        StringBuilder sb = new StringBuilder("\n--- REPERTÓRIO DA MESA ").append("-".repeat(66)).append("\n");
        for (Musica m : Musica.catalogo()) {
            StringBuilder nomes = new StringBuilder();
            for (Musica.Faixa f : m.getFaixas()) {
                nomes.append(nomes.length() == 0 ? "" : " + ").append(f.nome().toLowerCase());
            }
            sb.append(String.format("  %-8s %-42s %3d BPM   %s\n", m.getId(), m.getTitulo(), m.getBpm(), nomes));
        }
        sb.append("-".repeat(90)).append("\n");
        sb.append("Digite o apelido da música (billie, seven, sweet, save) para carregá-la em silêncio.");
        Console.log(sb.toString());
    }

    /**
     * Encerra todas as faixas e aguarda o término de cada thread via join()
     * garantindo que nenhuma thread fique órfã.
     */
    public void pararTodas() {
        for (Instrumento inst : faixas.values()) {
            inst.stop();
        }

        for (Instrumento inst : faixas.values()) {
            try {
                inst.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Console.logErro("Interrupção ao aguardar término da faixa: " + inst.getNome());
            }
        }

        faixas.clear();
    }

    /**
     * Encerramento gracioso da aplicação: para todas as threads, faz join em cada uma
     * e só então libera o sintetizador MIDI.
     */
    public void pararTudoEFinalizar() {
        pararTodas();
        GerenciadorAudio.fechar();
    }

    /**
     * Retorna o mapa de faixas para fins de monitoramento.
     */
    public Map<String, Instrumento> getFaixas() {
        return faixas;
    }

    public Musica getMusicaAtual() {
        return musicaAtual;
    }

    private boolean semFaixas() {
        if (faixas.isEmpty()) {
            Console.logSistema("Nenhuma faixa registrada no momento.");
            return true;
        }
        return false;
    }

    private Instrumento buscarInstrumento(String nome) {
        Instrumento inst = faixas.get(nome.toLowerCase());
        if (inst == null) {
            Console.logErro("Instrumento '" + nome + "' não encontrado.");
        }
        return inst;
    }
}
