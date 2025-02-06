package se.drachbar.chat;

import com.sun.net.httpserver.HttpExchange;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

import java.io.BufferedWriter;
import java.io.IOException;

public class StreamingResponseHandler implements StreamingChatResponseHandler {
    private final BufferedWriter writer;
    private final HttpExchange exchange;

    public StreamingResponseHandler(BufferedWriter writer, HttpExchange exchange) {
        this.writer = writer;
        this.exchange = exchange;
    }

    @Override
    public void onPartialResponse(String s) {
        try {
            writer.write(s);
            writer.flush();
        } catch (IOException e) {
            handleError(e);
        }
    }

    @Override
    public void onCompleteResponse(ChatResponse chatResponse) {
        try {
            writer.newLine();
            writer.flush();
            writer.close();
            exchange.close();
        } catch (IOException e) {
            handleError(e);
        }
    }

    @Override
    public void onError(Throwable error) {
        handleError(error);
    }

    private void handleError(Throwable error) {
        error.printStackTrace();
        try {
            writer.write("\n[ERROR] " + error.getMessage());
            writer.flush();
            writer.close();
            exchange.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
