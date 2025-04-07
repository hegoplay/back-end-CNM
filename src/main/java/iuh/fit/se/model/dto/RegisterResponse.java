package iuh.fit.se.model.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@lombok.NoArgsConstructor
public class RegisterResponse {
	boolean success;
	String message;
}
