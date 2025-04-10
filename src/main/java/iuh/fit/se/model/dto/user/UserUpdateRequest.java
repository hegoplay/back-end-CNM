package iuh.fit.se.model.dto.user;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserUpdateRequest(
		String name, 
		boolean isMale, 
		LocalDate dateOfBirth,
		String bio,
		String baseImg,
		String backgroundImg
) {
    	
}