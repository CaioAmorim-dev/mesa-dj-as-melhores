# Handover — reconstrução da Mesa de DJ Multithread em Java

> Documento de contexto para uma nova sessão com o Codex. Leia este arquivo inteiro antes de orientar ou alterar o projeto.

## 1. Contexto e intenção do aluno

Este é um projeto acadêmico da disciplina de **Infraestrutura de Software**. Já existe uma versão pronta da atividade, mas o aluno quer **refazê-la do zero para aprender**, e não apenas reproduzir a solução existente.

O objetivo principal é compreender profundamente:

- processos e threads;
- `Thread`, `Runnable`, `start()` e `run()`;
- escalonamento e concorrência;
- `sleep()`;
- estado mutável compartilhado;
- condição de corrida, atomicidade e visibilidade;
- `synchronized` e monitores;
- `wait()` e `notifyAll()`;
- interrupção cooperativa com `interrupt()`;
- encerramento controlado com flags e `join()`;
- coleções concorrentes, apenas quando o comando de adição dinâmica for introduzido.

A mesa de DJ é o domínio usado para estudar esses conceitos. Cada instrumento/faixa deve possuir um fluxo de execução independente, enquanto a thread principal recebe comandos do usuário.

## 2. Regra de mentoria — obrigatória

O Codex deve atuar como **mentor**, não como autor da atividade.

Durante a reconstrução:

1. Não entregar a aplicação inteira nem antecipar todas as classes prontas.
2. Apresentar um conceito de cada vez e propor experimentos pequenos.
3. Pedir ao aluno que faça previsões antes de executar um experimento.
4. Incentivar o aluno a escrever o código.
5. Revisar o código escrito pelo aluno e explicar erros, riscos e alternativas.
6. Dar pistas em camadas: primeiro uma pergunta, depois uma direção conceitual e somente então um trecho mínimo, se necessário.
7. Antes de avançar, pedir que o aluno explique com as próprias palavras o conceito recém-usado.
8. Usar o livro **Use a Cabeça! Java / Head First Java, 2nd Edition** como referência principal. Consultar internet somente quando o livro não cobrir o assunto ou quando for necessário confirmar documentação atual.
9. Não copiar a implementação que já existe neste repositório. Os arquivos atuais servem apenas como referência histórica de escopo e decisões.
10. Não apagar nem sobrescrever o projeto pronto. Se a reconstrução acontecer neste mesmo repositório, trabalhar em uma nova pasta ou branch definida com o aluno.

Se o aluno pedir explicitamente uma implementação completa em algum momento, confirmar se ele está encerrando o modo de mentoria antes de fornecê-la.

## 3. Identidade do projeto

**Nome do repositório:** `mesa-dj-as-melhores`

**Nome de apresentação:** Mesa de DJ Multithread

**Descrição breve do repositório:**

> Projeto acadêmico em Java para estudo de concorrência e sincronização, simulando uma mesa de DJ em que cada faixa musical é executada e controlada por uma thread independente.

**Linguagem:** Java 17 ou superior.

**Build inicial:** `javac` e `java`, sem Maven, Gradle, frameworks ou dependências externas. Ferramentas adicionais só devem ser introduzidas se houver uma necessidade concreta e depois que os fundamentos estiverem compreendidos.

## 4. Enunciado consolidado

Construir uma aplicação que simule uma mesa de DJ. Diferentes instrumentos ou faixas musicais devem tocar simultaneamente e de forma independente. O usuário, no papel de DJ, controla as faixas por comandos de texto, podendo pausar e retomar uma faixa sem afetar as demais.

O estado de cada faixa é compartilhado entre a thread do console e a thread do instrumento. Esse estado deve ser coordenado de maneira segura, usando mecanismos de sincronização do Java. As threads devem ser encerradas de forma cooperativa, sem finalizar abruptamente o processo.

Desafios extras sugeridos no enunciado:

- representar volume ou BPM por meio do intervalo de execução;
- criar uma thread que exiba o status das faixas periodicamente;
- adicionar instrumentos em tempo de execução;
- usar STEMs ou loops de áudio reais em uma evolução posterior.

## 5. Escopo e prioridades

### Núcleo obrigatório

- aplicação de console;
- no mínimo três instrumentos/faixas;
- uma thread independente por instrumento;
- thread principal dedicada à leitura dos comandos;
- pausa e retomada individual sem interferência entre faixas;
- sincronização explícita do estado compartilhado;
- ausência de espera ocupada;
- encerramento seguro de uma faixa e de toda a aplicação;
- validação de comandos inválidos;
- documentação suficiente para executar e explicar o projeto.

