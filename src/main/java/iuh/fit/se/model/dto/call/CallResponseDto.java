package iuh.fit.se.model.dto.call;

import iuh.fit.se.model.enumObj.CallStatus;
import iuh.fit.se.model.enumObj.CallType;
import lombok.Data;

@Data
public class CallResponseDto {
	private String id;
    private CallType callType; // "video" | "audio"
    private CallStatus status;
}
