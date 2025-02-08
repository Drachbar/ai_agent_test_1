package se.drachbar.service;

import se.drachbar.chat.ChatLabelService;
import se.drachbar.repository.ChatRepository;

public class ChatLogService {
    private static final ChatLabelService chatLabelService = new ChatLabelService();

    public static void appendChatMessage(String newMessage) {
        // Hämta det befintliga chat-meddelandet
        String existingChat = ChatRepository.getChat();

        // Konkatenation av nytt och gammalt meddelande
        String updatedChat = existingChat.isEmpty() ? newMessage : existingChat + "\n" + newMessage;

        // Uppdatera databasen med det nya meddelandet
        ChatRepository.updateChat(updatedChat);
    }

    public static void updateLabelIfMissing(String query, String fullResponse) {
        String existingLabel = ChatRepository.getLabel();

        // Om en label redan finns, gör ingenting
        if (existingLabel != null && !existingLabel.isBlank()) {
            return;
        }

        // Generera en ny label om ingen finns
        String newLabel = chatLabelService.processQuery(query, fullResponse);
        ChatRepository.insertLabel(newLabel);
    }

}
