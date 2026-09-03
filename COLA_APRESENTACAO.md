# COLA DE PALCO — Mesa de DJ Multithread

Uma página. Deixe aberta ao lado do terminal. **Tudo começa em silêncio: nada toca até você mandar.**

---

## ANTES DE COMEÇAR

```
run.bat
```

A mesa sobe com **Billie Jean carregada e muda**: as 3 threads já estão vivas, todas em `PAUSADO`.
Se quiser conferir antes de falar: `list`.

---

## BLOCO 1 — BILLIE JEAN · 117 BPM · Fá# menor

| # | Digite | O que acontece | Frase de apoio |
|---|---|---|---|
| 1 | `play bateria` | Entra a batida sozinha (é a intro real da música) | "Essa thread não estava parada: estava dormindo em `wait()`." |
| 2 | `play baixo` | O riff mais famoso do pop entra **no compasso** | "Ele entrou no lugar certo do compasso, não de onde parou." |
| 3 | `play synth` | Acorde F#m7 fecha o arranjo | "Três threads, um relógio só." |
| 4 | `add guitarra` | Cria uma thread nova em tempo de execução | "`ConcurrentHashMap` + `start()` com a música rodando." |
| 5 | `pause bateria` | Some a bateria, o resto continua | **Ponto alto.** "Pausar uma faixa não afeta as outras." |
| 6 | `play bateria` | Volta em cima do tempo | "`notifyAll()` acorda exatamente essa thread." |
| 7 | `solo baixo` | Só o baixo toca | "As outras foram para `wait()`, custando zero CPU." |
| 8 | `todos` | Tudo volta junto | |

---

## BLOCO 2 — SEVEN NATION ARMY · 124 BPM · Mi menor

```
seven
```
*(carrega o novo arranjo em silêncio — as threads antigas foram encerradas com `join()`)*

| # | Digite | O que acontece | Frase de apoio |
|---|---|---|---|
| 1 | `play baixo` | O riff sozinho — a plateia reconhece na hora | "Mudou a música, não mudou uma linha de sincronização." |
| 2 | `play bateria` | Batida crua, sem prato nenhum (como no disco) | |
| 3 | `play guitarra` | Dobra o riff uma oitava acima, distorcido | |
| 4 | `add palmas` | Palmas nos tempos 2 e 4 | Chame a plateia para bater palma junto. |
| 5 | `eco on` | A tela é inundada por linhas de threads diferentes | "Prova visual do paralelismo — e todas passam pelo mesmo lock de impressão." |
| 6 | `eco off` | Volta o silêncio na tela | |

---

## BLOCO 3 — SWEET DREAMS · 126 BPM · Dó menor

```
sweet
```

> É a faixa que chega mais perto do original, e a razão é técnica: o disco foi feito com
> sequenciador, sintetizador e bateria eletrônica — exatamente o que esta mesa é.

| # | Digite | O que acontece | Frase de apoio |
|---|---|---|---|
| 1 | `play riff` | O riff de sintetizador sozinho | "Aqui o sintetizador do Java não está imitando um instrumento: ele **é** o mesmo tipo de instrumento que gravou o disco." |
| 2 | `play baixo` | Toca a **mesma linha**, uma oitava abaixo | **Ponto alto.** "Duas threads independentes executando o mesmo desenho ao mesmo tempo. Se o relógio não fosse compartilhado, vocês ouviriam um eco entre as duas — e não ouvem." |
| 3 | `play bateria` | Bateria eletrônica seca, sem nenhuma variação | "Comportamento de máquina, não de baterista: é literalmente o mesmo padrão repetindo." |
| 4 | `add pad` | Cordas sintetizadas ao fundo | "Thread nova, criada com a música tocando." |
| 5 | `add solo` | O riff dobrado na oitava aguda | Agora são **três** threads no mesmo desenho, em três oitavas. |
| 6 | `solo riff` | Volta o riff sozinho | "Todas as outras foram para `wait()`." |
| 7 | `bpm all 150` | A música inteira acelera junta | "Uma linha só: `60000 / (bpm × subdivisão)`." |

---

## FECHAMENTO

| Digite | O que falar |
|---|---|
| `silencio` | "Todas as faixas em `wait()`. As threads estão vivas e não consomem CPU — dá para conferir no Gerenciador de Tarefas." |
| `exit` | "`volatile` para parar o laço, `notifyAll()` para acordar quem dormia, `interrupt()` para acordar quem estava no `sleep`, e `join()` em cada thread antes de sair. Zero threads órfãs." |

---

## SE ALGO DER ERRADO

| Situação | Comando |
|---|---|
| Não tem áudio na sala | `eco on` — a apresentação inteira funciona na tela |
| Esqueceu o nome de uma faixa | `list` |
| Esqueceu as músicas | `setlist` |
| O arranjo saiu do lugar | `sync` |
| Quer recomeçar a música do zero | `billie` / `seven` / `sweet` de novo |
| Quer tudo tocando de uma vez | `todos` |

---

## FAIXAS DE CADA MÚSICA

| Música | Comando | Faixas iniciais | Extras (`add`) |
|---|---|---|---|
| Billie Jean | `billie` | bateria, baixo, synth | guitarra, vocal |
| Seven Nation Army | `seven` | bateria, baixo, guitarra | palmas, solo |
| Sweet Dreams | `sweet` | riff, baixo, bateria | pad, solo |

> `add <qualquer nome>` também funciona: a faixa entra improvisando na tonalidade da música atual.
