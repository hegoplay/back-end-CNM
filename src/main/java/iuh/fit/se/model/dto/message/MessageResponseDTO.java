package iuh.fit.se.model.dto.message;

import iuh.fit.se.model.enumObj.MessageType;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class MessageResponseDTO {
    private String id;
    private String conversationId;
    private String senderId;
    private String content;
    private MessageType type;
    private String mediaUrl;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonProperty("isRecalled")
    private boolean isRecalled;
    private String replyTo;
    private List<ReactionDTO> reactions;
    private CallEventDTO callEvent;
    
    @Data
    public static class ReactionDTO {
        private String emoji;
        private List<String> users;
    }
}

