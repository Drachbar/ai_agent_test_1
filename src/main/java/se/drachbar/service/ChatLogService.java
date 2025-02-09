package se.drachbar.service;

import se.drachbar.chat.ChatLabelService;
import se.drachbar.model.AiMessageDto;
import se.drachbar.model.ConversationDto;
import se.drachbar.model.MessageDto;
import se.drachbar.model.UserMessageDto;
import se.drachbar.repository.ChatRepository;

import java.util.List;
import java.util.stream.Stream;

public class ChatLogService {
    private static final ChatLabelService chatLabelService = new ChatLabelService();

    public static void appendChatMessage(String query, String newMessage) {
        final ConversationDto conversation = ChatRepository.getChat();
        final List<MessageDto> oldMessages = conversation.messages();

        final UserMessageDto userMessageDto = new UserMessageDto(query);
        final AiMessageDto aiMessageDto = new AiMessageDto(newMessage, "GPT_4_O_MINI");
        final List<MessageDto> messages = List.of(userMessageDto, aiMessageDto);
        final List<MessageDto> combinedMessages = Stream.concat(oldMessages.stream(), messages.stream()).toList();

        ChatRepository.updateChat(combinedMessages);
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
