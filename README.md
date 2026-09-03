# Mesa de DJ Multithread (Java 17)

Aplicação de console para a disciplina de **Infraestrutura de Software**. Cada faixa musical de um arranjo executa de forma concorrente e autônoma **em sua própria `Thread`**, e o usuário atua como DJ: traz faixas, pausa, encerra e muda o andamento em tempo real, sem que uma faixa interfira nas demais.

A mesa tem um repertório de três músicas e sobe **em silêncio absoluto**: todas as threads já estão vivas, dormindo em `wait()`, e a música é montada ao vivo — faixa por faixa — pelos comandos do console. O som sai pelo sintetizador MIDI que já vem embutido no JDK (`javax.sound.midi`), sem nenhuma biblioteca externa.

---

## Como executar

**Pré-requisito:** JDK 17 ou superior no `PATH`.

```bash
javac -encoding UTF-8 -d out src/br/com/cesar/dj/*.java
java -cp out br.com.cesar.dj.Main
```

No Windows, o `run.bat` faz as duas coisas.

---

## Atendimento aos requisitos da atividade

Esta seção mapeia cada exigência do guia da disciplina para o ponto do código onde ela é cumprida.

### Mecanismos de sincronização exigidos

| Exigência | Como foi implementado | Onde |
|---|---|---|
| Uma thread por instrumento | `Instrumento implements Runnable`, com uma `Thread` dedicada por faixa | `Instrumento.java` |
| Pausar e retomar com `synchronized` + `wait()` / `notifyAll()` | A thread entra em bloco sincronizado e chama `lock.wait()`; o comando `play` altera o estado no mesmo monitor e chama `notifyAll()` | `Instrumento.java:71-77` e `:121-126` |
| `wait()` dentro de `while`, nunca de `if` | `while (estado == PAUSADO && ativo)` — proteção contra *spurious wakeups* | `Instrumento.java:72` |
| Encerrar com flag `volatile` + `interrupt()` | `volatile boolean ativo`, `notifyAll()` para acordar quem espera e `thread.interrupt()` para acordar quem dorme | `Instrumento.java:20` e `:152-159` |
| `Thread.sleep()` **fora** do bloco `synchronized` | O `sleep` da temporização acontece depois da região crítica, para não reter o monitor durante a espera | `Instrumento.java:103` |
| BPM ajustável com acesso seguro | Campo `volatile int bpm`, com o intervalo recalculado a cada volta do laço | `Instrumento.java` |
| Lista de faixas em `ConcurrentHashMap` | O comando `add` insere no mapa enquanto ele é percorrido para listar e encerrar faixas | `Mixer.java:13` |
| Impressão sincronizada no console | Toda escrita em `System.out` passa por um monitor único (`PRINT_LOCK`) | `Console.java:16` |
| Encerramento com `join()` | `pararTodas()` faz `stop()` e depois `join()` em cada faixa — usado tanto no `exit` quanto na troca de música | `Mixer.java:276` |

### Armadilhas que o guia manda evitar

| Armadilha | Situação no projeto |
|---|---|
| `while (pausado) {}` (busy-wait) | Não existe. A pausa usa `wait()`, e a thread fica em estado `WAITING` sem consumir CPU |
| `Thread.stop()` / `Thread.suspend()` | Não aparecem no código |
| `Thread.sleep()` dentro do `synchronized` | Não ocorre: o `sleep` está fora da região crítica |
| `if (pausado) wait();` | Não ocorre: a verificação é sempre em `while` |
| `notify()` no lugar de `notifyAll()` | Só `notifyAll()` é usado |
| `HashMap` compartilhado entre threads | Todas as coleções compartilhadas são `ConcurrentHashMap` |
| Esquecer o `join()` no encerramento | O `exit` aguarda cada thread antes de sair |
| Campo de estado sem `volatile` | `ativo`, `estado` e `bpm` são `volatile` |

### Checklist de entrega

- [x] Compila com `javac -Xlint:all` sem nenhum warning
- [x] Três ou mais faixas tocando simultaneamente, com saídas intercaladas (comando `eco on`)
- [x] `pause` em uma faixa não afeta as demais
- [x] `play` retoma exatamente a faixa pausada
- [x] Faixa pausada não consome CPU (verificável no Gerenciador de Tarefas)
- [x] `exit` encerra tudo sem exceção e sem thread pendurada
- [x] Nenhum `Thread.stop()` ou `Thread.suspend()` no código
- [x] README com instruções de execução
- [ ] Commits de todos os integrantes *(pendente: depende de cada membro commitar)*

### Além do que foi pedido

