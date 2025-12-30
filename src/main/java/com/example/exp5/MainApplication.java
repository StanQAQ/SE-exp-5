package com.example.exp5;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.example.exp5.repository.TransactionRepository;
import com.example.exp5.repository.AnalyticsService;
import com.example.exp5.models.PieData;
import com.example.exp5.models.TimeSeriesData;
import com.example.exp5.models.Transaction;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
import com.example.exp5.models.TransactionFilter;
 

public class MainApplication {
    private static final int PORT = 8080;
    private static final Logger logger = Logger.getLogger(MainApplication.class.getName());
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String UTF_8 = "UTF-8";

    public static void main(String[] args) throws Exception {
        TransactionRepository repo = new TransactionRepository();
        AnalyticsService analytics = new AnalyticsService(repo);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // 提供根路径和页面服务
        server.createContext("/", new ResourceHandler("index.html"));
        server.createContext("/add.html", new ResourceHandler("add.html"));
        server.createContext("/query.html", new ResourceHandler("query.html"));
        server.createContext("/charts.html", new ResourceHandler("charts.html"));

        // API 端点
        server.createContext("/api/transactions", new TransactionsHandler(repo));
        server.createContext("/api/analytics/pie", new PieHandler(analytics));
        server.createContext("/api/analytics/series", new SeriesHandler(analytics));

        server.setExecutor(null);
        server.start();
        logger.info(() -> "Server started at http://localhost:" + PORT + "/ (opening /index.html)");
        openBrowser("http://localhost:" + PORT + "/index.html");
    }

