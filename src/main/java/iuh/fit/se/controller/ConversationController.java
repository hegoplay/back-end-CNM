package iuh.fit.se.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import iuh.fit.se.model.dto.conversation.ConversationDetailDto;
import iuh.fit.se.model.dto.conversation.ConversationDto;
import iuh.fit.se.service.ConversationService;
import iuh.fit.se.service.MessageNotifier;
import iuh.fit.se.service.MessageService;
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
	MessageNotifier messageNotifier;
	JwtUtils jwtUtils;
	UserService userService;	
	
	
	@GetMapping("/")
	public ResponseEntity<List<ConversationDto>> getConversations(@RequestHeader("Authorization") String authHeader) {
		// Lấy JWT bằng cách loại bỏ "Bearer " prefix
		String jwt = authHeader.substring(7);
		
		// Lấy phone từ token
		String phone = jwtUtils.getPhoneFromToken(jwt);
		
		List<ConversationDto> conversations = conversationService.getConversations(phone);
		
		return ResponseEntity.ok(conversations);
	}
	
	@GetMapping("/{conversationId}")
	public ResponseEntity<ConversationDetailDto> getConversationDetail(@PathVariable String conversationId){
//		code di copilot
		ConversationDetailDto conversation = conversationService.getConversationDetail(conversationId);
		if (conversation == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		return ResponseEntity.ok(conversation);
	}
//	request thang nay để gửi event để nhận dữ liệu currentConversation
	@GetMapping("/initialize/{conversationId}")
	public ResponseEntity<ConversationDetailDto> markNotificationAsRead(@PathVariable String conversationId, @RequestHeader("Authorization") String authHeader) {
		log.info("Marking notification as read for conversation: {}", conversationId);
		String jwt = authHeader.substring(7);
		String phone = jwtUtils.getPhoneFromToken(jwt);
		ConversationDetailDto conversation = conversationService.getConversationDetail(conversationId);
		messageNotifier.initConversation(conversation, phone);
		return ResponseEntity.ok(conversation);
	}
	@PostMapping("/mark-as-read/{conversationId}")
	public ResponseEntity<Void> markAllMessagesAsRead(@PathVariable String conversationId, @RequestHeader("Authorization") String authHeader) {
		log.info("Marking all messages as read for conversation: {}", conversationId);
		String jwt = authHeader.substring(7);
		String phone = jwtUtils.getPhoneFromToken(jwt);
		conversationService.markAllMessagesAsRead(conversationId, phone);
		return ResponseEntity.ok().build();
	}
//	@DeleteMapping("/{conversationId}")
//	public ResponseEntity<Void> deleteConversation(@PathVariable String conversationId, @RequestHeader("Authorization") String authHeader) {
//		log.info("Deleting conversation: {}", conversationId);
//		String jwt = authHeader.substring(7);
//		String phone = jwtUtils.getPhoneFromToken(jwt);
//		conversationService.deleteFriendConversation(conversationId, phone);
//		return ResponseEntity.ok().build();
//	}
}
