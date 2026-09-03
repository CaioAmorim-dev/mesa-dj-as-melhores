# Decisões de Projeto e Fundamentos de Concorrência — Mesa de DJ

Este documento detalha **todas as decisões arquiteturais e escolhas de mecanismos de sincronização** adotadas no desenvolvimento da Mesa de DJ Multithread em Java, explicando os motivos teóricos e práticos de cada escolha, bem como as armadilhas clássicas de concorrência que foram evitadas.

---

## 1. Visão Geral dos Desafios de Concorrência

Em um sistema multithread em tempo real como uma mesa de som, múltiplas threads operam simultaneamente:
1. A **Thread Principal (Console/DJ)**, que lê comandos do teclado (`play`, `pause`, `stop`, `bpm`, `add`, `exit`).
2. Múltiplas **Threads de Instrumentos**, que executam seus loops temporizados emitindo batidas sonoras.
3. Threads auxiliares de monitoramento.

Os principais desafios são:
- Evitar desperdício de ciclos de processador (**CPU waste / Busy Waiting**);
- Garantir visibilidade imediata de alterações de estado entre diferentes núcleos (**Memory Visibility**);
- Evitar condições de corrida (**Race Conditions**) e modificação concorrente de coleções (**ConcurrentModificationException**);
- Prevenir travamentos mútuos (**Deadlocks**) e retenção desnecessária de monitores;
- Realizar encerramento gracioso e determinístico (**Graceful Shutdown**), sem deixar threads órfãs.

---

## 2. Análise Detalhada de Cada Tomada de Decisão

---

### Decisão 1: Por que NÃO utilizar `Thread.sleep()` ou Polling para Pausar uma Faixa?

#### ❌ Abordagem Incorreta (Polling com Sleep):
```java
// Código problemático:
while (pausado) {
    Thread.sleep(100); // Polling periódico
}
```

#### 💡 Justificativa da Decisão:
1. **Ineficiência e Latência Indesejada:**
   - Se uma thread dorme 100ms em cada ciclo, quando o DJ enviar o comando `play`, a faixa pode demorar até 100ms para reagir (latência perceptível).
   - Se o tempo do sleep for reduzido (ex.: 5ms), a thread acorda dezenas de vezes por segundo desnecessariamente, gerando trocas de contexto (*context switching*) e gastando ciclos de CPU sem realizar trabalho útil.
2. **Falta de Primitiva de Comunicação Direta:**
   - `sleep` é uma pausa temporal cega; ele não sabe quando o evento "DJ deu play" aconteceu.
   - O correto para coordenação entre threads que aguardam eventos/condições é o mecanismo de sinalização do monitor: **`wait()` e `notifyAll()`**. Com `wait()`, a thread suspende sua execução e **só acorda no exato instante em que o comando `play` é executado**, com latência próxima de zero e consumo zero de CPU durante a pausa.

---

### Decisão 2: Por que NÃO utilizar *Busy-Wait* (`while (pausado) {}`)?

#### ❌ Abordagem Incorreta (Espera Ocupada):
```java
// Código catastrófico:
while (pausado) {
    // Loop vazio aguardando a variável mudar
}
```

#### 💡 Justificativa da Decisão:
1. **Queima de 100% de CPU:**
   - O *busy-waiting* mantém o núcleo do processador executando instruções em velocidade máxima ininterruptamente, apenas para checar uma variável booleana.
   - Três faixas pausadas em busy-wait poderiam saturar múltiplos núcleos do computador do usuário, aquecendo a máquina e esgotando bateria/recursos.
2. **Prioridade e Starvation:**
   - O loop ocupado pode prejudicar outras threads prontas para executar, incluindo a própria thread do console que precisa ler o teclado para retomar a música.
3. **Solução Adotada:**
   - Uso de `lock.wait()`, onde o Sistema Operacional coloca a thread no estado `WAITING`, removendo-a da fila de execução do escalonador até receber um sinal explícito.

---

