package se.drachbar;

import dev.langchain4j.model.openai.OpenAiChatModel;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

public class Main {
    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");

        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(GPT_4_O_MINI)
                .build();

        String answer = model.generate("Hi, whats up?");
        System.out.println(answer);
    }
}