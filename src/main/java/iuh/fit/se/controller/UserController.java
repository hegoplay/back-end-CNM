package iuh.fit.se.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import iuh.fit.se.model.dto.UserResponseDto;
import iuh.fit.se.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
public class UserController {

	UserService userService;

	@GetMapping("/test")
	public String test() {
		return "test";
	}

	@GetMapping("/{phone}")
	public ResponseEntity<UserResponseDto> getUserInfo(@PathVariable String phone) {
		UserResponseDto response = userService.getUserInfo(phone);

		if (response == null) {
			return ResponseEntity.notFound().build(); // Trả về 404 nếu không tìm thấy
		}

		return ResponseEntity.ok(response); // Trả về 200 với data

	}
}
