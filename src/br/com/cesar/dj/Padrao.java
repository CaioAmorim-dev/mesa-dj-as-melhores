package br.com.cesar.dj;

/**
 * Padrão rítmico/melódico de uma faixa — é aqui que mora a "música", separada da concorrência.
 *
 * <p>Um padrão é uma sequência de <b>passos</b>. Cada passo é um conjunto de notas MIDI tocadas
 * simultaneamente (um acorde, ou bumbo + chimbal ao mesmo tempo). A {@code subdivisao} diz quantos
 * passos cabem em uma batida: 1 = semínimas, 2 = colcheias, 4 = semicolcheias.</p>
 *
 * <p>Convenção dos passos:</p>
 * <ul>
 *   <li>{@link #SUSTENTA} ..... deixa soando o que já estava tocando (acordes longos);</li>
 *   <li>{@link #SILENCIO} ..... corta o som (pausa musical);</li>
 *   <li>{@code {60, 64}} ...... toca essas notas MIDI juntas.</li>
 * </ul>
 *
 * <p>Referência de notas MIDI: 36 = C2, 48 = C3, 60 = C4 (dó central).</p>
 *
 * <p>Os padrões concretos de cada música ficam no catálogo {@link Musica}.</p>
 */
public final class Padrao {

    /** Passo que silencia a faixa (nenhuma nota). */
    public static final int[] SILENCIO = new int[0];

    /** Passo que não faz nada: o acorde/nota anterior continua soando. */
    public static final int[] SUSTENTA = null;

    private final String descricao;
    private final int patch;        // Instrumento General MIDI (0-based). Ignorado na percussão.
    private final boolean percussao;
    private final int velocidade;   // Volume/intensidade da nota (0 a 127)
    private final int subdivisao;   // Passos por batida
    private final boolean deixarSoar; // true = as notas se sobrepoem, como em um violao dedilhado
    private final int[][] passos;

    private Padrao(String descricao, int patch, boolean percussao, int velocidade, int subdivisao,
                   boolean deixarSoar, int[][] passos) {
        this.descricao = descricao;
        this.patch = patch;
        this.percussao = percussao;
        this.velocidade = velocidade;
        this.subdivisao = Math.max(1, subdivisao);
        this.deixarSoar = deixarSoar;
        this.passos = passos;
    }

    /**
     * Cria um padrão melódico (baixo, teclado, guitarra...) em um canal MIDI próprio.
     *
     * @param patch      instrumento General MIDI (0-based): 38 = Synth Bass, 89 = Warm Pad, 25 = Violão de aço...
     * @param subdivisao passos por batida: 1 = semínimas, 2 = colcheias, 4 = semicolcheias
     */
    public static Padrao melodico(String descricao, int patch, int velocidade, int subdivisao, int[][] passos) {
        return new Padrao(descricao, patch, false, velocidade, subdivisao, false, passos);
    }

    /**
     * Cria um padrao melodico dedilhado: cada nova nota NAO corta a anterior, de forma que o
     * arpejo va se acumulando e decaindo sozinho, como as cordas de um violao de verdade.
     */
    public static Padrao dedilhado(String descricao, int patch, int velocidade, int subdivisao, int[][] passos) {
        return new Padrao(descricao, patch, false, velocidade, subdivisao, true, passos);
    }

    /**
     * Cria um padrão de percussão. Vai sempre para o canal 9, reservado à bateria pela
     * especificação General MIDI: 36 = bumbo, 38 = caixa, 42 = chimbal fechado, 39 = palmas.
     */
    public static Padrao percussivo(String descricao, int velocidade, int subdivisao, int[][] passos) {
        return new Padrao(descricao, -1, true, velocidade, subdivisao, true, passos);
    }

    /**
     * Notas do passo indicado. O índice vem do relógio mestre e é reduzido pelo tamanho do
     * padrão, de modo que a posição no compasso é sempre absoluta — nunca depende de há quanto
     * tempo a faixa está tocando.
     */
    public int[] notasDoPasso(long indicePasso) {
        if (passos == null || passos.length == 0) {
            return SILENCIO;
        }
        int i = Math.floorMod(indicePasso, passos.length);
        return passos[i];
    }

    /** Quantidade de compassos de um ciclo completo do padrão (4/4). */
    public int getCompassos() {
        return passos == null ? 0 : Math.max(1, passos.length / (4 * subdivisao));
    }

    /** Rótulo curto da grade rítmica: 1/4, 1/8, 1/16... */
    public String getGrade() {
        return switch (subdivisao) {
            case 1 -> "1/4";
            case 2 -> "1/8";
            case 4 -> "1/16";
            default -> "1/" + (4 * subdivisao);
        };
    }

    public String getDescricao() {
        return descricao;
    }

    public int getPatch() {
        return patch;
    }

    public boolean isPercussao() {
        return percussao;
    }

    public boolean isDeixarSoar() {
        return deixarSoar;
    }

    public int getVelocidade() {
        return velocidade;
    }

    public int getSubdivisao() {
        return subdivisao;
    }
}
