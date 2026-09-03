# Guia do Projeto — Mesa de DJ com Threads (Java)

Guia de execução da atividade: o que construir, com quais ferramentas, em que ordem.
O objetivo é uma aplicação de console em que cada instrumento toca em sua **própria thread**,
e o DJ controla cada faixa por comandos de texto sem afetar as demais.

**Regra de escopo:** implemente exatamente o que está aqui. Nada de interface gráfica,
áudio real, banco de dados ou framework. O foco da nota é **threads + sincronização**.

---

## 1. Ferramentas

| Item | Escolha | Observação |
|---|---|---|
| Linguagem | Java 17 | Já instalado na máquina (Temurin 17.0.18) |
| Compilação | `javac` / `java` puro | Sem Maven/Gradle — o projeto não tem dependência externa |
| IDE | IntelliJ IDEA Community **ou** VS Code + Extension Pack for Java | Qualquer uma serve |
| Versionamento | Git + GitHub | Repositório da equipe, commits de todos os membros |
| Bibliotecas | **Nenhuma externa** | Só `java.lang.Thread`, `java.util.concurrent`, `java.util.Scanner` |

### Comandos de build e execução

Na raiz do projeto (`Atividade Threads`), no PowerShell:

```powershell
javac -d out (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })
java -cp out br.com.cesar.dj.Main
```

No Git Bash / Linux / Mac:

```bash
javac -d out $(find src -name "*.java")
java -cp out br.com.cesar.dj.Main
```

Dica: coloque essas duas linhas em um `run.bat` para não digitar toda vez.

---

## 2. Estrutura de pastas

```
Atividade Threads/
├── GUIA.md
├── README.md                  (escrito no final: como rodar + print do painel)
├── run.bat
└── src/
    └── br/com/cesar/dj/
        ├── Main.java          ponto de entrada: monta o mixer e chama o console
        ├── Instrumento.java   a thread da faixa (o coração do projeto)
        ├── EstadoFaixa.java   enum: TOCANDO, PAUSADO, PARADO
        ├── Mixer.java         registro thread-safe de todos os instrumentos
        ├── Console.java       lê comandos do teclado e chama o Mixer
        └── PainelStatus.java  thread extra que imprime o status a cada 2s (extra)
```

---

## 3. Conceitos e qual mecanismo usar em cada ponto

Esta é a parte que o professor vai cobrar. Cada decisão abaixo tem uma justificativa —
saiba explicá-la na apresentação.

### 3.1. Uma thread por instrumento
Cada `Instrumento` implementa `Runnable` e roda em uma `Thread` própria, com um loop
que imprime a batida e dorme o intervalo do BPM.

### 3.2. Pausar/retomar → `synchronized` + `wait()` / `notifyAll()`
**Não use** `while (pausado) { }` (busy-wait: queima 100% de CPU) nem `Thread.suspend()`
(deprecado e inseguro). O correto:

- A thread do instrumento, no início de cada volta do loop, entra em um bloco
  `synchronized` sobre um objeto de lock privado e chama `lock.wait()` **enquanto** o
  estado for `PAUSADO`.
- O comando `play` altera o estado dentro do mesmo `synchronized` e chama
  `lock.notifyAll()` para acordar a thread.

Isso garante que a faixa pausada fica **dormindo de verdade**, sem consumir CPU, e
que o estado só é lido/escrito por uma thread por vez.

> **Regra de ouro do `wait`:** sempre dentro de um `while`, nunca de um `if`
> (proteção contra *spurious wakeups*).

### 3.3. Encerrar uma faixa → flag `volatile` + `interrupt()`
Um `volatile boolean ativo = true;` controla o loop principal. O comando `stop`
seta `ativo = false`, chama `notifyAll()` (para acordar a thread se estiver pausada)
e `thread.interrupt()` (para acordar o `Thread.sleep`). No `catch (InterruptedException e)`
restaure o flag com `Thread.currentThread().interrupt()` e saia do loop.

Nunca use `Thread.stop()`.

### 3.4. BPM ajustável → campo protegido pelo lock
O intervalo do `sleep` vem de `60_000 / bpm`. Como o comando `bpm bateria 140` é
executado pela thread do console enquanto a thread do instrumento lê o valor,
o acesso precisa ser sincronizado (ou o campo ser `volatile`).

### 3.5. Lista de instrumentos → `ConcurrentHashMap`
O comando `add guitarra` insere no mapa enquanto o `PainelStatus` itera sobre ele.
Um `HashMap` comum estouraria `ConcurrentModificationException`.
Use `ConcurrentHashMap<String, Instrumento>` no `Mixer` e suba a thread nova com
`new Thread(instrumento).start()`.

### 3.6. Impressão no console
Várias threads escrevendo em `System.out` ao mesmo tempo embaralham a saída.
Centralize a impressão em um método `synchronized` (ex.: `Console.log(String)`)
ou deixe só o `PainelStatus` imprimir. Explique essa escolha na apresentação —
é o mesmo problema de seção crítica, aplicado à tela.

---

## 4. Roteiro de implementação (faça nesta ordem)

Cada etapa deve compilar e rodar antes de passar para a próxima.

