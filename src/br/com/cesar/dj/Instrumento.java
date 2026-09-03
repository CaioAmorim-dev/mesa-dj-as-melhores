package br.com.cesar.dj;

/**
 * Representa um instrumento musical individual que executa em sua própria Thread.
 * O controle de execução (play, pause, stop, bpm) utiliza mecanismos de sincronização
 * explícitos e seguros (synchronized, wait, notifyAll, volatile e interrupt).
 *
 * <p>A faixa avança sobre uma grade rítmica global ({@link RelogioMestre}): a cada volta do loop
 * ela descobre em que passo do compasso o arranjo inteiro está e toca o trecho correspondente do
 * seu {@link Padrao}. É isso que faz bateria, baixo e synth soarem como uma música só, e não como
 * três loops independentes que vão se afastando com o tempo.</p>
 */
public class Instrumento implements Runnable {

    private final String nome;
    private final String som;
    private final Padrao padrao;
    private volatile int bpm;
    private volatile EstadoFaixa estado;
    private volatile boolean ativo;

    /**
     * Objeto de lock privado para proteger a seção crítica de verificação de estado,
     * evitando exposição de monitores internos para classes externas.
     */
    private final Object lock = new Object();

    /**
     * Referência para a thread subjacente para permitir operações de interrupção e join.
     */
    private Thread thread;

    public Instrumento(String nome, String som, int bpm, Padrao padrao) {
        this(nome, som, bpm, padrao, EstadoFaixa.TOCANDO);
    }

    /**
     * @param estadoInicial estado com que a thread nasce. Subir a faixa ja em
     *                      {@link EstadoFaixa#PAUSADO} faz a thread cair direto no {@code wait()}:
     *                      a faixa existe e esta viva, mas nao emite uma unica nota ate o DJ
     *                      mandar - e nao ha janela de corrida entre o {@code start()} e um
     *                      {@code pause()} chamado logo depois.
     */
    public Instrumento(String nome, String som, int bpm, Padrao padrao, EstadoFaixa estadoInicial) {
        if (bpm <= 0) {
            throw new IllegalArgumentException("O BPM deve ser maior que 0.");
        }
        this.nome = nome;
        this.som = som;
        this.padrao = padrao;
        this.bpm = bpm;
        this.estado = (estadoInicial == null) ? EstadoFaixa.TOCANDO : estadoInicial;
        this.ativo = true;
    }

    /**
     * Inicializa e inicia a thread do instrumento.
     */
    public synchronized void iniciar() {
        if (thread == null) {
            this.thread = new Thread(this, "Thread-Instrumento-" + nome);
            this.thread.start();
        }
    }

    @Override
    public void run() {
        while (ativo) {
            // Seção crítica: verificação do estado de pausa usando wait() dentro de while.
            // Protegido contra Spurious Wakeups e sem gastar CPU (sem busy-wait).
            synchronized (lock) {
                while (estado == EstadoFaixa.PAUSADO && ativo) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        // Restaura o flag de interrupção da thread
                        Thread.currentThread().interrupt();
                        // Sai imediatamente do loop de espera caso a thread tenha sido interrompida
                        break;
                    }
                }
            }

            // Se o instrumento foi desativado durante a espera ou execução, encerra o loop
            if (!ativo) {
                break;
            }

            long intervaloMs = calcularIntervaloMs();

            // Emite o som do passo atual do compasso se o estado estiver tocando.
            // O índice vem do relógio compartilhado: a faixa entra sempre no lugar certo do
            // compasso, inclusive quando volta de um pause.
            if (estado == EstadoFaixa.TOCANDO) {
                tocarPasso(RelogioMestre.passoAtual(intervaloMs));
            }

            // Temporização da batida: dorme só o que falta até o próximo ponto da grade,
            // de forma que o atraso de uma volta não se acumule nas voltas seguintes.
            // IMPORTANTE: Thread.sleep é executado FORA do bloco synchronized
            // para não reter o lock do instrumento e não travar comandos do DJ.
            try {
                Thread.sleep(RelogioMestre.msAteProximoPasso(intervaloMs));
            } catch (InterruptedException e) {
                // Restaura o flag de interrupção para tratamento no início da próxima iteração
                Thread.currentThread().interrupt();
            }
        }

        this.estado = EstadoFaixa.PARADO;

        // A própria thread libera seu canal de áudio ao sair — assim não há corrida
        // entre o encerramento da faixa e a emissão da última nota.
        GerenciadorAudio.liberar(padrao);
    }

    /**
     * Retoma a execução do instrumento caso esteja pausado.
     */
    public void play() {
        synchronized (lock) {
            if (ativo && estado != EstadoFaixa.TOCANDO) {
                this.estado = EstadoFaixa.TOCANDO;
                lock.notifyAll(); // Acorda a thread que está no wait()
            }
        }
    }

    /**
     * Pausa a execução do instrumento sem encerrar sua thread.
     */
    public void pause() {
        boolean pausou = false;
        synchronized (lock) {
            if (ativo && estado == EstadoFaixa.TOCANDO) {
                this.estado = EstadoFaixa.PAUSADO;
                pausou = true;
            }
        }
        // Chamada ao áudio feita FORA da região crítica, para manter a seção crítica curta
        // e nunca segurar o lock durante uma operação de E/S.
        if (pausou) {
            GerenciadorAudio.silenciar(padrao);
        }
    }

    /**
     * Encerra a execução do instrumento de forma graciosa e segura.
     * Altera a flag ativa, notifica o lock e interrompe sleeps ativos.
     */
    public void stop() {
        synchronized (lock) {
            this.ativo = false;
            this.estado = EstadoFaixa.PARADO;
            lock.notifyAll(); // Acorda caso esteja aguardando em wait()
        }
        if (thread != null) {
            thread.interrupt(); // Acorda caso esteja dormindo em Thread.sleep()
        }
        GerenciadorAudio.silenciar(padrao);
    }

    /**
     * Aguarda o término da execução da thread (usado no desligamento da aplicação).
     */
    public void join() throws InterruptedException {
        if (thread != null) {
            thread.join();
        }
    }

    /**
     * Altera a velocidade (BPM) da faixa de forma segura.
     */
    public void setBpm(int bpm) {
        if (bpm <= 0) {
            throw new IllegalArgumentException("O BPM deve ser maior que 0.");
        }
        this.bpm = bpm;
    }

    /**
     * Intervalo em milissegundos entre dois passos da faixa.
     * Depende do BPM e da subdivisão do padrão: a 117 BPM, uma semínima dura 512ms
     * e uma colcheia (subdivisão 2, usada pela bateria e pelo baixo) dura 256ms.
     */
    public long calcularIntervaloMs() {
        return Math.max(1L, 60_000L / ((long) this.bpm * padrao.getSubdivisao()));
    }

    private void tocarPasso(long indicePasso) {
        GerenciadorAudio.tocar(padrao, indicePasso);
        Console.eco(nome, som);
    }

    // Getters para consulta de informações
    public String getNome() {
        return nome;
    }

    public String getSom() {
        return som;
    }

    public int getBpm() {
        return bpm;
    }

    public Padrao getPadrao() {
        return padrao;
    }

    public EstadoFaixa getEstado() {
        return estado;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
