package br.com.cesar.dj;

import java.util.List;

/**
 * Catálogo do repertório da mesa. Cada música é um conjunto de faixas — e cada faixa vira,
 * no {@link Mixer}, uma thread independente.
 *
 * <p>Toda a parte musical (notas, ritmos, timbres e andamento) está concentrada aqui, separada
 * da parte de concorrência. Trocar de música em cima do palco é derrubar as threads atuais e
 * subir as threads do novo arranjo: nenhuma linha de sincronização precisa ser tocada.</p>
 *
 * <p>Notas MIDI usadas nos padrões: 36 = C2, 48 = C3, 60 = C4 (dó central).
 * Na percussão (canal 9): 36 = bumbo, 38 = caixa, 39 = palmas, 42 = chimbal fechado.</p>
 */
public final class Musica {

    /** Uma faixa do arranjo: o nome que o DJ digita, o "som" exibido na tabela e o padrão tocado. */
    public record Faixa(String nome, String som, Padrao padrao) {
    }

    private final String id;
    private final String titulo;
    private final String tonalidade;
    private final int bpm;
    private final List<Faixa> faixas;
    private final List<Faixa> extras;
    private final int[] escalaParaImproviso;

    private Musica(String id, String titulo, String tonalidade, int bpm,
                   List<Faixa> faixas, List<Faixa> extras, int[] escalaParaImproviso) {
        this.id = id;
        this.titulo = titulo;
        this.tonalidade = tonalidade;
        this.bpm = bpm;
        this.faixas = faixas;
        this.extras = extras;
        this.escalaParaImproviso = escalaParaImproviso;
    }

    // ==================================================================================
    // 1. BILLIE JEAN — Michael Jackson (1982), 117 BPM, Fá# menor
    // ==================================================================================
    public static Musica billieJean() {
        // Bumbo nos tempos 1 e 3, caixa nos tempos 2 e 4, chimbal em todas as colcheias.
        Padrao bateria = Padrao.percussivo("bumbo 1-3, caixa 2-4, chimbal", 100, 2, new int[][]{
                {36, 42}, {42}, {38, 42}, {42},
                {36, 42}, {42}, {38, 42}, {42}
        });

        // A linha de baixo mais famosa do pop: F#2 C#3 E3 F#3 E3 C#3 B2 C#3, em colcheias.
        Padrao baixo = Padrao.melodico("riff F#m em colcheias", 38 /* Synth Bass 1 */, 105, 2, new int[][]{
                {42}, {49}, {52}, {54},
                {52}, {49}, {47}, {49}
        });

        // O verso é um vamp de um acorde só: F#m7 atacado no tempo 1 e sustentado.
        Padrao synth = Padrao.melodico("pad F#m7 sustentado", 89 /* Warm Pad */, 68, 1, new int[][]{
                {54, 57, 61, 64}, Padrao.SUSTENTA, Padrao.SUSTENTA, Padrao.SUSTENTA
        });

        // Extras: a palhetada seca dos contratempos e os adlibs de metais.
        Padrao guitarra = Padrao.melodico("chop de contratempo F#m", 28 /* Guitarra abafada */, 78, 2, new int[][]{
                Padrao.SILENCIO, {54, 57, 61}, Padrao.SILENCIO, {54, 57, 61},
                Padrao.SILENCIO, {54, 57, 61}, Padrao.SILENCIO, {54, 57, 61}
        });
        Padrao vocal = Padrao.melodico("adlibs de metais (F#m)", 62 /* Synth Brass */, 88, 2, new int[][]{
                {73}, Padrao.SUSTENTA, {71}, Padrao.SUSTENTA, {69}, Padrao.SILENCIO, Padrao.SILENCIO, Padrao.SILENCIO,
                {66}, Padrao.SUSTENTA, Padrao.SILENCIO, Padrao.SILENCIO, {69}, Padrao.SUSTENTA, Padrao.SILENCIO, Padrao.SILENCIO
        });

        return new Musica("billie", "Billie Jean - Michael Jackson", "Fa# menor", 117,
                List.of(new Faixa("Bateria", "tum-tss", bateria),
                        new Faixa("Baixo", "dum-dum", baixo),
                        new Faixa("Synth", "piiim", synth)),
                List.of(new Faixa("Guitarra", "chick", guitarra),
                        new Faixa("Vocal", "hee-hee", vocal)),
                new int[]{66, 69, 71, 73, 76} /* pentatonica de Fa# menor */);
    }

