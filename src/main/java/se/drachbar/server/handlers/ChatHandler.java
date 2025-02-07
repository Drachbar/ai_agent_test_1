package se.drachbar.server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import se.drachbar.chat.ChatLabelService;
import se.drachbar.chat.ChatService;
import se.drachbar.chat.StreamingResponseHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ChatHandler implements HttpHandler {

    private final ChatService chatService;
    private final ChatLabelService chatLabelService;

    public ChatHandler() {
        this.chatService = new ChatService();
        this.chatLabelService = new ChatLabelService();
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



        StreamingResponseHandler responseHandler = new StreamingResponseHandler(writer, exchange, (fullResponse) -> {
            logQueryAndResponse(query, fullResponse);
            System.out.println("chatService är klar?");
        });

        chatService.processQuery(query, responseHandler);

        if (!labelExistsInLogFile()) {
            final String label = chatLabelService.processQuery(query);
            prependLabelInLogfile(label);
        }
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

    private void logQueryAndResponse(String query, String response) {
        String logFilePath = "chat_log.txt";
        try (BufferedWriter logWriter = new BufferedWriter(new FileWriter(logFilePath, true))) {
            logWriter.write("Fråga: " + query + "\n");
            logWriter.write("Svar: " + response + "\n");
            logWriter.write("-------------------------------------------------\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean labelExistsInLogFile() {
        String logFilePath = "chat_log.txt";
        File logFile = new File(logFilePath);

        if (!logFile.exists()) {
            return false; // Filen finns inte, etikett kan läggas till
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String firstLine = reader.readLine();
            return firstLine != null && firstLine.startsWith("Etikett: ");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void prependLabelInLogfile(String label) {
        String logFilePath = "chat_log.txt";
        File logFile = new File(logFilePath);

        try {
            // Läs in befintligt innehåll
            StringBuilder fileContent = new StringBuilder();
            if (logFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        fileContent.append(line).append("\n");
                    }
                }
            }

            // Skapa nytt innehåll med etiketten först
            String newContent = "Etikett: " + label + "\n-------------------------------------------------\n" + fileContent;

            // Skriv tillbaka hela filen
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, false))) {
                writer.write(newContent);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
