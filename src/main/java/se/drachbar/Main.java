package se.drachbar;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

public class Main {
    private static final int PORT = 8080;
    private static final String BASE_DIR = "public";

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api", new ApiHandler());
        server.createContext("/api/chat", new ChatHandler());
        server.setExecutor(null); // Standard thread-pool
        server.start();
        System.out.println("Server startad på http://localhost:" + PORT);
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestedPath = exchange.getRequestURI().getPath();
            if (requestedPath.equals("/")) {
                requestedPath = "/index.html"; // Standardfil om ingen specifik fil anges
            }

            File file = new File(BASE_DIR + requestedPath);
            if (!file.exists() || file.isDirectory()) {
                send404(exchange);
                return;
            }

            // Sätt korrekt MIME-typ
            String mimeType = getMimeType(requestedPath);
            exchange.getResponseHeaders().set("Content-Type", mimeType);

            // Läs och skicka filen
            byte[] fileBytes = Files.readAllBytes(Paths.get(file.getPath()));
            exchange.sendResponseHeaders(200, fileBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(fileBytes);
            os.close();
        }

        private void send404(HttpExchange exchange) throws IOException {
            String response = "404 - Not Found";
            exchange.sendResponseHeaders(404, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        private String getMimeType(String filePath) {
            if (filePath.endsWith(".html")) return "text/html";
            if (filePath.endsWith(".css")) return "text/css";
            if (filePath.endsWith(".js")) return "application/javascript";
            if (filePath.endsWith(".png")) return "image/png";
            if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) return "image/jpeg";
            if (filePath.endsWith(".gif")) return "image/gif";
            if (filePath.endsWith(".svg")) return "image/svg+xml";
            return "application/octet-stream"; // Standard MIME-typ för okända filer
        }
    }

    static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = "{ \"message\": \"API fungerar!\" }";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());

                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes(StandardCharsets.UTF_8));
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }
    }

    static class ChatHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
                return;
            }

            // Läs in frågan från request-body
            InputStream requestBody = exchange.getRequestBody();
            String query = new BufferedReader(new InputStreamReader(requestBody, StandardCharsets.UTF_8))
                    .lines()
                    .reduce("", (acc, line) -> acc + line);

            if (query.isEmpty()) {
                String response = "Tom fråga!";
                exchange.sendResponseHeaders(400, response.length());
                exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
                exchange.close();
                return;
            }

            // Skicka headers innan vi börjar strömma svaret
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, 0);

            // Öppna output-stream för att skicka data direkt
            OutputStream outputStream = exchange.getResponseBody();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

            // Hämta API-nyckeln
            String apiKey = System.getenv("OPENAI_API_KEY");

            // Skapa OpenAI-strömningsmodell
            StreamingChatLanguageModel chatModel = OpenAiStreamingChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(GPT_4_O_MINI)
                    .build();

            // Skicka fråga till OpenAI och strömma svaret
            chatModel.chat(ChatRequest.builder()
                    .messages(List.of(new UserMessage(query)))
                    .build(), new StreamingChatResponseHandler() {

                @Override
                public void onPartialResponse(String s) {
                    try {
                        writer.write(s);
                        writer.flush(); // Viktigt! Skicka ut datan direkt
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onCompleteResponse(ChatResponse chatResponse) {
                    try {
                        writer.newLine();
                        writer.flush();
                        writer.close();
                        exchange.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(Throwable error) {
                    error.printStackTrace();
                    try {
                        writer.write("\n[ERROR] " + error.getMessage());
                        writer.flush();
                        writer.close();
                        exchange.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}