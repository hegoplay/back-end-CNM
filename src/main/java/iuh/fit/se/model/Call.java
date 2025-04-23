package iuh.fit.se.model;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import iuh.fit.se.model.enumObj.CallStatus;
import iuh.fit.se.model.enumObj.CallType;
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
public class Call {
	private String id;
    private CallType callType; // "video" | "audio"
    private String initiatorId;
    private List<String> participants;
    private String conversationId;
    private CallStatus status; // "pending" | "ongoing" | "ended" | "missed"
    private LocalDateTime startTime;
    private LocalDateTime endTime;
//    cái thuộc tính này sẽ để trong dto ?
//    private Integer duration; // Thời lượng (giây)

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}