### Extras, somente depois do núcleo

- BPM ajustável;
- painel de status executado em thread própria;
- `add <nome>` durante a execução;
- repertório com músicas diferentes;
- relógio mestre para reduzir o desvio temporal entre faixas;
- saída de áudio MIDI ou reprodução de STEMs.

### Fora do escopo inicial

- interface gráfica;
- banco de dados;
- aplicação web;
- frameworks;
- `ExecutorService`, `ReentrantLock`, `Condition` e abstrações concorrentes avançadas antes de dominar `Thread`, `synchronized`, `wait()` e `notifyAll()`.

## 6. Decisões arquiteturais a preservar

Estas decisões formam o caminho didático planejado. Elas devem ser compreendidas antes de serem aplicadas.

### `Runnable` separado de `Thread`

O instrumento representa o **trabalho** e implementa `Runnable`; uma `Thread` executa esse trabalho. A separação ajuda a distinguir a tarefa do mecanismo que a executa e evita modelar “instrumento é uma thread”.

### Estado explícito da faixa

Modelar pelo menos os estados:

- `TOCANDO`;
- `PAUSADO`;
- `PARADO`.

O estado pertence ao instrumento. O console solicita transições; ele não deve congelar uma thread externamente.

### Pausa cooperativa

Usar um monitor privado com `synchronized`, `wait()` e `notifyAll()`.

- `wait()` deve ser chamado dentro de um `while` que revalida a condição;
- `wait()` libera o monitor enquanto a thread espera;
- `sleep()` não libera monitores e deve ficar fora da região sincronizada;
- não usar polling nem `while (pausado) {}`;
- não usar `Thread.suspend()` ou `Thread.resume()`.

### Encerramento cooperativo

Usar uma flag de execução com garantia de visibilidade, acordar a thread caso ela esteja em `wait()`, interrompê-la caso esteja em `sleep()` e aguardar seu término com `join()`.

Não usar `Thread.stop()`.

O tratamento de `InterruptedException` deve ser discutido, e não aplicado mecanicamente. O aluno precisa entender quando restaurar o status de interrupção e quando encerrar o laço.

### Coleção de instrumentos

Começar com a estrutura mais simples que atenda à etapa atual. Um `Map<String, Instrumento>` é útil para localizar uma faixa pelo nome. Quando `add` e o painel periódico passarem a acessar a coleção simultaneamente, analisar a necessidade de sincronização ou de `ConcurrentHashMap`.

Não introduzir uma coleção concorrente antes de existir concorrência real sobre a coleção.

### Saída do console

Quando várias threads imprimirem, discutir a intercalação da saída como outro exemplo de recurso compartilhado. Depois, centralizar a impressão ou protegê-la com um monitor comum.

### Áudio em duas fases

1. Primeiro, representar o som por mensagens como `[Bateria] tum` e intervalos temporais. O foco é observar as threads.
2. Somente com a concorrência estável, avaliar áudio real. O caminho sem dependências externas é a API MIDI do JDK. STEMs em WAV são uma alternativa posterior, mas acrescentam sincronização de reprodução, buffers, latência, arquivos grandes e licenciamento.

## 7. Arquitetura evolutiva

Não criar todas estas classes no primeiro passo. Elas representam um possível destino da evolução.

### Estrutura mínima

```text
src/
└── br/com/cesar/dj/
    ├── Main.java
    ├── Instrumento.java
    └── EstadoFaixa.java
```

### Estrutura do núcleo completo

```text
src/
└── br/com/cesar/dj/
    ├── Main.java
    ├── Instrumento.java
    ├── EstadoFaixa.java
    ├── Mixer.java
    └── Console.java
```

### Extensões possíveis

```text
src/
└── br/com/cesar/dj/
    ├── PainelStatus.java
    ├── RelogioMestre.java
    ├── Musica.java
    ├── Padrao.java
    └── GerenciadorAudio.java
```

Responsabilidades pretendidas:

| Componente | Responsabilidade |
|---|---|
| `Main` | Compor os objetos e iniciar a aplicação |
| `Instrumento` | Manter estado e executar o laço da faixa |
| `EstadoFaixa` | Representar estados válidos sem strings soltas |
| `Mixer` | Registrar e coordenar instrumentos sem expor detalhes das threads ao console |
| `Console` | Ler, interpretar e validar comandos |
| `PainelStatus` | Ler estados e mostrar o painel periodicamente |
| `RelogioMestre` | Oferecer uma referência temporal comum, se o áudio exigir sincronismo mais preciso |
| `Musica` / `Padrao` | Manter dados musicais separados da lógica concorrente |
| `GerenciadorAudio` | Encapsular o recurso de áudio compartilhado |

