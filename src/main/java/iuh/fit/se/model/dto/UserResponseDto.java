package iuh.fit.se.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
@Builder	
public class UserResponseDto {
    private String phoneNumber;
    private String name;
    private boolean isMale;
    private String dateOfBirth;
    private String bio;
    private String baseImg;
    private String backgroundImg;
    private String status;
    private boolean isOnline;
    private LocalDateTime lastOnlineTime;
}