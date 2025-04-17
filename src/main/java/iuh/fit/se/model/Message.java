package iuh.fit.se.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import iuh.fit.se.model.converter.TypeConversationConverter;
import iuh.fit.se.model.converter.TypeMessageConverter;
import iuh.fit.se.model.enumObj.MessageType;
import lombok.Builder;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

@Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.experimental.FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@DynamoDbBean
@JsonSerialize
@JsonDeserialize
@Builder
public class Message {
    private String id;
    private String conversationId;
    private String senderId;
    private String content;
    private MessageType type; // "text" | "media" | "call-event"
    private String mediaUrl; // URL của media (ảnh, video, v.v.)
    private List<String> seenBy; // Danh sách user đã xem
    private LocalDateTime createdAt;
    private boolean isRecalled; // Tin nhắn có bị thu hồi không
    private String replyTo; // Tin nhắn mà tin nhắn này trả lời (nullable)
    private List<Reaction> reactions; // Danh sách phản ứng
    private CallEvent callEvent; // Dữ liệu sự kiện cuộc gọi (nếu type là "call-event")

    @Data
	@lombok.NoArgsConstructor
	@lombok.AllArgsConstructor
	@lombok.experimental.FieldDefaults(level = lombok.AccessLevel.PRIVATE)
	@DynamoDbBean
	@JsonSerialize
	@JsonDeserialize
    public static class Reaction {
        private String emoji; // Emoji được sử dụng (ví dụ: "❤️")
        private List<String> users; // Danh sách user đã phản ứng với emoji này
        
    }
    
    @Data
	@lombok.NoArgsConstructor
	@lombok.AllArgsConstructor
	@lombok.experimental.FieldDefaults(level = lombok.AccessLevel.PRIVATE)
	@DynamoDbBean
	@JsonSerialize
	@JsonDeserialize
    public static class CallEvent {
    	private String callId; // ID của cuộc gọi
        private String action; // "started" | "ended" | "missed"
        private int duration; // Thời lượng cuộc gọi (tính bằng giây)
    }
    
    @DynamoDbPartitionKey
	public String getId() {
		return id;
	}
    
    @DynamoDbAttribute("type")
    @DynamoDbConvertedBy(TypeMessageConverter.class)
    public MessageType getType() {
        return type;
    }
    
    @DynamoDbSecondaryPartitionKey(indexNames = "conversationId-createdAt-index")
    public String getConversationId() {
        return conversationId;
    }
    
    @DynamoDbSecondarySortKey(indexNames = "conversationId-createdAt-index")
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