### Decisão 3: Por que NÃO utilizar `Thread.suspend()` e `Thread.resume()`?

#### ❌ Abordagem Incorreta:
```java
threadInstrumento.suspend(); // DEPRECATED e PROIBIDO
threadInstrumento.resume();
```

#### 💡 Justificativa da Decisão:
1. **Risco Crítico de Deadlock:**
   - `Thread.suspend()` foi marcada como **`@Deprecated`** logo nas primeiras versões do Java porque ela suspende a thread sem liberar os locks (monitores) que a thread estiver segurando naquele momento.
   - Se a thread for suspensa enquanto segura um lock ou acessa um recurso crítico e a thread que iria chamá-la de volta (`resume`) precisar do mesmo lock para continuar, o sistema entra em **Deadlock irreversível**.
2. **Solução Adotada:**
   - Controle cooperativo de fluxo através de variáveis de estado sincronizadas e métodos canônicos `wait()` / `notifyAll()`.

---

### Decisão 4: Por que o `wait()` deve estar OBRIGATORIAMENTE dentro de um `while` (e nunca em um `if`)?

#### ❌ Abordagem Incorreta (Uso de `if`):
```java
if (estado == EstadoFaixa.PAUSADO) {
    lock.wait(); // ERRADO!
}
```

#### ✅ Abordagem Correta (Uso de `while`):
```java
while (estado == EstadoFaixa.PAUSADO && ativo) {
    lock.wait(); // CORRETO!
}
```

#### 💡 Justificativa da Decisão:
1. **Acordares Espúrios (*Spurious Wakeups*):**
   - Na especificação da JVM e na arquitetura POSIX/Windows de threads, uma thread pode acordar do `wait()` sem que nenhum `notify()` ou `notifyAll()` tenha sido disparado (fenômeno de hardware/kernel conhecido como *spurious wakeup*).
2. **Mudança de Condição antes de Adquirir o Lock:**
   - Mesmo com um `notifyAll()`, quando a thread acorda, ela precisa reobter o lock. Entre o momento do sinal e a aquisição do lock, o estado pode ter mudado novamente.
3. **Regra Canônica da JVM:**
   - O `while` garante que, sempre que a thread acordar por qualquer motivo, ela **revalida a condição** antes de prosseguir. Se ainda estiver `PAUSADO`, ela volta a dormir imediatamente.

---

### Decisão 5: Por que `Thread.sleep()` deve ficar ESTRITAMENTE FORA do bloco `synchronized`?

#### ❌ Abordagem Incorreta:
```java
synchronized (lock) {
    if (estado == EstadoFaixa.TOCANDO) {
        tocarBatida();
        Thread.sleep(intervaloMs); // PERIGO! Dormir segurando o lock!
    }
}
```

#### ✅ Abordagem Adotada:
```java
synchronized (lock) {
    while (estado == EstadoFaixa.PAUSADO && ativo) {
        lock.wait();
    }
}

if (estado == EstadoFaixa.TOCANDO) {
    tocarBatida();
}

// O sleep é executado FORA do synchronized:
Thread.sleep(intervaloMs);
```

#### 💡 Justificativa da Decisão:
1. **Diferença fundamental entre `wait()` e `sleep()`:**
   - `lock.wait()` **libera** temporariamente o monitor `lock` enquanto a thread dorme.
   - `Thread.sleep()` **NÃO LIBERA** nenhum monitor ou lock que a thread esteja segurando.
2. **Bloqueio dos Comandos do DJ:**
   - Se o `Thread.sleep(1000)` estivesse dentro do bloco `synchronized(lock)`, qualquer comando enviado pelo console (`pause`, `play`, `stop`) que tentasse obter o `synchronized(lock)` ficaria **travado e bloqueado** por até 1 segundo esperando o sleep da faixa terminar.
   - Ao executar o sleep fora da região crítica, os comandos do DJ adquirem o lock e alteram o estado do instrumento instantaneamente.

