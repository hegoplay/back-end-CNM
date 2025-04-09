package iuh.fit.se.model;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Builder;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.experimental.FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@DynamoDbBean
@Builder
@JsonSerialize
@JsonDeserialize
public class Call {
	private String id;
    private String callType; // "video" | "audio"
    private String initiatorId;
    private List<String> participants;
    private String conversationId;
    private String status; // "pending" | "ongoing" | "ended" | "missed"
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration; // Thời lượng (giây)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}
