package br.com.cesar.dj;

import java.util.Scanner;

/**
 * Interface de console para controle da mesa de DJ em tempo real.
 * Processa comandos do usuário na thread principal enquanto os instrumentos tocam em segundo plano.
 * Centraliza a escrita de logs para garantir thread-safety na saída padrão (System.out).
 */
public class Console {

    /**
     * Lock explícito para sincronização das impressões na tela,
     * evitando linhas truncadas ou saídas intercaladas de forma corrompida.
     */
    private static final Object PRINT_LOCK = new Object();

    /**
     * Liga/desliga a impressão de cada batida. Fica desligado por padrão para não poluir a tela
     * durante a música, e é ligado na apresentação com 'eco on' para mostrar, na prática, as
     * saídas das várias threads se intercalando.
     * É volatile porque a thread do console escreve e as threads dos instrumentos leem.
     */
    private static volatile boolean eco = false;

    /**
     * Imprime uma mensagem no console de maneira thread-safe.
     */
    public static void log(String msg) {
        synchronized (PRINT_LOCK) {
            System.out.println(msg);
        }
    }

    /**
     * Imprime uma mensagem formatada de sistema/DJ.
     */
    public static void logSistema(String msg) {
        synchronized (PRINT_LOCK) {
            System.out.println(">>> [MESA DJ] " + msg);
        }
    }

    /**
     * Imprime erro de comando.
     */
    public static void logErro(String msg) {
        synchronized (PRINT_LOCK) {
            System.out.println(">>> [ERRO] " + msg);
        }
    }

    /**
     * Eco de uma batida individual, chamado pela thread de cada instrumento.
     * Passa pelo mesmo PRINT_LOCK das demais impressões: System.out é o recurso compartilhado
     * mais disputado da aplicação, e sem esse lock as linhas de threads diferentes sairiam picotadas.
     */
    public static void eco(String nome, String som) {
        if (eco) {
            synchronized (PRINT_LOCK) {
                System.out.println("    [" + nome + "] " + som);
            }
        }
    }

    public static void setEco(boolean ligado) {
        eco = ligado;
    }