    // ==================================================================================
    // 2. SEVEN NATION ARMY — The White Stripes (2003), 124 BPM, Mi menor
    // ==================================================================================
    public static Musica sevenNationArmy() {
        // A bateria da música é deliberadamente crua: bumbo e caixa, sem nenhum prato.
        Padrao bateria = Padrao.percussivo("bumbo 1-3, caixa 2-4, sem prato", 105, 2, new int[][]{
                {36}, Padrao.SILENCIO, {38}, Padrao.SILENCIO,
                {36}, Padrao.SILENCIO, {38}, Padrao.SILENCIO
        });

        // O riff, em dois compassos: E - E - G - E - D - C - B.
        Padrao baixo = Padrao.melodico("riff E-E-G-E-D-C-B", 33 /* Baixo eletrico */, 108, 2, new int[][]{
                {40}, Padrao.SUSTENTA, Padrao.SUSTENTA, {40},
                {43}, Padrao.SUSTENTA, {40}, Padrao.SUSTENTA,
                {38}, Padrao.SUSTENTA, Padrao.SUSTENTA, {36},
                Padrao.SUSTENTA, {35}, Padrao.SUSTENTA, Padrao.SUSTENTA
        });

        // A guitarra dobra o mesmo riff uma oitava acima, com distorção.
        Padrao guitarra = Padrao.melodico("riff dobrado com distorcao", 30 /* Distortion Guitar */, 92, 2, new int[][]{
                {52}, Padrao.SUSTENTA, Padrao.SUSTENTA, {52},
                {55}, Padrao.SUSTENTA, {52}, Padrao.SUSTENTA,
                {50}, Padrao.SUSTENTA, Padrao.SUSTENTA, {48},
                Padrao.SUSTENTA, {47}, Padrao.SUSTENTA, Padrao.SUSTENTA
        });

        // Extras: palmas de estádio nos tempos 2 e 4 e o riff na oitava aguda.
        Padrao palmas = Padrao.percussivo("palmas nos tempos 2 e 4", 95, 2, new int[][]{
                Padrao.SILENCIO, Padrao.SILENCIO, {39}, Padrao.SILENCIO,
                Padrao.SILENCIO, Padrao.SILENCIO, {39}, Padrao.SILENCIO
        });
        Padrao solo = Padrao.melodico("riff na oitava aguda", 30 /* Distortion Guitar */, 80, 2, new int[][]{
                {64}, Padrao.SUSTENTA, Padrao.SUSTENTA, {64},
                {67}, Padrao.SUSTENTA, {64}, Padrao.SUSTENTA,
                {62}, Padrao.SUSTENTA, Padrao.SUSTENTA, {60},
                Padrao.SUSTENTA, {59}, Padrao.SUSTENTA, Padrao.SUSTENTA
        });

        return new Musica("seven", "Seven Nation Army - The White Stripes", "Mi menor", 124,
                List.of(new Faixa("Bateria", "bum-ta", bateria),
                        new Faixa("Baixo", "dum-dum", baixo),
                        new Faixa("Guitarra", "raaaw", guitarra)),
                List.of(new Faixa("Palmas", "clap", palmas),
                        new Faixa("Solo", "wiiii", solo)),
                new int[]{64, 67, 69, 71, 74} /* pentatonica de Mi menor */);
    }

