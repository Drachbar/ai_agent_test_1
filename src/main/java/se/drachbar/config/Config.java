package se.drachbar.config;

public class Config {
    public static String getOpenAiApiKey() {
        return System.getenv("OPENAI_API_KEY");
    }
}
