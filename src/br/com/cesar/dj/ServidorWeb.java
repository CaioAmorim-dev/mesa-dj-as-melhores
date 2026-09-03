package br.com.cesar.dj;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servidor HTTP local que expõe o {@link Mixer} para a interface HTML.
 * Usa somente classes do JDK, mantendo o projeto sem dependências externas.
 */
public final class ServidorWeb {

    private final Mixer mixer;
    private final int porta;
    private final Path diretorioWeb;
    private final Object lockComandos = new Object();
    private final CountDownLatch encerrado = new CountDownLatch(1);
    private final AtomicBoolean fechando = new AtomicBoolean(false);
    private HttpServer servidor;

    public ServidorWeb(Mixer mixer, int porta) {
        this.mixer = mixer;
        this.porta = porta;
        this.diretorioWeb = Path.of("web").toAbsolutePath().normalize();
    }

    public void iniciar() {
        try {
            if (!Files.isDirectory(diretorioWeb)) {
                throw new IllegalStateException("Diretório da interface não encontrado: " + diretorioWeb);
            }

            servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0);
            servidor.createContext("/api/", this::tratarApi);
            servidor.createContext("/", this::servirArquivo);
            servidor.setExecutor(Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "HTTP-Mesa-DJ");
                thread.setDaemon(true);
                return thread;
            }));
            servidor.start();

            Console.logSistema("Interface disponível em http://localhost:" + porta);
            Console.logSistema("Pressione Ctrl+C ou use o botão Desligar mesa para encerrar.");
        } catch (IOException e) {
            mixer.pararTudoEFinalizar();
            throw new IllegalStateException("Não foi possível iniciar o servidor web na porta " + porta + ".", e);
        }
    }

    public void aguardarEncerramento() {
        try {
            encerrado.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fechar();
        }
    }

    public void fechar() {
        if (!fechando.compareAndSet(false, true)) {
            return;
        }
        mixer.pararTudoEFinalizar();
        if (servidor != null) {
            servidor.stop(0);
        }
        encerrado.countDown();
        Console.logSistema("Interface web e threads da mesa encerradas.");
    }

    private void tratarApi(HttpExchange exchange) throws IOException {
        try {
            String metodo = exchange.getRequestMethod();
            String caminho = exchange.getRequestURI().getPath();

            if ("GET".equals(metodo) && "/api/state".equals(caminho)) {
                responderJson(exchange, 200, estadoJson());
                return;
            }
            if (!"POST".equals(metodo)) {
                responderErro(exchange, 405, "Método não permitido.");
                return;
            }

            Map<String, String> parametros = parametros(exchange.getRequestURI().getRawQuery());
            synchronized (lockComandos) {
                executarComando(caminho, parametros);
            }
            responderJson(exchange, 200, estadoJson());
        } catch (IllegalArgumentException e) {
            responderErro(exchange, 400, e.getMessage());
        } catch (Exception e) {
            Console.logErro("Falha na API web: " + e.getMessage());
            responderErro(exchange, 500, "Não foi possível executar o comando.");
        }
    }

    private void executarComando(String caminho, Map<String, String> parametros) {
        switch (caminho) {
            case "/api/music" -> {
                String id = obrigatorio(parametros, "id");
                Musica musica = Musica.porId(id);
                if (musica == null) {
                    throw new IllegalArgumentException("Música não encontrada: " + id);
                }
                mixer.carregarMusica(musica);
            }
            case "/api/track/play" -> mixer.play(obrigatorio(parametros, "name"));
            case "/api/track/pause" -> mixer.pause(obrigatorio(parametros, "name"));
            case "/api/track/stop" -> mixer.stop(obrigatorio(parametros, "name"));
            case "/api/track/solo" -> mixer.solo(obrigatorio(parametros, "name"));
            case "/api/all/play" -> mixer.tocarTodas();
            case "/api/all/pause" -> mixer.pausarTodas();
            case "/api/sync" -> mixer.sincronizar();
            case "/api/bpm" -> {
                String alvo = obrigatorio(parametros, "target");
                int bpm = inteiroPositivo(parametros, "value");
                if ("all".equalsIgnoreCase(alvo)) {
                    mixer.alterarBpmDeTodas(bpm);
                } else {
                    mixer.alterarBpm(alvo, bpm);
                }
            }
            case "/api/track/add" -> {
                String nome = obrigatorio(parametros, "name");
                String som = parametros.getOrDefault("sound", "tshhh");
                int bpm = parametros.containsKey("bpm")
                        ? inteiroPositivo(parametros, "bpm")
                        : mixer.getMusicaAtual().getBpm();
                mixer.adicionarFaixaExtra(nome, som, bpm);
            }
            case "/api/shutdown" -> {
                Thread thread = new Thread(this::fechar, "Encerramento-Web");
                thread.setDaemon(true);
                thread.start();
            }
            default -> throw new IllegalArgumentException("Comando desconhecido.");
        }
    }

    private String estadoJson() {
        Musica atual = mixer.getMusicaAtual();
        List<Instrumento> instrumentos = new ArrayList<>(mixer.getFaixas().values());
        instrumentos.sort(Comparator.comparing(Instrumento::getNome, String.CASE_INSENSITIVE_ORDER));

        StringBuilder json = new StringBuilder("{");
        if (atual == null) {
            json.append("\"currentMusic\":null,");
        } else {
            json.append("\"currentMusic\":{")
                    .append("\"id\":").append(json(atual.getId())).append(',')
                    .append("\"title\":").append(json(atual.getTitulo())).append(',')
                    .append("\"key\":").append(json(atual.getTonalidade())).append(',')
                    .append("\"bpm\":").append(atual.getBpm()).append("},");
        }

        json.append("\"catalog\":[");
        boolean primeiro = true;
        for (Musica musica : Musica.catalogo()) {
            if (!primeiro) json.append(',');
            primeiro = false;
            json.append('{')
                    .append("\"id\":").append(json(musica.getId())).append(',')
                    .append("\"title\":").append(json(musica.getTitulo())).append(',')
                    .append("\"bpm\":").append(musica.getBpm()).append('}');
        }
        json.append("],\"tracks\":[");

        primeiro = true;
        for (Instrumento instrumento : instrumentos) {
            if (!primeiro) json.append(',');
            primeiro = false;
            json.append('{')
                    .append("\"name\":").append(json(instrumento.getNome())).append(',')
                    .append("\"sound\":").append(json(instrumento.getSom())).append(',')
                    .append("\"state\":").append(json(instrumento.getEstado().name())).append(',')
                    .append("\"bpm\":").append(instrumento.getBpm()).append(',')
                    .append("\"grid\":").append(json(instrumento.getPadrao().getGrade())).append(',')
                    .append("\"stepMs\":").append(instrumento.calcularIntervaloMs()).append(',')
                    .append("\"pattern\":").append(json(instrumento.getPadrao().getDescricao()))
                    .append('}');
        }
        json.append("],\"extras\":[");

        primeiro = true;
        if (atual != null) {
            for (Musica.Faixa faixa : atual.getExtras()) {
                if (!mixer.getFaixas().containsKey(faixa.nome().toLowerCase())) {
                    if (!primeiro) json.append(',');
                    primeiro = false;
                    json.append(json(faixa.nome()));
                }
            }
        }
        return json.append("]}").toString();
    }

    private void servirArquivo(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            responderTexto(exchange, 405, "Método não permitido.", "text/plain; charset=utf-8");
            return;
        }

        String requisitado = exchange.getRequestURI().getPath();
        if ("/".equals(requisitado)) {
            requisitado = "/index.html";
        }
        Path arquivo = diretorioWeb.resolve(requisitado.substring(1)).normalize();
        if (!arquivo.startsWith(diretorioWeb) || !Files.isRegularFile(arquivo)) {
            responderTexto(exchange, 404, "Arquivo não encontrado.", "text/plain; charset=utf-8");
            return;
        }

        String tipo = switch (extensao(arquivo)) {
            case "html" -> "text/html; charset=utf-8";
            case "css" -> "text/css; charset=utf-8";
            case "js" -> "text/javascript; charset=utf-8";
            case "svg" -> "image/svg+xml";
            default -> "application/octet-stream";
        };
        byte[] conteudo = Files.readAllBytes(arquivo);
        exchange.getResponseHeaders().set("Content-Type", tipo);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, conteudo.length);
        try (OutputStream saida = exchange.getResponseBody()) {
            saida.write(conteudo);
        }
    }

    private static Map<String, String> parametros(String query) {
        Map<String, String> resultado = new HashMap<>();
        if (query == null || query.isBlank()) return resultado;
        for (String item : query.split("&")) {
            String[] partes = item.split("=", 2);
            String chave = URLDecoder.decode(partes[0], StandardCharsets.UTF_8);
            String valor = partes.length > 1 ? URLDecoder.decode(partes[1], StandardCharsets.UTF_8) : "";
            resultado.put(chave, valor);
        }
        return resultado;
    }

    private static String obrigatorio(Map<String, String> parametros, String nome) {
        String valor = parametros.get(nome);
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Parâmetro obrigatório ausente: " + nome);
        }
        return valor.trim();
    }

    private static int inteiroPositivo(Map<String, String> parametros, String nome) {
        try {
            int valor = Integer.parseInt(obrigatorio(parametros, nome));
            if (valor <= 0 || valor > 400) throw new NumberFormatException();
            return valor;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O BPM deve ser um número entre 1 e 400.");
        }
    }

    private static String json(String valor) {
        String escapado = valor.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escapado + "\"";
    }

    private static String extensao(Path arquivo) {
        String nome = arquivo.getFileName().toString();
        int ponto = nome.lastIndexOf('.');
        return ponto < 0 ? "" : nome.substring(ponto + 1).toLowerCase();
    }

    private static void responderJson(HttpExchange exchange, int status, String json) throws IOException {
        responderTexto(exchange, status, json, "application/json; charset=utf-8");
    }

    private static void responderErro(HttpExchange exchange, int status, String mensagem) throws IOException {
        responderJson(exchange, status, "{\"error\":" + json(mensagem) + "}");
    }

    private static void responderTexto(HttpExchange exchange, int status, String texto, String tipo) throws IOException {
        byte[] conteudo = texto.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", tipo);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, conteudo.length);
        try (OutputStream saida = exchange.getResponseBody()) {
            saida.write(conteudo);
        }
    }
}
