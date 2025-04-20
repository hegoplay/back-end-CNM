package iuh.fit.se.model.dto.conversation;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDto {
    private String phoneNumber;
    private String name;
    private boolean isAdmin;
    private boolean isLeader;
    private String baseImg;
    private boolean isOnline; // Trạng thái online
}