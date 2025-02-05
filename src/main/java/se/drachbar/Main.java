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
import java.util.List;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

public class Main {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/chat", new ChatHandler());
        server.setExecutor(null); // Standard thread-pool
        server.start();
        System.out.println("Server startad på http://localhost:8080/chat");
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