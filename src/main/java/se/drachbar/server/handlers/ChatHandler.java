package se.drachbar.server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.drachbar.chat.ChatLabelService;
import se.drachbar.chat.ChatService;
import se.drachbar.chat.StreamingResponseHandler;
import se.drachbar.repository.ChatRepository;
import se.drachbar.service.ChatLogService;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ChatHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatHandler.class);
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
            ChatLogService.appendChatMessage(fullResponse);
            if (!labelExistsInLogFile()) {
                final String label = chatLabelService.processQuery(query, fullResponse);
                prependLabelInLogfile(label);
            }
        });

        chatService.processQuery(query, responseHandler);

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
            log.error("Fel vid läsning av fil: ", e);
        }
        return false;
    }

    private void prependLabelInLogfile(String label) {
        ChatRepository.insertLabel(label);

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
            log.error("Fel vid skapandet av etikett: ", e);
        }
    }
}
