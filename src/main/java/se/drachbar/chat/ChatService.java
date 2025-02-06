package se.drachbar.chat;

import com.sun.net.httpserver.HttpExchange;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import se.drachbar.config.Config;

import java.io.BufferedWriter;
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

    public void processQuery(String query, BufferedWriter writer, HttpExchange exchange) {
        chatModel.chat(
                ChatRequest.builder()
                        .messages(List.of(
//                                new SystemMessage("You are a helpful AI agent, you answer short and concise."),
//                                new SystemMessage("You are an expert in java."),
                                new SystemMessage("""
                                        You are an advanced task-planning AI agent. You are a part of a AI-agent-chain.
                                        Your job is to analyze a user's question,
                                        break it down into smaller, actionable sub-tasks, and return the response strictly in JSON format.
                                        
                                        Instructions:
                                        1. Identify the main goal of the user's request.
                                        2. Decompose the task into smaller, logical sub-tasks.
                                        3. Ensure that each sub-task is clearly defined, follows a logical sequence, and can be executed independently.
                                        4. Respond strictly in JSON format without any additional text.
                                        
                                        JSON Response Format:
                                        {
                                          "main_goal": "The overall goal of the user's request",
                                          "sub_tasks": [
                                            {
                                              "id": 1,
                                              "description": "A clear, concise description of the sub-task",
                                              "dependencies": ["List of IDs of tasks that must be completed first, or empty array"]
                                            }
                                          ]
                                        }
                                        
                                        Only return JSON. Do not include any explanations or formatting outside the JSON structure.
                                        """),
                                new UserMessage(query)
                        ))
                        .build(),
                new StreamingResponseHandler(writer, exchange)
        );

    }

}
