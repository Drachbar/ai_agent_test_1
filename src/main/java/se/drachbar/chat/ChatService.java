package se.drachbar.chat;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import se.drachbar.config.Config;

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

    public void processQuery(String query, StreamingResponseHandler responseHandler) {
        System.out.println("Chatservice startar");

        chatModel.chat(
                ChatRequest.builder()
                        .messages(List.of(
                                new SystemMessage("You are a helpful AI agent, you answer short and concise."),
                                new SystemMessage("You are an expert in java and Angular"),
                                new UserMessage(query)
                        ))
                        .build(), responseHandler);

    }

}
