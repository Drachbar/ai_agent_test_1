package se.drachbar.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import se.drachbar.config.Config;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

public class ChatLabelService {

    private final ChatLanguageModel chatLanguageModel;

    public ChatLabelService() {
        this.chatLanguageModel = OpenAiChatModel.builder()
                .apiKey(Config.getOpenAiApiKey())
                .modelName(GPT_4_O_MINI)
                .build();
    }

    public String processQuery(String query) {
        Response<AiMessage> answer = chatLanguageModel.generate(
                new SystemMessage("""
                        You are a helpful AI agent, you are a part a part of an Ai-chain.
                        Your only job is to look at the question and give the question a label so that the chat can be recognised.
                        The label must be short and cannot be longer than 50 characters.
                        """),
                new UserMessage(query));
        System.out.println("Label klar!");
        return answer.content().text();
    }

}
