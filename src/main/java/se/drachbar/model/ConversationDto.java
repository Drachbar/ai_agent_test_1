package se.drachbar.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ConversationDto(List<MessageDto> messages) {
    @JsonCreator
    public ConversationDto(@JsonProperty("messages") List<MessageDto> messages) {
        this.messages = messages != null ? messages : List.of();
    }
}