    /**
     * Loop principal de leitura de comandos.
     */
    public void iniciar(Mixer mixer) {
        Scanner scanner = new Scanner(System.in);
        exibirCabecalho();
        mixer.listarRepertorio();
        mixer.listarFaixas();

        boolean executando = true;

        while (executando) {
            System.out.print("\nDJ> ");
            if (!scanner.hasNextLine()) {
                break;
            }

            String linha = scanner.nextLine().replace("\uFEFF", "").trim();
            if (linha.isEmpty()) {
                mixer.listarFaixas();
                continue;
            }

            String[] partes = linha.split("\\s+");
            String comando = partes[0].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

            try {
                switch (comando) {
                    case "play" -> {
                        if (partes.length < 2) {
                            logErro("Uso incorreto. Exemplo: play bateria");
                        } else {
                            mixer.play(partes[1]);
                            mixer.listarFaixas();
                        }
                    }
                    case "pause" -> {
                        if (partes.length < 2) {
                            logErro("Uso incorreto. Exemplo: pause bateria");
                        } else {
                            mixer.pause(partes[1]);
                            mixer.listarFaixas();
                        }
                    }
                    case "stop" -> {
                        if (partes.length < 2) {
                            logErro("Uso incorreto. Exemplo: stop bateria");
                        } else {
                            mixer.stop(partes[1]);
                            mixer.listarFaixas();
                        }
                    }
                    case "solo" -> {
                        if (partes.length < 2) {
                            logErro("Uso incorreto. Exemplo: solo baixo");
                        } else {
                            mixer.solo(partes[1]);
                            mixer.listarFaixas();
                        }
                    }
                    case "todos", "todas", "all" -> {
                        mixer.tocarTodas();
                        mixer.listarFaixas();
                    }
                    // "silncio" cobre o caso de o acento ser digitado: os acentos são removidos
                    // do comando antes da comparação.
                    case "silencio", "silncio", "mudo" -> {
                        mixer.pausarTodas();
                        mixer.listarFaixas();
                    }
                    case "list" -> mixer.listarFaixas();
                    case "setlist", "repertorio", "repertrio", "musicas", "msicas" -> mixer.listarRepertorio();
                    case "musica", "msica", "billie", "seven", "sweet", "save" -> {
                        boolean comandoGenerico = comando.startsWith("mus") || comando.startsWith("ms");
                        String alvo = comandoGenerico ? (partes.length >= 2 ? partes[1] : "") : comando;
                        Musica escolhida = Musica.porId(alvo);
                        if (escolhida == null) {
                            logErro("Música não encontrada: '" + alvo + "'.");
                            mixer.listarRepertorio();
                        } else {
                            mixer.carregarMusica(escolhida);
                            mixer.listarFaixas();
                        }
                    }
                    case "bpm" -> {
                        if (partes.length < 3) {
                            logErro("Uso incorreto. Exemplo: bpm bateria 140  (ou: bpm all 140)");
                        } else {
                            try {
                                int novoBpm = Integer.parseInt(partes[2]);
                                String alvo = partes[1].toLowerCase();
                                if (alvo.equals("all") || alvo.equals("todos") || alvo.equals("mesa")) {
                                    mixer.alterarBpmDeTodas(novoBpm);
                                } else {
                                    mixer.alterarBpm(partes[1], novoBpm);
                                }
                                mixer.listarFaixas();
                            } catch (NumberFormatException e) {
                                logErro("O valor do BPM deve ser um número inteiro válido.");
                            }
                        }
                    }
                    case "add" -> {
                        if (partes.length < 2) {
                            logErro("Uso incorreto. Exemplo: add guitarra OU add guitarra chick 117");
                        } else {
                            String nome = partes[1];
                            String som = (partes.length >= 3) ? partes[2] : "tshhh";
                            Musica atual = mixer.getMusicaAtual();
                            int bpm = (atual != null) ? atual.getBpm() : 120;
                            if (partes.length >= 4) {
                                try {
                                    bpm = Integer.parseInt(partes[3]);
                                } catch (NumberFormatException e) {
                                    logErro("BPM inválido. Utilizando o andamento da música (" + bpm + " BPM).");
                                }
                            }
                            mixer.adicionarFaixaExtra(nome, som, bpm);
                            mixer.listarFaixas();
                        }
                    }
                    case "sync" -> {
                        mixer.sincronizar();
                        mixer.listarFaixas();
                    }
                    case "eco" -> {
                        boolean ligar = partes.length < 2 || partes[1].equalsIgnoreCase("on");
                        setEco(ligar);
                        logSistema("Eco das batidas " + (ligar
                                ? "LIGADO - cada linha impressa é uma thread diferente escrevendo na tela."
                                : "DESLIGADO."));
                    }
                    case "help", "ajuda" -> {
                        exibirCabecalho();
                        mixer.listarFaixas();
                    }
                    case "exit", "sair" -> {
                        logSistema("Encerrando a mesa de DJ e finalizando todas as threads...");
                        mixer.pararTudoEFinalizar();
                        executando = false;
                        logSistema("Mesa de DJ desligada com sucesso. Até a próxima sessão!");
                    }
                    default -> logErro("Comando desconhecido: '" + comando + "'. Digite 'help' para ver os comandos.");
                }
            } catch (Exception e) {
                logErro("Ocorreu um erro ao processar o comando: " + e.getMessage());
            }
        }

        scanner.close();
    }

    private void exibirCabecalho() {
        log("""
        ===================================================================
                  MESA DE DJ MULTITHREAD - UMA THREAD POR FAIXA
        ===================================================================
        REPERTORIO
          setlist               - Lista as musicas disponiveis
          billie|seven|sweet|save - Carrega a musica (todas as faixas EM SILENCIO)

        MONTAR A MUSICA AO VIVO
          play <faixa>          - Traz a faixa (acorda a thread com notifyAll)
          pause <faixa>         - Tira a faixa sem encerrar a thread (wait)
          solo <faixa>          - Deixa so essa faixa tocando
          todos                 - Toca todas as faixas de uma vez
          silencio              - Pausa todas as faixas (threads seguem vivas)
          add <nome> [som] [bpm]- Cria uma faixa nova em tempo real
          stop <faixa>          - Encerra a thread daquela faixa

        AJUSTES
          list                  - Tabela de faixas, estados e BPM
          bpm <faixa> <valor>   - Muda o andamento de uma faixa
          bpm all <valor>       - Muda o andamento da mesa inteira
          sync                  - Realinha todas as faixas no compasso
          eco on | eco off      - Mostra cada batida impressa por sua thread
          help                  - Mostra esta lista
          exit                  - Encerra tudo com join() e sai
        ===================================================================
        """);
    }
}
