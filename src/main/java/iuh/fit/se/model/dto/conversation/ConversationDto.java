package iuh.fit.se.model.dto.conversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;

import iuh.fit.se.model.enumObj.ConversationType;
import lombok.Builder;
import lombok.Data;

@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Builder
@Data
@lombok.experimental.FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ConversationDto {

    private String id;
    private ConversationType type;
    private List<String> participants;
    private List<String> messages; //messages id
    
    private Boolean callInProgress;
    private String currentCallId;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt;

    public String getId() {
        return id;
    }
}
