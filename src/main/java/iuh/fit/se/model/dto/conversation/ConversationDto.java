package iuh.fit.se.model.dto.conversation;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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
public class ConversationDto {

    private String id;
    private ConversationType type;
    private List<String> participants;
    private List<String> messages; //messages id
    
    private Boolean callInProgress;
    private String currentCallId;
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)  
    LocalDateTime updatedAt;

//    updated 
    private String conversationName;
    private String leader;
    private List<String> admins;
    private MessageResponseDTO lastMessage;
    private String conversationImgUrl;
    private Integer unreadCount;
    
    public String getId() {
        return id;
    }
}