---

### Decisão 6: Por que utilizar `notifyAll()` em vez de `notify()`?

#### 💡 Justificativa da Decisão:
1. **Prevenção de Sinais Perdidos:**
   - `notify()` acorda apenas **uma única thread arbitrária** que esteja no conjunto de espera (*wait-set*) do objeto. Se múltiplas threads ou verificações dependerem do mesmo lock, não há garantia de que a thread desejada acordará.
2. **Robustez e Manutenibilidade:**
   - O uso de `notifyAll()` acorda todas as threads que aguardam aquele lock. Cada uma, ao acordar dentro de seu respectivo loop `while`, verifica sua condição e prossegue com segurança.
   - Em padrões concorrentes profissionais Java, `notifyAll()` é a recomendação padrão para evitar que notificações fiquem retidas.

---

### Decisão 7: Por que usar Flag `volatile boolean ativo` + `thread.interrupt()` em vez de `Thread.stop()`?

#### ❌ Abordagem Incorreta:
```java
threadInstrumento.stop(); // DEPRECATED e EXTREMAMENTE INSEGURO
```

#### ✅ Abordagem Adotada:
```java
public void stop() {
    synchronized (lock) {
        this.ativo = false;
        this.estado = EstadoFaixa.PARADO;
        lock.notifyAll(); // Destrava se estiver em wait()
    }
    if (thread != null) {
        thread.interrupt(); // Destrava se estiver em sleep()
    }
}
```

#### 💡 Justificativa da Decisão:
1. **Insegurança do `Thread.stop()`:**
   - `Thread.stop()` mata a thread de forma bruta e abrupta lançando um erro `ThreadDeath`, liberando todos os locks instantaneamente sem deixar o código limpar recursos ou completar transações atômicas, deixando objetos em estado corrompido.
2. **Encerramento Gracioso e Cooperativo:**
   - O uso da flag `ativo = false` permite que a thread termine seu ciclo de vida normalmente.
   - `lock.notifyAll()` acorda a thread se ela estiver travada em `wait()`.
   - `thread.interrupt()` faz o `Thread.sleep()` lançar uma `InterruptedException` imediatamente, acordando a thread sem precisar esperar os segundos restantes da batida para encerrar.

---

### Decisão 8: Por que a palavra-chave `volatile` nos campos `ativo`, `estado` e `bpm`?

#### 💡 Justificativa da Decisão:
1. **Problema de Visibilidade de Memória (Caches L1/L2 dos Núcleos):**
   - Em arquiteturas modernas de CPU multiprocessadas, cada núcleo possui seus próprios caches locais de memória. Sem sincronização ou `volatile`, uma escrita feita pela thread do Console na variável `bpm` ou `ativo` pode ficar retida no cache daquele núcleo e **nunca ser vista** pela thread do Instrumento rodando em outro núcleo.
2. **Garantia de *Happens-Before*:**
   - O modificador `volatile` força todas as leituras e escritas a irem diretamente para a memória principal compartilhada (RAM/Cache Coerente), garantindo visibilidade imediata entre threads distintas sem a sobrecarga de bloqueios pesados.

---

### Decisão 9: Por que `ConcurrentHashMap` no `Mixer` em vez de `HashMap` tradicional?

#### ❌ Abordagem Problemática (`HashMap`):
```java
Map<String, Instrumento> faixas = new HashMap<>();
```

#### ✅ Abordagem Adotada:
```java
Map<String, Instrumento> faixas = new ConcurrentHashMap<>();
```

#### 💡 Justificativa da Decisão:
1. **Modificações e Leituras Simultâneas:**
   - O DJ pode executar `add guitarra` (que insere no mapa) no exato instante em que o comando `list` ou o `PainelStatus` está iterando sobre as faixas para exibir o status.
2. **`ConcurrentModificationException`:**
   - O `HashMap` padrão do Java possui iteradores do tipo *fail-fast*. Uma modificação estrutural durante uma iteração estoura `ConcurrentModificationException` e quebra o programa.