## 8. Roteiro de aprendizagem e implementação

Cada etapa deve ser pequena, compilável e verificável. Não avançar várias etapas de uma vez.

### Etapa 0 — Preparação

- decidir com o aluno onde ficará a reconstrução sem destruir a versão pronta;
- conferir `java -version` e `javac -version`;
- preparar a estrutura mínima do projeto;
- registrar o objetivo da etapa atual;
- combinar commits pequenos, se Git fizer parte da entrega.

**Critério de saída:** o aluno sabe compilar e executar uma classe `Main` pelo terminal.

### Etapa 1 — Processo, thread, `run()` e `start()`

- criar um experimento mínimo com uma única tarefa;
- observar o nome da thread atual;
- comparar chamar `run()` diretamente com chamar `start()`;
- discutir pilhas de execução e o papel do escalonador.

**Pergunta de validação:** por que `run()` não cria um fluxo de execução concorrente?

### Etapa 2 — `Runnable` e duas threads

- transformar o trabalho em uma implementação de `Runnable`;
- executar duas tarefas independentes;
- observar que a ordem de saída não é garantida;
- evitar conclusões baseadas em uma única execução.

**Critério de saída:** o aluno consegue explicar a diferença entre a tarefa e a thread.

### Etapa 3 — `sleep()` e ritmo

- adicionar intervalos diferentes às tarefas;
- observar que uma thread dormindo não paralisa as outras;
- discutir que `sleep()` representa espera temporal, não comunicação;
- tratar `InterruptedException` conscientemente.

**Critério de saída:** três instrumentos imprimem em ritmos independentes.

### Etapa 4 — Modelar o instrumento e seu estado

- introduzir `Instrumento` e `EstadoFaixa`;
- identificar exatamente quais dados são lidos e escritos por quais threads;
- desenhar o estado mutável compartilhado antes de sincronizá-lo;
- discutir visibilidade, atomicidade e condição de corrida.

**Pergunta de validação:** qual thread muda o estado e qual thread observa a mudança?

### Etapa 5 — Console de comandos

- manter a leitura com `Scanner` na thread principal;
- implementar primeiro `list` e `exit`;
- depois acrescentar `pause <nome>` e `play <nome>`;
- validar comandos vazios, nomes inexistentes e argumentos incorretos.

**Critério de saída:** o console continua responsivo enquanto os instrumentos executam.

### Etapa 6 — Pausa e retomada corretas

- demonstrar por que busy-wait é inadequado;
- criar um lock privado;
- usar `synchronized` para testar/alterar a condição;
- usar `while` + `wait()` ao pausar;
- usar `notifyAll()` ao retomar;
- manter `sleep()` e qualquer saída lenta fora do lock.

**Critério de saída:** pausar uma faixa não para as outras e a faixa pausada não fica consumindo CPU em polling.

### Etapa 7 — Encerramento seguro

- implementar `stop <nome>` como solicitação cooperativa;
- acordar uma thread em `wait()`;
- interromper uma thread em `sleep()`;
- implementar `exit` solicitando o término de todas;
- usar `join()` para a thread principal esperar o encerramento.

**Critério de saída:** a JVM termina de forma limpa, sem threads de instrumento penduradas.

### Etapa 8 — BPM

- derivar com o aluno a fórmula `intervaloMs = 60_000 / bpm`;
- validar limites e divisão por zero;
- discutir a visibilidade do novo BPM entre console e instrumento;
- comparar um campo sob o mesmo monitor com um campo `volatile`.

**Critério de saída:** mudar o BPM de uma faixa afeta seu próximo ciclo sem reiniciar a thread.

### Etapa 9 — Painel de status

- criar uma tarefa que lê os estados a cada dois segundos;
- decidir se ela será daemon e explicar a consequência;
- proteger a saída compartilhada;
- analisar a coleção compartilhada antes de implementar `add`.

**Critério de saída:** o painel não corrompe a saída nem impede o encerramento.

### Etapa 10 — Adição dinâmica

- implementar `add <nome>`;
- iniciar a nova thread sem interromper as existentes;
- impedir nomes duplicados;
- reavaliar o tipo de mapa e as iterações concorrentes.

**Critério de saída:** adicionar, listar, pausar e encerrar a nova faixa funciona durante a execução.

