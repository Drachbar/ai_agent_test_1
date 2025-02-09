package se.drachbar.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AiMessageDto(
        @JsonProperty("text") String text,
        @JsonProperty("modelName") String modelName
) implements MessageDto {
    @JsonCreator public AiMessageDto {}
}
