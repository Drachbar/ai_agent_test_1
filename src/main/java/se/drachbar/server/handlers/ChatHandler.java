package se.drachbar.server.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.drachbar.chat.ChatService;
import se.drachbar.chat.StreamingResponseHandler;
import se.drachbar.model.ChatRequestDto;
import se.drachbar.model.MessageDto;
import se.drachbar.repository.ChatRepository;
import se.drachbar.service.ChatLogService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ChatHandler implements HttpHandler {
    private final ChatService chatService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(ChatRepository.class);

    public ChatHandler() {
        this.chatService = new ChatService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            return;
        }

        final ChatRequestDto chatRequest = readRequestBody(exchange);
        if (chatRequest == null || chatRequest.query() == null || chatRequest.query().isEmpty()) {
            sendErrorResponse(exchange, "Tom fråga eller ogiltig JSON!", 400);
            return;
        }

        final String query = chatRequest.query();
        final int id = chatRequest.id();

        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(200, 0);

        OutputStream outputStream = exchange.getResponseBody();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

        StreamingResponseHandler responseHandler = new StreamingResponseHandler(writer, exchange, (fullResponse) -> {
            ChatLogService.appendChatMessage(query, fullResponse);
            ChatLogService.updateLabelIfMissing(query, fullResponse);
        });
        List<MessageDto> prevChats = ChatLogService.getChatMessages(id);

        chatService.processQuery(query, responseHandler, prevChats);

    }

    private ChatRequestDto readRequestBody(HttpExchange exchange) {
        try (InputStream requestBody = exchange.getRequestBody()) {
            return objectMapper.readValue(requestBody, ChatRequestDto.class);
        } catch (IOException e) {
            log.error("Fel vid inläsning av data: ", e);
            return null; // Returnerar null om JSON inte kunde tolkas
        }
    }

    private void sendErrorResponse(HttpExchange exchange, String message, int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, message.length());
        exchange.getResponseBody().write(message.getBytes(StandardCharsets.UTF_8));
        exchange.close();
    }
}
