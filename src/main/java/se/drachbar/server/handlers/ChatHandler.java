package se.drachbar.server.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.drachbar.chat.ChatService;
import se.drachbar.chat.StreamingResponseHandler;
import se.drachbar.model.ChatDto;
import se.drachbar.model.ChatListDto;
import se.drachbar.model.ChatRequestDto;
import se.drachbar.model.MessageDto;
import se.drachbar.repository.ChatRepository;
import se.drachbar.service.ChatLogService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChatHandler implements HttpHandler {
    private final ChatService chatService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(ChatRepository.class);

    public ChatHandler() {
        this.chatService = new ChatService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();
        String requestPath = exchange.getRequestURI().getPath();

        if ("GET".equalsIgnoreCase(requestMethod) && requestPath.equals("/api/chat/get-all")) {
            handleGetAllChats(exchange);
            return;
        }

        if ("GET".equalsIgnoreCase(requestMethod) && requestPath.contains("/api/chat/get-chat")) {
            handleGetChatById(exchange);
            return;
        }

        if ("POST".equalsIgnoreCase(requestMethod) && requestPath.equals("/api/chat/new-conversation")) {
            handleCreateNewConversation(exchange);
            return;
        }

        if ("POST".equalsIgnoreCase(requestMethod) && requestPath.equals("/api/chat")) {
            handleChatRequest(exchange);
            return;
        }

        if ("DELETE".equalsIgnoreCase(requestMethod) && requestPath.contains("/api/chat/delete-chat")) {
            handleDeleteChat(exchange);
            return;
        }

        exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
    }

    private void handleDeleteChat(HttpExchange exchange) throws IOException {
        final GetChatByIdRequestDto request = readQueryParameterChatById(exchange);
        if (request == null) {
            sendErrorResponse(exchange, "Tom fråga eller ogiltig JSON!", 400);
            return;
        }

        final int id = request.id();

        final boolean chatDeleted = ChatRepository.deleteChat(id);
        if (chatDeleted) {
            sendJsonResponse(exchange, chatDeleted, 200);
        } else {
            sendErrorResponse(exchange, "Chatten finns inte.", 404);
        }
    }

    private void handleChatRequest(HttpExchange exchange) throws IOException {
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
            ChatLogService.appendChatMessage(query, fullResponse, id);
            ChatLogService.updateLabelIfMissing(query, fullResponse, id);
        });
        List<MessageDto> prevChats = ChatLogService.getChatMessages(id);

        chatService.processQuery(query, responseHandler, prevChats);
    }


    private void handleGetChatById(final HttpExchange exchange) throws IOException {
        final GetChatByIdRequestDto request = readQueryParameterChatById(exchange);
        if (request == null) {
            sendErrorResponse(exchange, "Tom fråga eller ogiltig JSON!", 400);
            return;
        }

        final int id = request.id();

        final ChatDto chat = ChatRepository.getChat2(id);
        sendJsonResponse(exchange, chat, 200);
    }

    private void handleGetAllChats(final HttpExchange exchange) throws IOException {
        final ChatListDto chatListDto = ChatRepository.getAllChats();
        sendJsonResponse(exchange, chatListDto, 200);
    }

    private void handleCreateNewConversation(final HttpExchange exchange) throws IOException {
        final int conversationId = ChatRepository.createConversation();
        sendJsonResponse(exchange, conversationId, 201);
    }

    private <T> void sendJsonResponse(final HttpExchange exchange, T responseObject, int statusCode) throws IOException {
        final String responseJson = objectMapper.writeValueAsString(responseObject);
        byte[] responseBytes = responseJson.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private GetChatByIdRequestDto readQueryParameterChatById(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();

        if (query == null || query.isEmpty()) {
            return null; // Ingen query-sträng, returnera null eller hantera felet
        }

        Map<String, String> queryParams = parseQueryParams(query);
        String idParam = queryParams.get("id");

        if (idParam == null) {
            sendErrorResponse(exchange, "id saknas", 400);
            return null; // Om id saknas, returnera null eller kasta ett exception
        }

        try {
            int id = Integer.parseInt(idParam);
            return new GetChatByIdRequestDto(id);
        } catch (NumberFormatException e) {
            log.error("Fel vid parsing av id: ", e);
            return null;
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        return Arrays.stream(query.split("&"))
                .map(param -> param.split("="))
                .filter(pair -> pair.length == 2)
                .collect(Collectors.toMap(pair -> pair[0], pair -> pair[1]));
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

    private record GetChatByIdRequestDto(int id) {}
}
