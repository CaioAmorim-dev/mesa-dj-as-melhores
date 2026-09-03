# Roteiro da Apresentação — Mesa de DJ Multithread

Duração alvo: **5 a 7 minutos**. A ideia é que a música seja o *instrumento de prova* dos conceitos
de concorrência: o público **ouve** o que normalmente só se vê em log.

> Para a sequência exata de comandos, use a **[COLA_APRESENTACAO.md](COLA_APRESENTACAO.md)** ao lado
> do terminal. Este documento é o roteiro do que falar em cada momento.

---

## Preparação (antes de falar)

```
run.bat
```

A mesa sobe com **Billie Jean carregada e em silêncio absoluto**: as três threads já estão vivas,
todas em `PAUSADO`. Nada toca até você mandar.

Deixe o terminal em fonte grande. Confira o som com um `play bateria` seguido de `pause bateria`
antes de a turma entrar.

**Abertura:**
> "Cada faixa desta mesa é uma thread independente. Nenhuma nota está tocando agora, mas as três
> threads já existem e estão vivas — dormindo em `wait()`. Eu vou montar a música ao vivo."

---

## Ato 1 — Billie Jean: montar a música do zero (~90 s)

Comandos: `play bateria` → `play baixo` → `play synth` → `add guitarra`

O que cada passo prova:

| Passo | Conceito demonstrado |
|---|---|
| `play bateria` | `notifyAll()` acorda uma thread específica que estava em `wait()` |
| `play baixo` | A faixa entra **no compasso certo**, não de onde parou (relógio compartilhado) |
| `play synth` | Três threads independentes soando como um arranjo só |
| `add guitarra` | Thread criada e iniciada em tempo de execução, sem parar a música |

> "Nenhuma dessas threads manda na outra. Elas só concordam sobre o mesmo relógio."

---

## Ato 2 — O ponto alto: pausar sem afetar as outras (~40 s)

```
pause bateria
```

A música continua com baixo, synth e guitarra. Depois:

```
play bateria
```

> "Se eu tivesse usado `while (pausado) {}`, essa faixa estaria queimando um núcleo inteiro agora.
> Com `wait()`, ela está em estado WAITING: custo zero de CPU, e acorda em microssegundos.
> Dá para abrir o Gerenciador de Tarefas e conferir."

Se quiser um segundo efeito: `solo baixo` deixa só o baixo tocando (todas as outras vão para
`wait()`) e `todos` traz todo mundo de volta.

---

## Ato 3 — O código (~90 s) — abra `Instrumento.java`

Mostre exatamente três trechos:

1. **A seção crítica** — `synchronized (lock)` com `while (estado == PAUSADO)`.
   `while` e não `if`, por causa de *spurious wakeups*.
2. **O `Thread.sleep` FORA do `synchronized`** — se dormisse dentro, a faixa seguraria o monitor
   enquanto dorme e travaria os comandos do DJ.
3. **`volatile boolean ativo` + `interrupt()`** — encerramento sem `Thread.stop()`.

Se sobrar fôlego, abra `RelogioMestre.java`:

> "Cada thread poderia simplesmente dormir 256 ms por volta. Só que o tempo gasto tocando a nota
> se acumula, e em 30 segundos o baixo já teria saído do tempo da bateria. Então nenhuma thread
> conta o tempo sozinha: todas calculam o **instante absoluto** do próximo passo a partir de uma
> origem comum."

---

## Ato 4 — Troca de música: a arquitetura aguenta (~90 s)

```
seven
```

> "Trocar de música é derrubar as threads atuais com `join()` e subir as threads do novo arranjo.
> Nenhuma linha de código de sincronização mudou: a música mora em `Musica.java`, a concorrência
> mora em `Instrumento.java`."

Monte: `play baixo` → `play bateria` → `play guitarra` → `add palmas` (chame a plateia para bater
palma nos tempos 2 e 4).

Prova visual do paralelismo:

```
eco on
```

> "Cada linha dessas é uma thread diferente escrevendo na tela — e todas passam pelo mesmo lock de
> impressão. Sem ele, as linhas sairiam picotadas umas dentro das outras: a tela também é uma
> seção crítica."

```
eco off
```

---

## Ato 5 — Duas threads, o mesmo desenho (~60 s)

```
sweet
```

Monte na ordem: `play riff` → `play baixo` → `play bateria`.

O momento que amarra tudo é o segundo comando:

> "O baixo está tocando **exatamente a mesma linha** do riff, uma oitava abaixo. São duas threads
> independentes, escalonadas separadamente pelo sistema operacional, executando o mesmo desenho
> rítmico. Se cada uma contasse o próprio tempo, vocês ouviriam um eco entre as duas — um
> *flanger* acidental que ia piorando. Não ouvem porque as duas leem a mesma origem de tempo."

Depois:

```
add pad
add solo
bpm all 150
```

> "Com o solo são **três** threads no mesmo desenho, em três oitavas. E a música inteira acelera
> junta porque o intervalo de cada uma é `60000 / (bpm × subdivisão)`."

### O fecho conceitual da apresentação

Guarde esta observação para o final — ela explica por que esta faixa soa mais convincente que as
outras duas:

> "Repararam que a mesa acerta mais umas músicas do que outras? Não é sorte. Nas outras, o
> sintetizador do Java está **imitando** uma bateria e um baixo acústicos. Nesta, não: Sweet
> Dreams foi gravada em 1983 com sequenciador, sintetizador e bateria eletrônica — a mesma
> tecnologia que este programa. Um relógio, uma grade de passos e um sintetizador recebendo
> eventos. Quanto mais a música original for feita de eventos e menos de gravação, mais perto a
> nossa versão chega."

---

## Fechamento (~30 s)

```
silencio
exit
```

> "O `silencio` põe todas as threads em `wait()` — vivas, sem consumir CPU. O `exit` seta a flag
> `volatile`, chama `notifyAll()` para acordar quem estiver pausado, `interrupt()` para acordar
> quem estiver dormindo, e faz `join()` em cada thread antes de sair. Nenhuma thread órfã, e o
> sintetizador MIDI é fechado por último."

---

## Divisão sugerida entre os integrantes

| Quem | Fala sobre |
|---|---|
| Integrante A | Atos 1 e 2 (montar a música, `wait`/`notifyAll`, pausa não bloqueante) |
| Integrante B | Ato 3 (o código: seção crítica, `sleep` fora do lock, `volatile`/`interrupt`) |
| Integrante C | Atos 4 e 5 (troca de arranjo, `ConcurrentHashMap`, relógio mestre, BPM) |
| Todos | Fechamento com `exit` e as perguntas do professor |

---

## Perguntas prováveis do professor (e a resposta curta)

| Pergunta | Resposta |
|---|---|
| "Por que não `Thread.sleep` para pausar?" | `sleep` é cego: não sabe quando o `play` chegou. `wait`/`notifyAll` acorda no evento, com latência quase zero. |
| "Por que `while` no `wait` e não `if`?" | *Spurious wakeups*: a thread pode acordar sem ter sido notificada e precisa reconferir a condição. |
| "Por que `notifyAll` e não `notify`?" | Com mais de uma thread esperando no mesmo monitor, `notify` pode acordar a errada. |
| "Por que `volatile`?" | Visibilidade entre núcleos: sem ele, a thread do instrumento pode nunca enxergar o `ativo = false` escrito pela thread do console. |
| "Por que `ConcurrentHashMap`?" | O comando `add` insere no mapa enquanto ele está sendo iterado para listar/parar as faixas. |
| "Onde está o `join()`?" | No `Mixer.pararTodas()`, chamado tanto no `exit` quanto na troca de música. |
