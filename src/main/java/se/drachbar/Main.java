package se.drachbar;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.List;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

public class Main {
    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");

        StreamingChatLanguageModel chatModel = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(GPT_4_O_MINI)
                .build();

        chatModel.chat(ChatRequest.builder()
                .messages(
                        List.of(new UserMessage("Tell me a joke about Java"))
                ).build(), new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String s) {
                System.out.print(s);
            }

            @Override
            public void onCompleteResponse(ChatResponse chatResponse) {
                System.out.println();
                System.out.println(chatResponse.aiMessage().text());
            }

            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
            }
        });
    }
}