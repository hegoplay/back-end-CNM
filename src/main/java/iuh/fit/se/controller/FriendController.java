package iuh.fit.se.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import iuh.fit.se.model.dto.UserResponseDto;
import iuh.fit.se.service.ConversationService;
import iuh.fit.se.service.UserService;
import iuh.fit.se.util.JwtUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
public class FriendController {
		
	ConversationService conversationService;
	JwtUtils jwtUtils;
	UserService userService;	
	
	@PostMapping("/accept")
	public ResponseEntity<Void> addFriend(@RequestHeader("Authorization") String authHeader, @RequestBody String friendPhone) {
		// Kiểm tra header và loại bỏ prefix "Bearer " nếu có
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 nếu header không hợp lệ
		}

		// Lấy JWT bằng cách loại bỏ "Bearer " prefix
		String jwt = authHeader.substring(7);
		
		// Lấy phone từ token
		String phone = jwtUtils.getPhoneFromToken(jwt);
		
		conversationService.createFriendConversation(phone, friendPhone);
		userService.acceptRequest(phone, friendPhone);
		
		return ResponseEntity.ok().build();
	}
	
	@GetMapping("/")
	public ResponseEntity<List<UserResponseDto>> getFriends(@RequestHeader("Authorization") String authHeader) {
		// Kiểm tra header và loại bỏ prefix "Bearer " nếu có
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 nếu header không hợp lệ
		}

		// Lấy JWT bằng cách loại bỏ "Bearer " prefix
		String jwt = authHeader.substring(7);
		
		// Lấy phone từ token
		String phone = jwtUtils.getPhoneFromToken(jwt);
		
		return ResponseEntity.ok(userService.getFriends(phone));
	}
}
