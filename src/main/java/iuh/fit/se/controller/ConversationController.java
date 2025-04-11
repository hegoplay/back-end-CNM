package iuh.fit.se.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import iuh.fit.se.model.dto.ConversationDto;
import iuh.fit.se.model.dto.UserResponseDto;
import iuh.fit.se.service.ConversationService;
import iuh.fit.se.service.UserService;
import iuh.fit.se.util.JwtUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
public class ConversationController {
		
	ConversationService conversationService;
	JwtUtils jwtUtils;
	UserService userService;	
	
	private record PhoneObj (String phone){
		
	}
	
	@GetMapping("/")
	public ResponseEntity<List<ConversationDto>> getConversations(@RequestHeader("Authorization") String authHeader) {
		// Lấy JWT bằng cách loại bỏ "Bearer " prefix
		String jwt = authHeader.substring(7);
		
		// Lấy phone từ token
		String phone = jwtUtils.getPhoneFromToken(jwt);
		
		List<ConversationDto> conversations = conversationService.getConversations(phone);
		
		return ResponseEntity.ok(conversations);
	}
	
}
