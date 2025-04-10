package iuh.fit.se.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import iuh.fit.se.model.dto.UserResponseDto;
import iuh.fit.se.model.dto.user.UserUpdateRequest;
import iuh.fit.se.service.UserService;
import iuh.fit.se.util.JwtUtils;
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
	JwtUtils jwtUtils;
	
	@GetMapping("/test")
	public String test() {
		return "test";
	}
	
	@PostMapping("/whoami")
	public ResponseEntity<UserResponseDto> getWhoAmI(@RequestHeader("Authorization") String authHeader) {
		
	    // Kiểm tra header và loại bỏ prefix "Bearer " nếu có
	    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 nếu header không hợp lệ
	    }

	    // Lấy JWT bằng cách loại bỏ "Bearer " prefix
	    String jwt = authHeader.substring(7);
	    
	    // Lấy phone từ token
	    String phone = jwtUtils.getPhoneFromToken(jwt);
	    UserResponseDto response = userService.getUserInfo(phone);

	    if (response == null) {
	        return ResponseEntity.notFound().build(); // 404 nếu không tìm thấy
	    }

	    return ResponseEntity.ok(response); // 200 với data
	}

	@GetMapping("/{phone}")
	public ResponseEntity<UserResponseDto> getUserInfo(@PathVariable String phone) {
		UserResponseDto response = userService.getUserInfo(phone);

		if (response == null) {
			return ResponseEntity.notFound().build(); // Trả về 404 nếu không tìm thấy
		}

		return ResponseEntity.ok(response); // Trả về 200 với data

	}
//	Authorization chinh la jwt 
//	@PutMapping("/")
	@PutMapping("/")
	public ResponseEntity<UserResponseDto> updateUserInfo(@RequestHeader("Authorization") String authHeader, @ModelAttribute UserUpdateRequest userInfo) {
		log.info("JOINED");
		String jwt = authHeader.substring(7);
		String phone = jwtUtils.getPhoneFromToken(jwt);
		UserResponseDto updatedUser = userService.updateUserInfo(phone, userInfo);

		if (updatedUser == null) {
			return ResponseEntity.notFound().build(); // Trả về 404 nếu không tìm thấy
		}

		return ResponseEntity.ok(updatedUser); // Trả về 200 với data
	}
	@DeleteMapping("/{phone}")
	public ResponseEntity<Void> deleteUser(@PathVariable String phone) {
		userService.deleteUser(phone);
		return ResponseEntity.noContent().build(); // Trả về 204 No Content
	}
}
