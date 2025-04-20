package iuh.fit.se.model.dto.conversation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGroupRequest {
    private String name;
    private MultipartFile baseImg;
    private List<String> memberIds;
}