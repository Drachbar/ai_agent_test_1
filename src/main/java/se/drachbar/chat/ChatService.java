package se.drachbar.chat;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import se.drachbar.config.Config;
import se.drachbar.model.*;

import java.util.ArrayList;
import java.util.List;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

public class ChatService {

    private final StreamingChatLanguageModel chatModel;

    public ChatService() {
        this.chatModel = OpenAiStreamingChatModel.builder()
                .apiKey(Config.getOpenAiApiKey())
                .modelName(GPT_4_O_MINI)
                .build();
    }

    public void processQuery(String query, StreamingResponseHandler responseHandler, List<MessageDto> prevChats) {
        List<ChatMessage> previousMessages = prevChats.stream()
                .map(this::convertToChatMessage)
                .toList();

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage("You are a helpful AI agent, you answer short and concise."));
        messages.add(new SystemMessage("You are an expert in java and Angular"));
        messages.addAll(previousMessages);
        messages.add(new UserMessage(query));

        chatModel.chat(ChatRequest.builder().messages(messages).build(), responseHandler);

    }

    private ChatMessage convertToChatMessage(MessageDto message) {
        return switch (message) {
            case UserMessageDto(String text) -> new UserMessage(text);
            case AiMessageDto aiMessage -> new AiMessage(aiMessage.text());
            case SystemMessageDto systemMessage -> new SystemMessage(systemMessage.text());
            case ToolExecutionResultMessageDto toolExecutionResultMessage ->
                    new ToolExecutionResultMessage("", "", toolExecutionResultMessage.text());
        };
    }

}