**Etapa 1 — Uma faixa tocando.**
`EstadoFaixa` (enum) + `Instrumento implements Runnable` com loop imprimindo
`[Bateria] tum` a cada 500ms. `Main` cria uma thread e dá `start()`.
Meta: ver o som saindo no console.

**Etapa 2 — Várias faixas simultâneas.**
`Main` sobe 3 instrumentos (Bateria, Baixo, Synth) com intervalos diferentes.
Meta: ver as três saídas intercaladas — prova visual de paralelismo.

**Etapa 3 — Console de comandos.**
`Console` com `Scanner(System.in)` em loop lendo linhas. Implemente `list` e `exit`.
Atenção: o `Scanner` roda na thread principal enquanto as faixas tocam nas outras.

**Etapa 4 — Pausar e retomar (núcleo da atividade).**
Implemente `pause <nome>` e `play <nome>` com `synchronized`/`wait`/`notifyAll`
conforme a seção 3.2. Meta: pausar a bateria e ver baixo e synth seguirem tocando.

**Etapa 5 — Encerramento seguro.**
`stop <nome>` encerra uma faixa; `exit` encerra todas e faz `join()` em cada thread
antes de o programa terminar. Meta: nenhuma thread órfã, saída limpa.

**Etapa 6 — Extras** (seção 6, se der tempo).

---

## 5. Comandos da mesa

| Comando | Efeito |
|---|---|
| `play <nome>` | Retoma a faixa (`notifyAll` na thread pausada) |
| `pause <nome>` | Pausa a faixa sem encerrar a thread |
| `stop <nome>` | Encerra a thread daquela faixa |
| `list` | Lista faixas, estado e BPM |
| `bpm <nome> <valor>` | Altera a velocidade da faixa (extra) |
| `add <nome>` | Cria e inicia uma nova faixa em tempo de execução (extra) |
| `exit` | Encerra todas as threads e sai |

Valide entrada inválida (nome inexistente, BPM não numérico) com mensagem de erro —
o programa não pode quebrar com uma exceção não tratada vinda do console.

---

## 6. Desafios extras

1. **BPM real:** `sleep(60_000 / bpm)`. 60 BPM = 1 batida por segundo. Deixe a bateria
   em 120 e o baixo em 60 para ouvir a diferença de densidade.
2. **Painel ao vivo:** `PainelStatus` como thread daemon (`setDaemon(true)`) que a cada
   2s imprime a tabela de todas as faixas. Para "limpar" o console use
   `System.out.print("\033[H\033[2J")` (funciona no Windows Terminal) ou simplesmente
   imprima 30 linhas em branco. Se ativar o painel, silencie as batidas individuais
   para a tela não brigar consigo mesma.
3. **`add <nome>` em tempo de execução:** cria o `Instrumento`, insere no
   `ConcurrentHashMap` e dá `start()` na hora — sem parar a música.

---

## 7. Armadilhas comuns (vão aparecer na correção)

- `while (pausado) {}` — busy-wait, queima CPU. Use `wait()`.
- `Thread.sleep()` **dentro** do bloco `synchronized` — a faixa segura o lock dormindo e
  trava o comando do DJ. Durma **fora** da região crítica.
- `if (pausado) wait();` — tem que ser `while`.
- `notify()` em vez de `notifyAll()` — com mais de uma thread esperando, pode não acordar
  a certa.
- `HashMap` compartilhado entre console e painel — `ConcurrentModificationException`.
- Esquecer o `join()` no `exit` — o programa "termina" com threads ainda rodando.
- Campo de estado sem `volatile` nem `synchronized` — uma thread pode nunca enxergar a
  mudança feita por outra (problema de visibilidade de memória).

---

## 8. Divisão sugerida na equipe

| Frente | Entrega |
|---|---|
| A | `Instrumento` + `EstadoFaixa` (loop, wait/notify, encerramento) |
| B | `Mixer` + `Console` (parsing de comandos, validação, `exit` com `join`) |
| C | `PainelStatus` + BPM + `add` (extras) |
| Todos | `README.md`, testes manuais, roteiro da apresentação |

Todos devem commitar no Git — o histórico é evidência de participação.

---

## 9. Checklist de entrega

- [ ] Compila com `javac` sem warnings de deprecação
- [ ] 3+ instrumentos tocando simultaneamente, saídas intercaladas
- [ ] `pause` em uma faixa **não** afeta as outras
- [ ] `play` retoma exatamente a faixa pausada
- [ ] Faixa pausada não consome CPU (verificável no Gerenciador de Tarefas)
- [ ] `exit` encerra tudo sem exceção e sem thread pendurada
- [ ] Nenhum `Thread.stop()` / `Thread.suspend()` no código
- [ ] `README.md` com instruções de execução
- [ ] Repositório Git com commits de todos os membros

---

## 10. Roteiro da apresentação (5 min)

1. Rodar a mesa com 3 faixas e mostrar as saídas intercaladas.
2. Pausar uma faixa ao vivo e mostrar que as outras seguem — este é o ponto alto.
3. Abrir o `Instrumento.java` e explicar o `synchronized`/`wait`/`notifyAll`:
   qual é a seção crítica e por que ela existe.
4. Explicar o encerramento seguro (`volatile` + `interrupt`, sem `Thread.stop()`).
5. Demonstrar um extra (`add` ou `bpm`) e encerrar com `exit`.