### Etapa 11 — Música e áudio real

- escolher primeiro entre MIDI do JDK, loops WAV ou STEMs;
- manter dados musicais separados da sincronização;
- usar uma origem temporal comum caso seja necessário evitar drift;
- verificar licença, atribuição e tamanho dos arquivos;
- não versionar datasets grandes diretamente no Git.

**Critério de saída:** o áudio acrescenta valor à demonstração sem esconder nem quebrar os conceitos de concorrência.

### Etapa 12 — Revisão e apresentação

- compilar com avisos habilitados;
- executar testes manuais repetidos;
- documentar como executar;
- relacionar cada requisito a uma decisão no código;
- preparar uma demonstração curta de concorrência, pausa isolada e encerramento.

## 9. Comandos planejados

Introduzir somente os comandos necessários à etapa em andamento.

| Comando | Fase | Efeito |
|---|---:|---|
| `list` | núcleo | Lista faixa, estado e ritmo |
| `play <nome>` | núcleo | Retoma uma faixa pausada |
| `pause <nome>` | núcleo | Pausa uma faixa sem encerrar sua thread |
| `stop <nome>` | núcleo | Encerra definitivamente uma faixa |
| `exit` | núcleo | Encerra todas as threads e a aplicação |
| `bpm <nome> <valor>` | extra | Altera o ritmo de uma faixa |
| `add <nome>` | extra | Cria uma faixa em tempo de execução |
| `status` | extra | Mostra um retrato das faixas |
| `solo <nome>` | extensão | Pausa as demais e mantém apenas uma tocando |
| `todos` | extensão | Retoma todas as faixas |
| `silencio` | extensão | Pausa todas sem encerrá-las |
| `sync` | extensão | Realinha as faixas em uma origem temporal comum |

## 10. Trilha de leitura — Use a Cabeça! Java

Usar os títulos no índice porque páginas e numeração podem variar entre a edição física em português e o PDF em inglês.

### Revisão seletiva: capítulos 1 a 5

Revisar somente se houver insegurança em:

- classes, objetos e métodos;
- referências;
- encapsulamento;
- herança, interfaces e polimorfismo.

O foco é ter base suficiente para compreender por que `Instrumento` deve encapsular seu próprio estado e por que `Runnable` é uma interface.

### Capítulo 6 — Using the Java Library / Usando a biblioteca Java

Priorizar:

- uso da documentação da API;
- `ArrayList` e coleções;
- escolha de estruturas de dados.

Depois complementar com `Map` e `ConcurrentHashMap` na documentação do JDK quando essas estruturas realmente entrarem no projeto.

### Capítulo 11 — Risky Behavior / Comportamento de risco

Priorizar:

- exceções verificadas;
- `try`/`catch`/`finally`;
- propagação de exceções;
- exemplos de música/Java Sound existentes no capítulo.

Essa leitura prepara o tratamento de `InterruptedException` e, futuramente, de recursos de áudio.

### Capítulo 15 — Make a Connection / Faça uma conexão

É a leitura principal. Procurar, nesta ordem, as seções ou títulos equivalentes a:

1. múltiplas threads e a classe `Thread`;
2. cada thread precisa de um trabalho;
3. criação de um trabalho com `Runnable`;
4. o escalonador de threads;
5. colocar uma thread para dormir;
6. criar e iniciar duas threads;
7. problemas de concorrência;
8. operações atômicas e região crítica;
9. uso do lock de um objeto;
10. sincronização com `synchronized`.

O livro é a base, mas pode não aprofundar todo o protocolo moderno de cancelamento. Para `wait()`, `notifyAll()`, interrupção e `join()`, usar também a documentação oficial do JDK quando chegar a essas etapas.

### Leitura posterior

Depois de concluir a versão fundamental, estudar:

- `ExecutorService`;
- `Future`;
- coleções concorrentes;
- `AtomicBoolean`;
- `ReentrantLock` e `Condition`.

Esses recursos servem para comparar uma solução profissional moderna com os mecanismos fundamentais exigidos pela disciplina, não para substituir o aprendizado inicial.

## 11. Fontes de STEMs e loops fornecidas pelo professor

Não é necessário baixar nada na fase inicial.

### STEMs e multitracks

- Cambridge Music Technology: <https://cambridge-mt.com/ms3/mtk/>
- Telefunken Multitracks: <https://www.telefunken-elektroakustik.com/multitracks>
- Internet Archive — Stems: <https://archive.org/details/stems>
- Wikiloops: <https://www.wikiloops.com/>
- ccMixter: <https://ccmixter.org/>
- MUSDB: <https://sigsep.github.io/datasets/musdb.html>
- Slakh: <http://www.slakh.com/>

