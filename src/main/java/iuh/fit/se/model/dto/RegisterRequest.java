package iuh.fit.se.model.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {
	String name;
	String dateOfBirth;
	MultipartFile avatar;
	String phone;
	String password;
	boolean gender;
}
