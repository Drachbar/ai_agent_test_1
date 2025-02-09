package se.drachbar.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AiMessageDto.class, name = "AIMessage"),
        @JsonSubTypes.Type(value = UserMessageDto.class, name = "UserMessage"),
        @JsonSubTypes.Type(value = SystemMessageDto.class, name = "SystemMessage"),
        @JsonSubTypes.Type(value = ToolExecutionResultMessageDto.class, name = "ToolExecutionResultMessage")
})
public sealed interface MessageDto permits AiMessageDto, SystemMessageDto, ToolExecutionResultMessageDto, UserMessageDto {
}

