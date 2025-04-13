package iuh.fit.se.model.dto.message;

import iuh.fit.se.model.enumObj.MessageType;
import lombok.Data;

@Data
public class MessageRequestDTO {
    private String conversationId;
    private String senderId;
    private String content; // Text content hoặc media URL
    private MessageType type; // TEXT, MEDIA, CALL_EVENT
    private String replyTo; // ID tin nhắn được reply (nullable)
    private CallEventDTO callEvent; // Dùng khi type = CALL_EVENT
}

