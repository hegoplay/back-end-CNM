package iuh.fit.se.model;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.experimental.FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@DynamoDbBean
@JsonSerialize
@JsonDeserialize
public class Message {
    private String id;
    private String conversationId;
    private String senderId;
    private String content;
    private String type; // "text" | "media" | "call-event"
    private String mediaUrl; // URL của media (ảnh, video, v.v.)
    private List<String> seenBy; // Danh sách user đã xem
    private LocalDate createdAt;
    private boolean isRecalled; // Tin nhắn có bị thu hồi không
    private String replyTo; // Tin nhắn mà tin nhắn này trả lời (nullable)
    private String sendTime; // Thời gian gửi (có thể lưu dạng string)
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
        private String action; // "started" | "ended" | "missed"
        private int duration; // Thời lượng cuộc gọi (tính bằng giây)
    }
    
    @DynamoDbPartitionKey
	public String getId() {
		return id;
	}
}