3. **Escalabilidade com Locks Granulares:**
   - O `ConcurrentHashMap` suporta leituras totalmente não-bloqueantes e escritas concorrentes em diferentes segmentos/buckets sem travar a coleção inteira, oferecendo alta performance e segurança de threads.

---

### Decisão 10: Por que utilizar `thread.join()` no comando `exit`?

#### 💡 Justificativa da Decisão:
1. **Evitar Threads Órfãs e Encerramento Incompleto:**
   - Quando o usuário digita `exit`, se a thread principal simplesmente chamasse `return`, o programa poderia finalizar com threads de som ainda executando em segundo plano ou gerar saída desordenada no terminal.
2. **Sincronização de Término:**
   - O método `join()` bloqueia a thread chamadora até que a thread do instrumento alvo encerre completamente seu método `run()`.
   - Dessa forma, o `Mixer.pararTudoEFinalizar()` garante que todas as faixas pararam antes de exibir a mensagem de desligamento e encerrar o processo.

---

### Decisão 11: Por que utilizar um Lock Privado (`final Object lock = new Object();`)?

#### 💡 Justificativa da Decisão:
1. **Encapsulamento de Monitores:**
   - Se utilizássemos `synchronized(this)`, qualquer classe externa que obtivesse uma referência do objeto `Instrumento` poderia fazer `synchronized(instrumento)` e travar acidentalmente (ou maliciosamente) os monitores internos do instrumento.
2. **Isolamento de Responsabilidades:**
   - O lock privado `private final Object lock = new Object();` não pode ser acessado fora da classe `Instrumento`, garantindo total integridade da seção crítica interna.

---

### Decisão 12: Por que Sincronizar a Saída do Console e Utilizar Painel em Menu Estático?

#### 💡 Justificativa da Decisão:
1. **Ergonomia e Usabilidade da CLI:**
   - Em aplicações multithread interativas de console, se cada thread imprimisse texto na tela continuamente a cada batida (ex: centenas de milissegundos), o cursor do teclado no prompt `DJ>` seria constantemente interrompido e rolado para baixo, impedindo o usuário de digitar comandos.
   - Para resolver isso, as threads de instrumento executam o áudio MIDI de forma autônoma e não-bloqueante em segundo plano, enquanto o console exibe um **painel estático em formato de menu** com o resumo atualizado das faixas a cada comando executado.
2. **Centralização e Thread-Safety (`Console.log`):**
   - Centralizar todas as mensagens em `Console.log()` com um monitor estático unificado (`PRINT_LOCK`) garante que cada tabela de status, resposta de comando e aviso de sistema seja impresso de forma atômica e sem intercalação de caracteres.

---

### Decisão 13: Por que um Relógio Mestre em vez de `sleep(intervalo)` a cada volta?

#### ❌ Abordagem Ingênua (temporização relativa):
```java
while (ativo) {
    tocarBatida();
    Thread.sleep(60_000 / bpm); // dorme "um intervalo" a partir de agora
}
```

#### 💡 Justificativa da Decisão:
1. **Acúmulo de Erro (Drift):**
   - O tempo gasto tocando a nota, o custo do escalonamento e a imprecisão do `sleep` (que garante *no mínimo* o tempo pedido, nunca *exatamente*) se somam a **cada volta do loop**.
   - Como cada thread é escalonada de forma independente pelo SO, esse erro é diferente em cada faixa. Em poucos segundos o baixo já não cai mais junto com a bateria — o arranjo deixa de soar como uma música e vira três loops soltos.
2. **Abordagem Adotada (temporização absoluta):**
   - Todas as threads compartilham uma origem única (`RelogioMestre`, baseada em `System.nanoTime()`, que é monotônico) e calculam o **instante absoluto do próximo passo**, dormindo somente o tempo que falta para chegar lá.
   - O erro de uma volta não é herdado pela volta seguinte: o desvio fica limitado à precisão de um único `sleep`, em vez de crescer indefinidamente.
