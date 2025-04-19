package iuh.fit.se.controller;

import java.util.List;
import java.util.Map;

import iuh.fit.se.model.dto.conversation.CreateGroupRequest;
import iuh.fit.se.util.FormatUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
	@DeleteMapping("/{conversationId}")
	public ResponseEntity<Void> deleteConversation(@PathVariable String conversationId, @RequestHeader("Authorization") String authHeader) {
		String jwt = authHeader.substring(7);
		String phone = jwtUtils.getPhoneFromToken(jwt);
//		conversationService.deleteFriendConversation(conversationId, phone);
		
		return ResponseEntity.ok().build();
	}

	@PostMapping("/create-group")
	public ResponseEntity<ConversationDetailDto> createGroupChat(@RequestBody CreateGroupRequest request, @RequestHeader("Authorization") String authHeader) {
//		{
//			"conversationName": "TestingCreatingGroubImplement",
//				"conversationImgUrl": "conversationImgUrl",
//				"participants": [
//			"+9999999999",
//					"+8433667701",
//					"+84376626025"
//   		]
//		}
		String jwt = authHeader.substring(7);
		String creatorPhone = jwtUtils.getPhoneFromToken(jwt);
		log.info("Trying to create group with request: {}", request);
		ConversationDetailDto conversation = conversationService.createGroupChat(creatorPhone, request.getConversationName(), request.getConversationImgUrl(), request.getParticipants());
		return ResponseEntity.status(HttpStatus.CREATED).body(conversation);
	}

	@PostMapping("/{conversationId}/add-members")
	public ResponseEntity<Void> addMembers(@PathVariable String conversationId, @RequestBody List<String> newMembersPhone, @RequestHeader("Authorization") String authHeader) {
		try {
			if (authHeader == null || !authHeader.startsWith("Bearer ")) {
				throw new IllegalArgumentException("Invalid Authorization header");
			}
			String jwt = authHeader.substring(7);
			String userPhone = jwtUtils.getPhoneFromToken(jwt);
			if (userPhone == null || userPhone.isEmpty()) {
				throw new IllegalArgumentException("Invalid user phone retrieved from token");
			}
			log.info("Trying to add members to conversation: {}", newMembersPhone);
			for(String newMemberPhone : newMembersPhone) {
				newMemberPhone = FormatUtils.formatPhoneNumber(newMemberPhone);
			}
			conversationService.addMembersToGroup(conversationId, userPhone, newMembersPhone);
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@DeleteMapping("/{conversationId}/delete-member")
	public ResponseEntity<Void> removeMember(
			@PathVariable String conversationId,
			@RequestParam String memberPhone,
			@RequestHeader("Authorization") String authHeader) {
		String jwt = authHeader.substring(7);
		String userPhone = jwtUtils.getPhoneFromToken(jwt);
		memberPhone = FormatUtils.formatPhoneNumber(memberPhone);
		log.info("Trying to remove members from conversation: {}", memberPhone);
		conversationService.removeMemberFromGroup(conversationId, userPhone, memberPhone);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/{conversationId}/leave")
	public ResponseEntity<Void> leaveGroup(
			@PathVariable String conversationId,
			@RequestBody String newLeaderPhone,
			@RequestHeader("Authorization") String authHeader) {
		String jwt = authHeader.substring(7);
		String userPhone = jwtUtils.getPhoneFromToken(jwt);
		newLeaderPhone = FormatUtils.formatPhoneNumber(newLeaderPhone);
		conversationService.leaveGroup(conversationId, userPhone, newLeaderPhone);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/{conversationId}/delete-group")
	public ResponseEntity<Void> deleteGroup(
			@PathVariable String conversationId,
			@RequestHeader("Authorization") String authHeader) {

		String jwt = authHeader.substring(7);
		String userPhone = jwtUtils.getPhoneFromToken(jwt);

		log.info("User {} is attempting to delete group with ID: {}", userPhone, conversationId);

		try {
			conversationService.deleteGroup(conversationId, userPhone);
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			log.warn("Delete group failed due to invalid request: {}", e.getMessage());
			return ResponseEntity.badRequest().build();
		} catch (Exception e) {
			log.error("Failed to delete group: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

}
