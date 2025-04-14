package iuh.fit.se.model.dto.user;

import java.time.LocalDateTime;

public record UserUpdateRequestJSON(
		String phoneNumber, 
		String name, 
		boolean isMale, 
		String dateOfBirth,
		String bio,
		String baseImg,
		String backgroundImg
) {
    	
}