3. **Benefício Extra — Reentrada no Compasso:**
   - Como o índice do passo é **derivado do tempo absoluto**, e não de um contador interno da faixa, uma faixa que volta de um `pause` reentra na posição correta do compasso. É o que permite pausar a bateria ao vivo e trazê-la de volta em cima do tempo.
4. **Analogia com Sistemas Distribuídos:**
   - É o mesmo princípio de sincronização por relógio comum usado entre processos/nós: nenhuma parte tenta contar o tempo sozinha; todas concordam sobre uma referência única.

---

### Decisão 14: Por que separar "subdivisão rítmica" do BPM?

#### 💡 Justificativa da Decisão:
1. **Cada Instrumento Toca em uma Densidade Diferente:**
   - No mesmo andamento de 117 BPM, a bateria e o baixo de "Billie Jean" andam em colcheias (256 ms por passo) enquanto o pad de teclado ataca um acorde por compasso (512 ms por passo).
   - Por isso o intervalo da thread é `60_000 / (bpm * subdivisao)`, e não apenas `60_000 / bpm`.
2. **Consistência entre Threads:**
   - Como todos os intervalos são divisores/múltiplos exatos ancorados na mesma origem, faixas com subdivisões diferentes continuam coincidindo nos pontos comuns da grade — o pad cai exatamente junto com o bumbo do tempo 1.
3. **Separação de Responsabilidades:**
   - A música (padrões, notas, timbres) fica isolada em `Padrao.java`; a concorrência fica isolada em `Instrumento.java`. Dá para trocar a música inteira sem tocar em uma linha de código de sincronização.

---

### Decisão 15: Como proteger o sintetizador MIDI, que é um recurso compartilhado?

#### 💡 Justificativa da Decisão:
1. **Um Canal MIDI por Faixa:**
   - O sintetizador é único e acessado por todas as threads. Cada faixa recebe um **canal MIDI exclusivo** (canal 9 reservado à percussão, conforme a especificação General MIDI), o que elimina a disputa por timbre e por notas ativas entre threads diferentes.
   - Os canais são devolvidos a um conjunto de livres quando a faixa é encerrada, permitindo `add` e `stop` repetidos sem esgotar os 16 canais.
2. **Estado Mutável sob Monitor:**
   - O único estado compartilhado que resta (quais notas estão soando em um canal) fica encapsulado na classe `Voz`, com métodos `synchronized`. O mapa de vozes é um `ConcurrentHashMap` porque o comando `add` pode inserir uma faixa nova enquanto as demais tocam.
3. **Chamada de Áudio Fora da Região Crítica:**
   - No `pause`, a faixa é silenciada **depois** de sair do bloco `synchronized (lock)`. Nenhuma operação de E/S acontece com o monitor do instrumento na mão — mesma regra aplicada ao `Thread.sleep`.
4. **Liberação pela Própria Thread:**
   - O canal só é devolvido quando a thread do instrumento sai do seu laço (`run()`), e não pelo console. Isso elimina, por construção, a corrida entre "encerrar a faixa" e "emitir a última nota".

---

### Decisão 16: Por que as faixas nascem em `PAUSADO` em vez de subirem tocando e serem pausadas em seguida?

#### ❌ Abordagem Ingênua:
```java
mixer.adicionarInstrumento(inst); // start() -> a thread já começa a tocar
mixer.pause("baixo");             // ...e só depois é silenciada
```

#### 💡 Justificativa da Decisão:
1. **Janela de Corrida entre `start()` e `pause()`:**
   - Entre o `start()` e o `pause()` existe um intervalo real de tempo em que a thread já está executando. Com o escalonador do SO decidindo a ordem, a faixa pode emitir uma ou mais batidas antes de ser pausada — um "vazamento" audível de som que deveria estar em silêncio.
