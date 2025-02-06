package se.drachbar.server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import se.drachbar.chat.ChatService;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ChatHandler implements HttpHandler {

    private final ChatService chatService;

    public ChatHandler() {
        this.chatService = new ChatService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            return;
        }

        String query = readRequestBody(exchange);
        if (query.isEmpty()) {
            sendErrorResponse(exchange, "Tom fråga!", 400);
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(200, 0);

        OutputStream outputStream = exchange.getResponseBody();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

        chatService.processQuery(query, writer, exchange);
    }

    private String readRequestBody(HttpExchange exchange) {
        InputStream requestBody = exchange.getRequestBody();
        return new BufferedReader(new InputStreamReader(requestBody, StandardCharsets.UTF_8))
                .lines()
                .reduce("", (acc, line) -> acc + line);
    }

    private void sendErrorResponse(HttpExchange exchange, String message, int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, message.length());
        exchange.getResponseBody().write(message.getBytes(StandardCharsets.UTF_8));
        exchange.close();
    }
}