    // ==================================================================================
    // 3. SWEET DREAMS - Eurythmics (1983), 126 BPM, Do menor
    //
    // Esta e a musica do repertorio que chega mais perto do original, e a razao e tecnica:
    // o disco foi feito com sequenciador, sintetizador e bateria eletronica - exatamente o
    // que esta mesa e. O Gervill nao esta IMITANDO um instrumento aqui, ele E o mesmo tipo
    // de instrumento que gravou a faixa.
    //
    // Ressalva honesta: o andamento, a tonalidade e o carater sao fieis; o desenho exato do
    // riff e uma reconstrucao aproximada, nao uma transcricao verificada.
    // ==================================================================================
    public static Musica sweetDreams() {
        // O riff hipnotico de sintetizador, em colcheias, dois compassos.
        // Nota repetida no ataque, descida ate a fundamental, respiro, e o mesmo desenho
        // um semitom acima no segundo compasso.
        Padrao riff = Padrao.melodico("riff de synth em Dó menor", 81 /* Lead 2 sawtooth */, 96, 2, new int[][]{
                {67}, {67}, {63}, {60},
                Padrao.SUSTENTA, {63}, {60}, Padrao.SUSTENTA,
                {68}, {68}, {63}, {60},
                Padrao.SUSTENTA, {58}, {60}, Padrao.SUSTENTA
        });

        // O baixo toca EXATAMENTE a mesma linha, uma oitava abaixo - marca registrada da faixa.
        // Duas threads independentes executando o mesmo desenho ritmico ao mesmo tempo: se o
        // relogio nao fosse compartilhado, daria para ouvir um eco entre as duas.
        Padrao baixo = Padrao.melodico("mesma linha, uma oitava abaixo", 38 /* Synth Bass 1 */, 104, 2, new int[][]{
                {55}, {55}, {51}, {48},
                Padrao.SUSTENTA, {51}, {48}, Padrao.SUSTENTA,
                {56}, {56}, {51}, {48},
                Padrao.SUSTENTA, {46}, {48}, Padrao.SUSTENTA
        });

        // Bateria eletronica, seca e sem variacao: bumbo em 1 e 3, caixa dobrada com palmas
        // em 2 e 4, chimbal nas colcheias. E o comportamento de uma maquina, nao de um baterista.
        Padrao bateria = Padrao.percussivo("bateria eletrônica 4/4", 100, 2, new int[][]{
                {36, 42}, {42}, {38, 39, 42}, {42},
                {36, 42}, {42}, {38, 39, 42}, {42}
        });

        // Extras: o naipe de cordas sintetizadas ao fundo e o riff dobrado na oitava aguda.
        Padrao pad = Padrao.melodico("cordas sintetizadas Cm-Ab", 50 /* Synth Strings */, 58, 1, new int[][]{
                {48, 51, 55}, Padrao.SUSTENTA, Padrao.SUSTENTA, Padrao.SUSTENTA,
                {44, 48, 51}, Padrao.SUSTENTA, Padrao.SUSTENTA, Padrao.SUSTENTA
        });
        Padrao solo = Padrao.melodico("riff na oitava aguda", 80 /* Lead 1 square */, 76, 2, new int[][]{
                {79}, {79}, {75}, {72},
                Padrao.SUSTENTA, {75}, {72}, Padrao.SUSTENTA,
                {80}, {80}, {75}, {72},
                Padrao.SUSTENTA, {70}, {72}, Padrao.SUSTENTA
        });

        return new Musica("sweet", "Sweet Dreams - Eurythmics", "Dó menor", 126,
                List.of(new Faixa("Riff", "piiim", riff),
                        new Faixa("Baixo", "dum-dum", baixo),
                        new Faixa("Bateria", "tum-tss", bateria)),
                List.of(new Faixa("Pad", "aaaah", pad),
                        new Faixa("Solo", "wiiii", solo)),
                new int[]{60, 63, 65, 67, 70} /* pentatonica de Do menor */);
    }

