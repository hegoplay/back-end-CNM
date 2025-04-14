package iuh.fit.se.model.dto.user;

import java.time.LocalDate;

import org.springframework.web.multipart.MultipartFile;

public record UserUpdateRequest(
		String name, 
		boolean isMale, 
		LocalDate dateOfBirth,
		String bio,
		MultipartFile baseImg,
		MultipartFile backgroundImg
) {
    	
}