### Loops e amostras

- MusicRadar SampleRadar: <https://www.musicradar.com/news/tech/free-music-samples-royalty-free-loops-hits-and-multis-to-download-sampleradar>
- Looperman: <https://www.looperman.com/loops>
- Sample Focus: <https://samplefocus.com/>
- Freesound: <https://freesound.org/>
- Pixabay: <https://pixabay.com/sound-effects/search/loop/>
- SampleSwap: <https://sampleswap.org/>

Antes de escolher material, verificar:

- licença de uso e necessidade de atribuição;
- se as faixas pertencem à mesma música e começam no mesmo ponto;
- formato compatível com a API escolhida;
- duração e tamanho total;
- possibilidade de disponibilizar apenas instruções de download em vez dos arquivos.

## 12. Armadilhas que devem virar perguntas de revisão

- Qual é a diferença entre `thread.run()` e `thread.start()`?
- Por que a ordem das mensagens de duas threads muda entre execuções?
- Por que `sleep()` não é uma solução de comunicação entre threads?
- Por que `while (pausado) {}` desperdiça CPU?
- Por que `wait()` precisa ocorrer com o monitor adquirido?
- Por que testar a condição com `while`, e não com `if`?
- Qual é a diferença entre `wait()` e `sleep()` quanto ao monitor?
- Como uma alteração feita pela thread do console se torna visível à thread da faixa?
- Que invariantes a região crítica protege?
- Como acordar uma faixa pausada para que ela possa encerrar?
- Como interromper rapidamente uma faixa que está dormindo?
- Por que `join()` pertence ao fluxo de encerramento?
- O que acontece se o painel iterar no mapa enquanto o console adiciona uma faixa?
- O recurso de áudio é seguro para acesso concorrente? Onde deve ser encapsulado?

## 13. Checklist de aceitação final

- [ ] O aluno consegue explicar processo versus thread.
- [ ] O aluno explica `start()` versus `run()` sem consultar o código.
- [ ] Cada instrumento executa em sua própria thread.
- [ ] Há pelo menos três faixas simultâneas.
- [ ] Pausar uma faixa não afeta as demais.
- [ ] A pausa não usa busy-wait.
- [ ] `wait()` está em um laço que revalida a condição.
- [ ] Nenhum `sleep()` ocorre segurando o lock de controle da faixa.
- [ ] Não são usados `Thread.stop()`, `suspend()` ou `resume()`.
- [ ] Estado compartilhado tem uma estratégia de visibilidade e exclusão justificável.
- [ ] Entradas inválidas não derrubam o programa.
- [ ] `exit` acorda/interrompe e faz `join()` nas threads necessárias.
- [ ] A aplicação compila sem warnings relevantes.
- [ ] O README explica objetivo, requisitos, build, execução e comandos.
- [ ] Todos os integrantes possuem commits, se isso for exigido na entrega.
- [ ] O aluno consegue apontar no código onde cada requisito foi atendido.

## 14. Primeiro encontro da nova sessão

O Codex deve começar assim:

1. Confirmar que entendeu que a meta é aprendizagem e reconstrução, não cópia.
2. Inspecionar o ambiente de maneira não destrutiva e definir com o aluno o local da nova implementação.
3. Pedir que o aluno explique sua compreensão atual de processo e thread.
4. Orientar a leitura inicial do trecho de threads do Capítulo 15.
5. Propor somente o primeiro experimento: observar a diferença entre `run()` e `start()` e o nome da thread executora.
6. Pedir uma previsão antes da execução e uma explicação depois.
7. Registrar o que foi aprendido e deixar a próxima etapa explícita.

Não iniciar criando a mesa inteira. A implementação deve crescer a partir dos experimentos e das explicações do aluno.

## 15. Referência histórica da versão pronta

A versão atual deste repositório chegou a incluir:

- uma thread por faixa;
- `synchronized`, `wait()` e `notifyAll()`;
- flags com visibilidade entre threads;
- `interrupt()` e `join()`;
- `ConcurrentHashMap`;
- painel auxiliar;
- BPM;
- adição dinâmica;
- repertório;
- saída MIDI;
- relógio mestre para reduzir drift.

Esses itens confirmam que o caminho é viável, mas **não são o ponto de partida da reconstrução**. O novo desenvolvimento deve conquistar cada item gradualmente e justificar cada decisão.

