package iuh.fit.se.model.dto;

public record UserResponseDto(
		String phoneNumber, 
		String name, 
		boolean gender, 
		String dateOfBirth,
		String bio,
		String baseImg,
		String backgroundImg
		) {
    	
}