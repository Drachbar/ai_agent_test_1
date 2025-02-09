package se.drachbar.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SystemMessageDto(@JsonProperty("text") String text) implements MessageDto {
    @JsonCreator
    public SystemMessageDto {}
}