2. **Abordagem Adotada:**
   - O estado inicial é um parâmetro do construtor: a thread nasce em `PAUSADO` e a primeira coisa que o método `run()` faz é entrar no `synchronized`/`wait()`. Não existe janela nenhuma, por construção.
3. **Valor para a Apresentação:**
   - Isso é o que permite carregar uma música inteira **em silêncio**, com todas as threads vivas, e montá-la ao vivo faixa por faixa: cada `play` é uma demonstração isolada de `notifyAll()` acordando exatamente uma thread.
4. **Princípio Geral:**
   - Estado inicial correto vale mais do que correção posterior. Sempre que possível, um objeto concorrente deve nascer no estado desejado, em vez de nascer em um estado qualquer e ser ajustado por outra thread depois.

---

### Decisão 17: Por que trocar de música derruba as threads em vez de reaproveitá-las?

#### 💡 Justificativa da Decisão:
1. **Ciclo de Vida Explícito:**
   - Cada música tem um conjunto próprio de faixas (Sweet Dreams tem riff de sintetizador; Billie Jean tem pad de teclado). Reaproveitar threads exigiria trocar o padrão, o timbre e o canal MIDI de uma thread em execução — ou seja, mutação de estado compartilhado no meio do laço, exatamente o que se quer evitar.
2. **Abordagem Adotada:**
   - `carregarMusica()` chama `pararTodas()`, que faz `stop()` + `join()` em cada faixa antes de subir o novo arranjo. O mesmo caminho de encerramento gracioso usado no `exit` é exercitado a cada troca de música — inclusive na frente do professor.
3. **Separação Música/Concorrência:**
   - Como todo o material musical está em `Musica.java` e `Padrao.java`, acrescentar uma quarta música é escrever dados, não código concorrente. A classe `Instrumento` não sabe (nem precisa saber) qual música está tocando.

---

## 3. Resumo Comparativo: Práticas Adotadas vs. Más Práticas

| Aspecto | Má Prática Evitada | Prática Adotada no Projeto | Benefício Obtido |
|---|---|---|---|
| **Pausa de Faixa** | `while(pausado){}` (Busy-wait) | `lock.wait()` em loop `while` | 0% de uso de CPU durante a pausa e resposta imediata |
| **Retomada de Faixa** | `thread.resume()` ou polling | `lock.notifyAll()` dentro de `synchronized` | Imunidade a Deadlocks e acionamento determinístico |
| **Temporização da Batida** | `sleep()` dentro do lock | `sleep()` fora do lock | Comandos do DJ respondem na hora sem serem bloqueados |
| **Validação de Espera** | `if (pausado) wait()` | `while (pausado) wait()` | Imunidade a Acordares Espúrios (*Spurious Wakeups*) |
| **Parada da Thread** | `thread.stop()` | `volatile boolean ativo` + `thread.interrupt()` | Encerramento seguro sem corromper memória |
| **Visibilidade de Variáveis** | Variáveis sem proteção | `volatile` (`bpm`, `estado`, `ativo`) | Leituras imediatas entre múltiplos núcleos de CPU |
| **Coleção de Faixas** | `HashMap` não-sincronizado | `ConcurrentHashMap` | Sem `ConcurrentModificationException` ao adicionar e listar faixas |
| **Encerramento Geral** | Deixar threads penduradas | `thread.join()` em todas as faixas | Zero threads órfãs e saída limpa da JVM |
| **Temporização do Arranjo** | `sleep(intervalo)` relativo a cada volta | Instante absoluto a partir do `RelogioMestre` | Faixas não derivam entre si; reentram no compasso após o `pause` |
| **Áudio Compartilhado** | Todas as threads no mesmo canal MIDI | Um canal exclusivo por faixa + estado sob monitor | Sem disputa de timbre nem notas presas soando |
| **Estado Inicial da Faixa** | `start()` tocando e `pause()` logo depois | Thread nasce em `PAUSADO` (parâmetro do construtor) | Sem janela de corrida e sem vazamento de som antes do comando |
