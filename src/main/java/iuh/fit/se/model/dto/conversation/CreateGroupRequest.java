package iuh.fit.se.model.dto.conversation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateGroupRequest {
    private String conversationName;
    private String conversationImgUrl;
    private List<String> participants; // List of phone numbers, including the creator
}