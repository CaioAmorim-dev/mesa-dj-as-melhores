package br.com.cesar.dj;

/**
 * Relógio mestre compartilhado por todas as threads de instrumento.
 *
 * <p>Problema que ele resolve: se cada thread apenas executa {@code trabalho(); sleep(intervalo);},
 * o tempo gasto no trabalho se acumula a cada volta e as faixas <b>derivam</b> umas das outras
 * (em poucos segundos o baixo sai do tempo da bateria). Como cada thread é escalonada pelo SO de
 * forma independente, não existe garantia de que elas acordem juntas.</p>
 *
 * <p>Solução adotada: em vez de "dormir um intervalo", cada thread calcula o <b>instante absoluto</b>
 * do próximo passo a partir de uma origem única e comum, e dorme apenas o tempo que falta para
 * chegar lá. Assim o erro nunca acumula e todas as faixas permanecem travadas na mesma grade
 * rítmica (phase-lock), que é o que faz o arranjo soar como uma música e não como três loops soltos.</p>
 *
 * <p>Efeito colateral desejado: como o índice do passo é derivado do tempo absoluto, uma faixa que
 * volta de um {@code pause} reentra <b>no lugar certo do compasso</b>, e não do ponto onde parou.</p>
 */
public final class RelogioMestre {

    /**
     * Origem da grade rítmica. É {@code volatile} porque é escrita pela thread do console
     * (comando {@code sync}) e lida por todas as threads de instrumento.
     */
    private static volatile long origemNs = System.nanoTime();

    private RelogioMestre() {
        // Classe utilitária: não deve ser instanciada.
    }

    /**
     * Reinicia a grade rítmica no instante atual (comando {@code sync} do DJ).
     */
    public static void sincronizar() {
        origemNs = System.nanoTime();
    }

    /**
     * Tempo decorrido, em milissegundos, desde a origem da grade.
     * Usa {@link System#nanoTime()} (relógio monotônico) e não {@code currentTimeMillis},
     * que pode andar para trás se o relógio do sistema for ajustado.
     */
    public static long decorridoMs() {
        return (System.nanoTime() - origemNs) / 1_000_000L;
    }

    /**
     * Número do passo atual da grade para um dado intervalo.
     * Duas faixas com o mesmo intervalo enxergam sempre o mesmo número — é isso que as mantém juntas.
     */
    public static long passoAtual(long intervaloMs) {
        long intervalo = Math.max(1L, intervaloMs);
        return decorridoMs() / intervalo;
    }

    /**
     * Milissegundos que faltam até o próximo ponto da grade (valor a ser passado ao sleep).
     */
    public static long msAteProximoPasso(long intervaloMs) {
        long intervalo = Math.max(1L, intervaloMs);
        long restante = intervalo - (decorridoMs() % intervalo);
        return restante <= 0 ? intervalo : restante;
    }
}