    // ==================================================================================
    // 4. SAVE A PRAYER — Duran Duran (2009 Remaster), ~114 BPM, Re menor
    //
    // Ressalva honesta: o BPM e a tonalidade sao tratados aqui como aproximacoes praticas.
    // O arranjo usa a progressao Dm-F-Bbmaj7-G, muito associada a musica, mas condensada
    // para caber na arquitetura simples de padroes MIDI desta mesa.
    //
    // Como o projeto nao trabalha com audio real, a faixa "Vocal" e uma representacao
    // instrumental da melodia cantada, tocada por um lead suave.
    // ==================================================================================
    public static Musica saveAPrayer() {
        // Bateria pop eletronica: bumbo marcando o pulso, caixa/palmas no 2 e 4 e chimbal
        // em colcheias, com um pequeno acento no fechamento do ciclo.
        Padrao bateria = Padrao.percussivo("bateria eletronica pop 4/4", 96, 2, new int[][]{
                {36, 42}, {42}, {38, 39, 42}, {42},
                {36, 42}, {42}, {38, 39, 42}, {42},
                {36, 42}, {42}, {38, 39, 42}, {42},
                {36, 42}, {42}, {38, 39, 42}, {42, 46}
        });

        // Linha de baixo inspirada na subida/descida do original sobre Dm-F-Bbmaj7-G.
        // Nao e uma transcricao nota a nota: a intencao e capturar o movimento do groove.
        Padrao baixo = Padrao.melodico("baixo Dm-F-Bbmaj7-G", 38 /* Synth Bass 1 */, 102, 2, new int[][]{
                {38}, {45}, {43}, {41}, {41}, {40}, {38}, {45},
                {41}, {48}, {47}, {45}, {45}, {43}, {41}, {48},
                {34}, {41}, {45}, {46}, {46}, {45}, {43}, {41},
                {43}, {50}, {48}, {47}, {45}, {43}, {41}, {40}
        });

        // Pad de synth sustentado com a progressao principal do verso/intro.
        Padrao synth = Padrao.melodico("pad Dm-F-Bbmaj7-G", 89 /* Warm Pad */, 66, 1, new int[][]{
                {50, 53, 57}, Padrao.SUSTENTA, Padrao.SUSTENTA, Padrao.SUSTENTA,
                {53, 57, 60}, Padrao.SUSTENTA, Padrao.SUSTENTA, Padrao.SUSTENTA,
                {46, 50, 53, 57}, Padrao.SUSTENTA, Padrao.SUSTENTA, Padrao.SUSTENTA,
                {43, 47, 50}, Padrao.SUSTENTA, Padrao.SUSTENTA, Padrao.SUSTENTA
        });

        // Teclado/piano marcando a harmonia com ataques leves nos contratempos.
        Padrao piano = Padrao.melodico("piano eletrico Dm-F-Bbmaj7-G", 4 /* Piano eletrico */, 74, 2, new int[][]{
                {62, 65, 69}, Padrao.SILENCIO, Padrao.SILENCIO, {65, 69, 74},
                Padrao.SILENCIO, {62, 65, 69}, Padrao.SILENCIO, Padrao.SILENCIO,
                {65, 69, 72}, Padrao.SILENCIO, Padrao.SILENCIO, {69, 72, 77},
                Padrao.SILENCIO, {65, 69, 72}, Padrao.SILENCIO, Padrao.SILENCIO,
                {58, 62, 65, 69}, Padrao.SILENCIO, Padrao.SILENCIO, {62, 65, 69},
                Padrao.SILENCIO, {58, 62, 65, 69}, Padrao.SILENCIO, Padrao.SILENCIO,
                {55, 59, 62}, Padrao.SILENCIO, Padrao.SILENCIO, {59, 62, 67},
                Padrao.SILENCIO, {55, 59, 62}, Padrao.SILENCIO, Padrao.SILENCIO
        });

        // Representacao instrumental da melodia vocal ("Save it till the morning after"),
        // simplificada para ficar reconhecivel sem usar audio externo.
        Padrao vocal = Padrao.melodico("melodia vocal instrumental", 80 /* Lead 1 square */, 78, 2, new int[][]{
                {74}, Padrao.SUSTENTA, {72}, {69}, {67}, Padrao.SUSTENTA, {69}, Padrao.SILENCIO,
                {74}, Padrao.SUSTENTA, {72}, {69}, {67}, Padrao.SUSTENTA, {65}, Padrao.SILENCIO,
                {71}, Padrao.SUSTENTA, {69}, {67}, {66}, Padrao.SUSTENTA, {69}, Padrao.SILENCIO,
                {72}, Padrao.SUSTENTA, {71}, {69}, {67}, Padrao.SUSTENTA, Padrao.SILENCIO, Padrao.SILENCIO
        });

        // Extras: arpejo brilhante de synth e uma cama de cordas para reforcar o clima noturno.
        Padrao arpejo = Padrao.dedilhado("arpejo de synth Dm-F-Bbmaj7-G", 81 /* Lead 2 sawtooth */, 58, 4, new int[][]{
                {62}, {65}, {69}, {74}, {69}, {65}, {62}, Padrao.SILENCIO,
                {65}, {69}, {72}, {77}, {72}, {69}, {65}, Padrao.SILENCIO,
                {58}, {62}, {65}, {69}, {65}, {62}, {58}, Padrao.SILENCIO,
                {55}, {59}, {62}, {67}, {62}, {59}, {55}, Padrao.SILENCIO
        });
        Padrao pad = Padrao.melodico("cordas sintetizadas em Re menor", 50 /* Synth Strings */, 54, 1, new int[][]{
                {50, 57, 65}, Padrao.SUSTENTA, Padrao.SUSTENTA, Padrao.SUSTENTA,
                {53, 60, 69}, Padrao.SUSTENTA, Padrao.SUSTENTA, Padrao.SUSTENTA,
                {46, 53, 62, 69}, Padrao.SUSTENTA, Padrao.SUSTENTA, Padrao.SUSTENTA,
                {43, 50, 59, 67}, Padrao.SUSTENTA, Padrao.SUSTENTA, Padrao.SUSTENTA
        });

        return new Musica("save", "Save a Prayer - Duran Duran (2009 Remaster)", "Re menor", 114,
                List.of(new Faixa("Bateria", "tum-tss", bateria),
                        new Faixa("Baixo", "dum-dum", baixo),
                        new Faixa("Synth", "aaaah", synth),
                        new Faixa("Piano", "plim", piano),
                        new Faixa("Vocal", "wiiii", vocal)),
                List.of(new Faixa("Arpejo", "piiim", arpejo),
                        new Faixa("Pad", "oooooh", pad)),
                new int[]{62, 65, 67, 69, 72} /* pentatonica de Re menor */);
    }

