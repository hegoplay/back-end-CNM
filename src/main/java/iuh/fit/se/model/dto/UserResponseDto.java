package iuh.fit.se.model.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
@Builder	
public class UserResponseDto implements Serializable {
    private static final long serialVersionUID = -2652515556721756516L;
	private String phoneNumber;
    private String name;
    private boolean isMale;
    private String dateOfBirth;
    private String bio;
    private String baseImg;
    private String backgroundImg;
    private String status;
    private boolean isOnline;
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)  
    private LocalDateTime lastOnlineTime;
}