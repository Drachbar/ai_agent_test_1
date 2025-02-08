package se.drachbar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.drachbar.repository.ChatRepository;

public class ChatLogService {
    private static final Logger log = LoggerFactory.getLogger(ChatLogService.class);

    public static void appendChatMessage(String newMessage) {
        // Hämta det befintliga chat-meddelandet
        String existingChat = ChatRepository.getChat();

        // Konkatenation av nytt och gammalt meddelande
        String updatedChat = existingChat.isEmpty() ? newMessage : existingChat + "\n" + newMessage;

        // Uppdatera databasen med det nya meddelandet
        ChatRepository.updateChat(updatedChat);

        log.info("New chat message appended: {}", newMessage);
    }

}
