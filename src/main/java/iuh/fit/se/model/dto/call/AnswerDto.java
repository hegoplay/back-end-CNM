package iuh.fit.se.model.dto.call;

import lombok.Data;

@Data
public class AnswerDto {
    private String targetUserId;
    private String answer;
    private String roomId;
}