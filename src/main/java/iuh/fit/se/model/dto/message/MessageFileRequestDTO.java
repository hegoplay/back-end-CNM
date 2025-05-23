package iuh.fit.se.model.dto.message;

import org.springframework.web.multipart.MultipartFile;

import iuh.fit.se.model.enumObj.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MessageFileRequestDTO{
	private String conversationId;
    private String senderId;
    private String content; // Text content hoặc media URL
    private MessageType type; // TEXT, MEDIA, CALL_EVENT
    private String replyTo; // ID tin nhắn được reply (nullable)
//    private CallEventDTO callEvent; // Dùng khi type = CALL_EVENT
    private String callId; // ID cuộc gọi (nullable)
    private MultipartFile file; // Dùng khi type = MEDIA
}
