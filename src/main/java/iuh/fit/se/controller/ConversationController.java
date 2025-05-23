package iuh.fit.se.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import iuh.fit.se.model.dto.conversation.AddMemberRequestDto;
import iuh.fit.se.model.dto.conversation.ConversationDetailDto;
import iuh.fit.se.model.dto.conversation.ConversationDto;
import iuh.fit.se.model.dto.conversation.CreateGroupImgDto;
import iuh.fit.se.model.dto.conversation.CreateGroupRequest;
import iuh.fit.se.model.dto.conversation.LeaveGroupRequestDto;
import iuh.fit.se.model.dto.conversation.MemberDto;
import iuh.fit.se.model.dto.conversation.UpdateAdminRequestDto;
import iuh.fit.se.model.dto.message.MessageRequestDTO;
import iuh.fit.se.model.dto.message.MessageResponseDTO;
import iuh.fit.se.model.enumObj.ConversationType;
import iuh.fit.se.service.ConversationService;
import iuh.fit.se.service.MessageNotifier;
import iuh.fit.se.service.UserService;
import iuh.fit.se.serviceImpl.AwsService;
import iuh.fit.se.util.FormatUtils;
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
	AwsService awsService;
    ObjectMapper objectMapper;

//  phải giữ
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
	public ResponseEntity<ConversationDetailDto> getConversationDetail(@PathVariable String conversationId, @RequestHeader("Authorization") String authHeader){
//		code di copilot
		String jwt = authHeader.substring(7);
		String phone = jwtUtils.getPhoneFromToken(jwt);
		ConversationDetailDto conversation = conversationService.getConversationDetail(conversationId, phone);
		if (conversation == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		return ResponseEntity.ok(conversation);
	}
	
	@GetMapping("/initialize/{conversationId}")
	public ResponseEntity<ConversationDetailDto> markNotificationAsRead(@PathVariable String conversationId,
			@RequestHeader("Authorization") String authHeader) {
		log.info("Marking notification as read for conversation: {}", conversationId);
		String jwt = authHeader.substring(7);
		String phone = jwtUtils.getPhoneFromToken(jwt);
		ConversationDetailDto conversation = conversationService.getConversationDetail(conversationId, phone);
		messageNotifier.initConversation(conversation, phone);
		return ResponseEntity.ok(conversation);
	}

	@PostMapping("/mark-as-read/{conversationId}")
	public ResponseEntity<Void> markAllMessagesAsRead(@PathVariable String conversationId,
			@RequestHeader("Authorization") String authHeader) {
		log.info("Marking all messages as read for conversation: {}", conversationId);
		String jwt = authHeader.substring(7);
		String phone = jwtUtils.getPhoneFromToken(jwt);
		conversationService.markAllMessagesAsRead(conversationId, phone);
		return ResponseEntity.ok().build();
	}
	
	@DeleteMapping("/{conversationId}")
	public ResponseEntity<Void> deleteConversation(@PathVariable String conversationId,
			@RequestHeader("Authorization") String authHeader) {
		String jwt = authHeader.substring(7);
		String phone = jwtUtils.getPhoneFromToken(jwt);
		ConversationDto conversationById = conversationService.getConversationById(conversationId);
		
		if (conversationById.getType() == ConversationType.PRIVATE) {
			conversationService.deleteFriendConversation(conversationId, phone);	
		}
		
		if (conversationById.getType() == ConversationType.GROUP) {
			try {
				conversationService.deleteGroup(conversationId, phone);
				return ResponseEntity.ok().build();
			} catch (IllegalArgumentException e) {
				log.warn("Delete group failed due to invalid request: {}", e.getMessage());
				return ResponseEntity.badRequest().build();
			} catch (Exception e) {
				log.error("Failed to delete group: {}", e.getMessage(), e);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
			}
		}

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
	public ResponseEntity<Void> addMembers(@PathVariable String conversationId, @RequestBody AddMemberRequestDto request, @RequestHeader("Authorization") String authHeader) {
		try {
			if (authHeader == null || !authHeader.startsWith("Bearer ")) {
				throw new IllegalArgumentException("Invalid Authorization header");
			}
			String jwt = authHeader.substring(7);
			String userPhone = jwtUtils.getPhoneFromToken(jwt);
			if (userPhone == null || userPhone.isEmpty()) {
				throw new IllegalArgumentException("Invalid user phone retrieved from token");
			}
			log.info("Trying to add members to conversation: {}", request);
			
			List<String> list = request.getNewMembersPhone().stream().map(FormatUtils::formatPhoneNumber).toList();
			
			conversationService.addMembersToGroup(conversationId, list);
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
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
			@RequestBody LeaveGroupRequestDto request,
			@RequestHeader("Authorization") String authHeader) {
		String jwt = authHeader.substring(7);
		String userPhone = jwtUtils.getPhoneFromToken(jwt);
		String newLeaderPhone = request.getNewLeaderPhone() !=null ? FormatUtils.formatPhoneNumber(request.getNewLeaderPhone()) : null;
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
		}
	}

	@PostMapping("/{conversationId}/join")
	public ResponseEntity<Void> joinGroup(
			@PathVariable String conversationId,
			@RequestHeader("Authorization") String authHeader) {

		String jwt = authHeader.substring(7);
		String userPhone = jwtUtils.getPhoneFromToken(jwt);
		if (userPhone == null || userPhone.isEmpty()) {
			throw new IllegalArgumentException("Invalid user phone retrieved from token");
		}
		log.info("User {} is attempting to join group with ID: {}", userPhone, conversationId);
		userPhone = FormatUtils.formatPhoneNumber(userPhone);
		conversationService.joinGroup(conversationId, userPhone);
		return ResponseEntity.ok().build();
	}

	@PostMapping(value = "/group", consumes = { "multipart/form-data" }, produces = { "application/json" })
	public ResponseEntity<ConversationDetailDto> createGroup(@RequestHeader("Authorization") String authHeader,
			@RequestPart("name") String name, @RequestPart(value = "baseImg", required = false) MultipartFile baseImg,
			@RequestPart("memberIds") String memberIdsJson) {
		try {
			String jwt = authHeader.substring(7);
			String phone = jwtUtils.getPhoneFromToken(jwt);
			log.info("Creating group: userId={}, name={}, memberIds={}, baseImg= {}", phone, name, memberIdsJson, baseImg.getOriginalFilename());
			
			List<String> memberIds = parseMemberIds(memberIdsJson);
			memberIds.stream().anyMatch(id -> !userService.isExistPhone(id));
			CreateGroupImgDto request = CreateGroupImgDto.builder().conversationName(name).conversationImgUrl(baseImg).participants(memberIds)
					.build();
			ConversationDetailDto conversationId = conversationService.createGroupChat(request, phone);
			log.info("Group created successfully: {}", conversationId);
			return ResponseEntity.ok(conversationId);
		} catch (Exception e) {
			log.error("Failed to create group: {}", e.getMessage());
			throw new RuntimeException("Fail to create group: %s".formatted(e.getMessage()));
		}
	}
	
//	@PostMa

	@GetMapping("/{conversationId}/members")
	public ResponseEntity<List<MemberDto>> getGroupMembers(@PathVariable String conversationId,
			@RequestHeader("Authorization") String authHeader) {
		try {
			String jwt = authHeader.substring(7);
			String phone = jwtUtils.getPhoneFromToken(jwt);
			ConversationDetailDto conversation = conversationService.getConversationDetail(conversationId);
			if (!conversation.getParticipants().contains(phone)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
			}
			List<MemberDto> members = conversationService.getGroupMembers(conversationId);
			return ResponseEntity.ok(members);
		} catch (Exception e) {
			log.error("Failed to get group members: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping("/{conversationId}/members/search")
	public ResponseEntity<List<MemberDto>> searchMembers(@PathVariable String conversationId,
			@RequestParam String keyword, @RequestHeader("Authorization") String authHeader) {
		try {
			String jwt = authHeader.substring(7);
			String phone = jwtUtils.getPhoneFromToken(jwt);
			ConversationDetailDto conversation = conversationService.getConversationDetail(conversationId);
			if (!conversation.getParticipants().contains(phone)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
			}
			List<MemberDto> result = conversationService.searchMembers(conversationId, keyword);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			log.error("Failed to search members: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PutMapping("/{conversationId}/admin")
	public ResponseEntity<Void> updateAdmin(@RequestHeader("Authorization") String authHeader,
			@PathVariable String conversationId, @RequestBody UpdateAdminRequestDto request) {
		try {
			String jwt = authHeader.substring(7);
			String phone = jwtUtils.getPhoneFromToken(jwt);
			String targetUserPhone = FormatUtils.formatPhoneNumber(request.getTargetUserId());
			if (!userService.isExistPhone(targetUserPhone)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
			}
			conversationService.updateAdmin(phone, conversationId, targetUserPhone, request.isAdmin());
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			log.error("Failed to update admin: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PutMapping(value = "/{conversationId}/info", consumes = {"multipart/form-data"})
	public ResponseEntity<String> updateGroupInfo(
	    @RequestHeader("Authorization") String authHeader,
	    @PathVariable String conversationId,
	    @RequestPart(required = false) String conversationName,
	    @RequestPart(value = "baseImg", required = false) MultipartFile baseImg
	) {
	    try {
	        // Kiểm tra và lấy phone từ JWT
	        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing Authorization header");
	        }
	        String jwt = authHeader.substring(7);
	        String phone = jwtUtils.getPhoneFromToken(jwt);

	        // Kiểm tra quyền truy cập
	        ConversationDetailDto conversation = conversationService.getConversationDetail(conversationId);
	        if (!conversation.getParticipants().contains(phone)) {
	            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User is not a member of this group");
	        }

	        // Xử lý upload ảnh
	        String imageUrl = null;
	        if (baseImg != null && !baseImg.isEmpty()) {
	            try {
	                imageUrl = awsService.uploadToS3(baseImg);
	                log.info("Uploaded group image to S3: {}", imageUrl);
	            } catch (Exception e) {
	                log.error("Failed to upload group image: {}", e.getMessage());
	                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload group image");
	            }
	        }

	        // Gọi service để cập nhật thông tin nhóm
	        conversationService.updateGroupInfo(phone, conversationId, conversationName, imageUrl);
	        return ResponseEntity.ok("Group updated successfully");

	    } catch (RuntimeException e) {
	        log.error("Failed to update group info: {}", e.getMessage());
	        if (e.getMessage().equals("Conversation not found")) {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Conversation not found");
	        }
	        if (e.getMessage().equals("Only admin or leader can update group info")) {
	            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admin or leader can update group info");
	        }
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update group info");
	    } catch (Exception e) {
	        log.error("Unexpected error: {}", e.getMessage());
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred");
	    }
	}

	private List<String> parseMemberIds(String memberIdsJson) {
		try {
			return objectMapper.readValue(memberIdsJson, new TypeReference<List<String>>() {
			});
		} catch (Exception e) {
			log.error("Failed to parse memberIds: {}", memberIdsJson, e);
			throw new IllegalArgumentException("Invalid memberIds format");
		}
	}
}