    // ==================================================================================
    // Catálogo
    // ==================================================================================

    /** Repertório completo da mesa, na ordem em que aparece no comando setlist. */
    public static List<Musica> catalogo() {
        return List.of(billieJean(), sevenNationArmy(), sweetDreams(), saveAPrayer());
    }

    /**
     * Procura uma música pelo apelido digitado pelo DJ, aceitando abreviações
     * (billie, seven, sweet, save) e trechos do título.
     */
    public static Musica porId(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            return null;
        }
        String chave = entrada.trim().toLowerCase();
        for (Musica m : catalogo()) {
            if (m.id.equals(chave)) {
                return m;
            }
        }
        for (Musica m : catalogo()) {
            if (m.id.startsWith(chave) || m.titulo.toLowerCase().contains(chave)) {
                return m;
            }
        }
        return null;
    }

    /**
     * Procura, no arranjo da musica atual, uma faixa prevista com esse nome (extras primeiro).
     * Devolve null se o DJ inventou um nome que nao faz parte do arranjo.
     */
    public Faixa faixaPorNome(String nome) {
        String chave = (nome == null ? "" : nome).trim().toLowerCase();
        for (Faixa f : extras) {
            if (f.nome().toLowerCase().equals(chave)) {
                return f;
            }
        }
        for (Faixa f : faixas) {
            if (f.nome().toLowerCase().equals(chave)) {
                return f;
            }
        }
        return null;
    }

    /**
     * Padrao usado por uma faixa inventada na hora pelo DJ: um improviso na tonalidade da
     * musica atual, para que qualquer faixa nova entre afinada com o resto do arranjo.
     */
    public Padrao padraoImproviso() {
        int[][] passos = new int[escalaParaImproviso.length * 2][];
        for (int i = 0; i < escalaParaImproviso.length; i++) {
            passos[i] = new int[]{escalaParaImproviso[i]};
            passos[passos.length - 1 - i] = new int[]{escalaParaImproviso[i]};
        }
        return Padrao.melodico("improviso em " + tonalidade, 4 /* Piano eletrico */, 72, 2, passos);
    }

    public String getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getTonalidade() {
        return tonalidade;
    }

    public int getBpm() {
        return bpm;
    }

    public List<Faixa> getFaixas() {
        return faixas;
    }

    public List<Faixa> getExtras() {
        return extras;
    }
}
