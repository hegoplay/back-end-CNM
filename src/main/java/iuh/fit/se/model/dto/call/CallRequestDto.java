package iuh.fit.se.model.dto.call;

import lombok.Data;

import java.util.Set;

@Data
public class CallRequestDto {
    private String roomId;
    private Set<String> participants;
}