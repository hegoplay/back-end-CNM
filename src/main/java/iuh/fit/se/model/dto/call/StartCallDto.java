package iuh.fit.se.model.dto.call;

import iuh.fit.se.model.enumObj.CallType;
import lombok.Data;

@Data
public class StartCallDto {
    private String conversationId; // ID của conversation
    private CallType callType; // Loại cuộc gọi (video/audio)
    private String callId; // ID của cuộc gọi
}