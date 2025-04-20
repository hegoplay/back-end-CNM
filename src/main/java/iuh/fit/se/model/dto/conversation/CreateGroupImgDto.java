package iuh.fit.se.model.dto.conversation;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateGroupImgDto {
    private String conversationName;
    private MultipartFile conversationImgUrl;
    private List<String> participants; // List of phone numbers, including the creator
}