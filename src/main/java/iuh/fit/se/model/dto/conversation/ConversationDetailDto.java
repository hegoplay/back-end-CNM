package iuh.fit.se.model.dto.conversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import iuh.fit.se.model.Message;
import iuh.fit.se.model.dto.CustomLocalDateTimeSerializer;
import iuh.fit.se.model.dto.message.MessageResponseDTO;
import iuh.fit.se.model.enumObj.ConversationType;
import lombok.Builder;
import lombok.Data;

@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Builder
@Data
@lombok.experimental.FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ConversationDetailDto {

    private String id;
    private ConversationType type;
    private List<String> participants;
    private List<MessageResponseDTO> messageDetails; //messages id
    
    private Boolean callInProgress;
    private String currentCallId;
    
    
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)   
    LocalDateTime updatedAt;
    
    private String conversationName;
    private String leader;
    private List<String> admins;
    private String conversationImgUrl;
    
    public String getId() {
        return id;
    }
}