Três itens que o guia lista como extras opcionais estão implementados: **BPM real** (`sleep(60000 / bpm)`, ajustável por faixa ou pela mesa inteira), **criação de faixas em tempo de execução** (`add`, com `ConcurrentHashMap` e `start()` sem parar a música) e a thread daemon auxiliar (`PainelStatus`).

Foi acrescentado ainda um **relógio mestre** (`RelogioMestre.java`). Em vez de cada thread dormir "um intervalo a partir de agora" — o que faz o erro se acumular e as faixas saírem do tempo umas das outras —, todas calculam o **instante absoluto** do próximo passo a partir de uma origem comum. É o que permite que as faixas soem como um arranjo único, e que uma faixa retomada por `play` volte no lugar certo do compasso em vez de voltar de onde parou.

---

## Repertório

| Comando | Música | Andamento | Faixas iniciais | Extras (`add`) |
|---|---|---|---|---|
| `billie` | Billie Jean — Michael Jackson | 117 BPM, Fá# menor | bateria, baixo, synth | guitarra, vocal |
| `seven` | Seven Nation Army — The White Stripes | 124 BPM, Mi menor | bateria, baixo, guitarra | palmas, solo |
| `sweet` | Sweet Dreams — Eurythmics | 126 BPM, Dó menor | riff, baixo, bateria | pad, solo |

Cada faixa é uma thread; cada música é apenas um conjunto diferente de faixas. Todo o material musical fica em `Musica.java` e `Padrao.java`, separado do código de concorrência — acrescentar uma música é escrever dados, não lógica de threads.

> Os arranjos são **aproximações** montadas para a demonstração, com os timbres do banco de sons General MIDI. Não são transcrições das gravações originais.

---

## Comandos

### Repertório
| Comando | Descrição |
|---|---|
| `setlist` | Lista as músicas disponíveis |
| `billie` / `seven` / `sweet` | Carrega a música com todas as faixas **em silêncio** |

### Montar a música ao vivo
| Comando | Descrição |
|---|---|
| `play <faixa>` | Traz a faixa (`notifyAll` acorda a thread), entrando no compasso |
| `pause <faixa>` | Tira a faixa sem encerrar sua thread (`wait`) |
| `solo <faixa>` | Deixa só essa faixa tocando; as demais vão para `wait` |
| `todos` | Toca todas as faixas de uma vez |
| `silencio` | Pausa todas as faixas (as threads seguem vivas) |
| `add <nome> [som] [bpm]` | Cria e inicia uma faixa nova em tempo de execução |
| `stop <faixa>` | Encerra definitivamente a thread daquela faixa |

### Ajustes
| Comando | Descrição |
|---|---|
| `list` | Tabela com faixa, estado, BPM, grade rítmica e padrão |
| `bpm <faixa> <valor>` | Muda o andamento de uma faixa |
| `bpm all <valor>` | Muda o andamento da mesa inteira |
| `sync` | Realinha todas as faixas no início do compasso |
| `eco on` / `eco off` | Mostra cada batida impressa por sua thread — prova visual do paralelismo |
| `help` | Exibe o menu de ajuda |
| `exit` | Encerra todas as faixas com `join()` e fecha o programa |

---

## Estrutura do código

```
src/br/com/cesar/dj/
├── Main.java              ponto de entrada: monta o mixer e chama o console
├── Instrumento.java       a thread da faixa (wait/notify, volatile, interrupt)
├── EstadoFaixa.java       enum: TOCANDO, PAUSADO, PARADO
├── RelogioMestre.java     grade rítmica compartilhada (temporização absoluta)
├── Padrao.java            estrutura de um padrão rítmico/melódico
├── Musica.java            catálogo: as três músicas e seus arranjos
├── GerenciadorAudio.java  saída MIDI thread-safe (um canal por faixa)
├── Mixer.java             registro thread-safe das faixas (ConcurrentHashMap)
├── Console.java           leitor de comandos e sincronizador de log
└── PainelStatus.java      thread daemon auxiliar de monitoramento
```

---

## Arquitetura

O projeto foi modelado antes da implementação para definir as responsabilidades e os relacionamentos entre as classes.

![Diagrama de classes](docs/diagramas/diagrama-classes.png)

## Documentos de apoio

- **[DECISOES_DE_PROJETO.md](DECISOES_DE_PROJETO.md)** — justificativa técnica de cada decisão de concorrência, com as más práticas que foram evitadas e o porquê de cada escolha.
- **[ROTEIRO_APRESENTACAO.md](ROTEIRO_APRESENTACAO.md)** — roteiro da apresentação, com o que falar em cada momento e a divisão entre os integrantes.
- **[COLA_APRESENTACAO.md](COLA_APRESENTACAO.md)** — a sequência exata de comandos das três músicas.
- **[GUIA.md](GUIA.md)** — a especificação original da atividade.
