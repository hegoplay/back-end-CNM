package iuh.fit.se.model.dto.message;

import lombok.Data;

@Data
public class CallEventDTO {
    private String callId;
    private String action; // "started", "ended", "missed"
    private Integer duration; // seconds
}

