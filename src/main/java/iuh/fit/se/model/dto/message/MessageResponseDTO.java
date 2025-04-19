package iuh.fit.se.model.dto.message;

import iuh.fit.se.model.dto.CustomLocalDateTimeSerializer;
import iuh.fit.se.model.enumObj.MessageType;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Data
public class MessageResponseDTO {
    private String id;
    private String conversationId;
    private String senderId;
    private String content;
    private MessageType type;
//    đang bị dư, đang kiểm tra lại
//    private String mediaUrl;
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class) 
    private LocalDateTime createdAt;
    @JsonProperty("isRecalled")
    private boolean isRecalled;
    private String replyTo;
    private List<ReactionDTO> reactions;
    private String callId;
    
    @Data
    public static class ReactionDTO {
        private String emoji;
        private List<String> users;
    }
}