    private static void openBrowser(String url) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                logger.log(Level.SEVERE, "Failed to open browser: {0}", e.getMessage());
            }
        } else {
            logger.log(Level.SEVERE, "Desktop not supported. Please open this URL manually: {0}", url);
        }
    }

    // 用于从类路径资源提供单个文件的处理器
    static class ResourceHandler implements HttpHandler {
        private final String resourcePath;

        ResourceHandler(String path) {
            this.resourcePath = path.startsWith("/") ? path : "/" + path;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 尝试从类路径加载
            InputStream is = MainApplication.class.getResourceAsStream(resourcePath);
            if (is == null) {
                String notFound = "404 - file not found";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes());
                }
                return;
            }

            Headers h = exchange.getResponseHeaders();
            // 基于扩展名的简单 MIME 类型猜测
            String mime = "application/octet-stream";
            if (resourcePath.endsWith(".html")) mime = "text/html; charset=utf-8";
            else if (resourcePath.endsWith(".css")) mime = "text/css";
            else if (resourcePath.endsWith(".js")) mime = "application/javascript";

            h.set(CONTENT_TYPE, mime);

            // 读取所有字节
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] bytes = buffer.toByteArray();
            is.close();

            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // 静态目录处理器（提供文件夹下的文件服务）
    static class StaticHandler implements HttpHandler {
        private final File baseDir;

        StaticHandler(File baseDir) {
            this.baseDir = baseDir;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uri = exchange.getRequestURI().getPath();
            String rel = uri.replaceFirst("/static/?", "");
            File f = new File(baseDir, rel);
            if (!f.exists() || f.isDirectory()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            String mime = URLConnection.guessContentTypeFromName(f.getName());
            if (mime == null) mime = "application/octet-stream";
            exchange.getResponseHeaders().set(CONTENT_TYPE, mime);
            byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // /api/transactions (GET -> 列表, POST -> 添加表单数据)
    static class TransactionsHandler implements HttpHandler {
        private final TransactionRepository repo;

        TransactionsHandler(TransactionRepository repo) {
            this.repo = repo;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ("GET".equalsIgnoreCase(method)) {
                handleGet(exchange);
            } else if ("POST".equalsIgnoreCase(method)) {
                handlePost(exchange);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                handleDelete(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handleGet(HttpExchange exchange) throws IOException {
            List<Transaction> all = repo.listTransactions();
            String query = exchange.getRequestURI().getQuery();
            if (query != null && !query.isEmpty()) {
                all = filterTransactions(all, query);
            }
            String json = "[" + all.stream().map(Transaction::toJson).collect(Collectors.joining(",")) + "]";
            sendJsonResponse(exchange, json, 200);
        }

        private List<Transaction> filterTransactions(List<Transaction> all, String query) {
            try {
                Map<String, String> q = parseForm(query);
                TransactionFilter filter = new TransactionFilter();
                if (q.containsKey("start")) {
                    filter.setStartDate(LocalDate.parse(q.get("start")));
                }
                if (q.containsKey("end")) {
                    filter.setEndDate(LocalDate.parse(q.get("end")));
                }
                if (q.containsKey("type") && !q.get("type").isEmpty()) {
                    filter.setType(q.get("type"));
                }
                if (q.containsKey("min") && !q.get("min").isEmpty()) {
                    try {
                        filter.setMinAmount(Double.parseDouble(q.get("min")));
                    } catch (NumberFormatException ignored) {
                        logger.log(Level.WARNING, "Invalid min amount: {0}", q.get("min"));
                    }
                }
                if (q.containsKey("max") && !q.get("max").isEmpty()) {
                    try {
                        filter.setMaxAmount(Double.parseDouble(q.get("max")));
                    } catch (NumberFormatException ignored) {
                        logger.log(Level.WARNING, "Invalid max amount: {0}", q.get("max"));
                    }
                }
                return all.stream().filter(filter::matches).collect(Collectors.toList());
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error filtering transactions", ex);
                return all;
            }
        }

        private void handlePost(HttpExchange exchange) throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            int nRead;
            InputStream is = exchange.getRequestBody();
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            String body = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            Map<String, String> params = parseForm(body);
            Transaction t = Transaction.fromMap(params);
            repo.addTransaction(t);
            sendJsonResponse(exchange, "{\"status\":\"ok\"}", 200);
        }

        private void handleDelete(HttpExchange exchange) throws IOException {
            String q = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseForm(q == null ? "" : q);
            String id = params.get("id");
            if (id == null || id.isEmpty()) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }
            boolean ok = repo.deleteTransaction(id);
            String respStr = ok ? "{\"status\":\"ok\"}" : "{\"status\":\"not_found\"}";
            sendJsonResponse(exchange, respStr, ok ? 200 : 404);
        }

        private void sendJsonResponse(HttpExchange exchange, String json, int statusCode) throws IOException {
            byte[] resp = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set(CONTENT_TYPE, JSON_CONTENT_TYPE);
            exchange.sendResponseHeaders(statusCode, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        }
    }

    static Map<String, String> parseForm(String body) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.isEmpty()) return map;
        String[] pairs = body.split("&");
        for (String p : pairs) {
            int idx = p.indexOf('=');
            if (idx >= 0) {
                String k = URLDecoder.decode(p.substring(0, idx), UTF_8);
                String v = URLDecoder.decode(p.substring(idx + 1), UTF_8);
                map.put(k, v);
            }
        }
        return map;
    }

    static class PieHandler implements HttpHandler {
        private final AnalyticsService analytics;

        PieHandler(AnalyticsService a) {
            this.analytics = a;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<PieData> pie = analytics.pieByCategory();
            String json = "[" + pie.stream().map(PieData::toJson).collect(Collectors.joining(",")) + "]";
            byte[] resp = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set(CONTENT_TYPE, JSON_CONTENT_TYPE);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        }
    }

    static class SeriesHandler implements HttpHandler {
        private final AnalyticsService analytics;

        SeriesHandler(AnalyticsService a) {
            this.analytics = a;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<TimeSeriesData> series = analytics.timeSeriesMonthly();
            String json = "[" + series.stream().map(TimeSeriesData::toJson).collect(Collectors.joining(",")) + "]";
            byte[] resp = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set(CONTENT_TYPE, JSON_CONTENT_TYPE);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        }
    }
}
