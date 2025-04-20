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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import iuh.fit.se.model.Conversation;
import iuh.fit.se.model.dto.conversation.ConversationDetailDto;
import iuh.fit.se.model.dto.conversation.ConversationDto;
import iuh.fit.se.model.dto.conversation.CreateGroupRequest;
import iuh.fit.se.model.dto.conversation.MemberDto;
import iuh.fit.se.service.ConversationService;
import iuh.fit.se.service.MessageNotifier;
import iuh.fit.se.service.MessageService;
import iuh.fit.se.service.UserService;
import iuh.fit.se.serviceImpl.AwsService;
import iuh.fit.se.util.JwtUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.thirdparty.jackson.core.JsonProcessingException;

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
	public ResponseEntity<ConversationDetailDto> getConversationDetail(@PathVariable String conversationId) {
//		code di copilot
		ConversationDetailDto conversation = conversationService.getConversationDetail(conversationId);
		if (conversation == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		return ResponseEntity.ok(conversation);
	}

//	request thang nay để gửi event để nhận dữ liệu currentConversation

	@GetMapping("/initialize/{conversationId}")
	public ResponseEntity<ConversationDetailDto> markNotificationAsRead(@PathVariable String conversationId,
			@RequestHeader("Authorization") String authHeader) {
		log.info("Marking notification as read for conversation: {}", conversationId);
		String jwt = authHeader.substring(7);
		String phone = jwtUtils.getPhoneFromToken(jwt);
		ConversationDetailDto conversation = conversationService.getConversationDetail(conversationId);
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
//		conversationService.deleteFriendConversation(conversationId, phone);

		return ResponseEntity.ok().build();
	}

	@PostMapping(value = "/group", consumes = { "multipart/form-data" })
	public ResponseEntity<String> createGroup(@RequestHeader("Authorization") String authHeader,
			@RequestPart("name") String name, @RequestPart(value = "baseImg", required = false) MultipartFile baseImg,
			@RequestPart("memberIds") String memberIdsJson) {
		try {
			String jwt = authHeader.substring(7);
			String phone = jwtUtils.getPhoneFromToken(jwt);
			log.info("Creating group: userId={}, name={}, memberIds={}", phone, name, memberIdsJson);
			List<String> memberIds = parseMemberIds(memberIdsJson);
			if (memberIds.stream().anyMatch(id -> !userService.isExistPhone(id))) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid member IDs");
			}
			CreateGroupRequest request = CreateGroupRequest.builder().name(name).baseImg(baseImg).memberIds(memberIds)
					.build();
			String conversationId = conversationService.createGroup(phone, request);
			return ResponseEntity.ok(conversationId);
		} catch (Exception e) {
			log.error("Failed to create group: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create group");
		}
	}

	@PostMapping("/{conversationId}/members")
    public ResponseEntity<Void> addMembers(@RequestHeader("Authorization") String authHeader,
                                          @PathVariable String conversationId, @RequestBody List<String> memberIds) {
        log.info("Received request to add members to conversationId: {}, memberIds: {}", conversationId, memberIds);
        try {
            String jwt = authHeader.substring(7);
            String phone = jwtUtils.getPhoneFromToken(jwt);
            if (memberIds == null || memberIds.isEmpty()
                    || memberIds.stream().anyMatch(id -> !userService.isExistPhone(id))) {
                log.warn("Invalid memberIds: {}", memberIds);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            conversationService.addMembers(phone, conversationId, memberIds);
            log.info("Successfully added members to conversationId: {}", conversationId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (SecurityException e) {
            log.error("Unauthorized: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Failed to add members: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

//	@DeleteMapping("/{conversationId}/members/{targetUserId}")
//	public ResponseEntity<Void> removeMember(@RequestHeader("Authorization") String authHeader,
//			@PathVariable String conversationId, @PathVariable String targetUserId) {
//		try {
//			String jwt = authHeader.substring(7);
//			String phone = jwtUtils.getPhoneFromToken(jwt);
//			if (!userService.isExistPhone(targetUserId)) {
//				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
//			}
//			conversationService.removeMember(phone, conversationId, targetUserId);
//			return ResponseEntity.ok().build();
//		} catch (Exception e) {
//			log.error("Failed to remove member: {}", e.getMessage());
//			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//		}
//	}
//
//	@PutMapping("/{conversationId}/leave")
//	public ResponseEntity<Void> leaveGroup(@RequestHeader("Authorization") String authHeader,
//			@PathVariable String conversationId) {
//		try {
//			String jwt = authHeader.substring(7);
//			String phone = jwtUtils.getPhoneFromToken(jwt);
//			ConversationDetailDto conversation = conversationService.getConversationDetail(conversationId);
//			if (!conversation.getParticipants().contains(phone)) {
//				return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
//			}
//			conversationService.leaveGroup(phone, conversationId);
//			return ResponseEntity.ok().build();
//		} catch (Exception e) {
//			log.error("Failed to leave group: {}", e.getMessage());
//			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//		}
//	}

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
			@PathVariable String conversationId, @RequestParam String targetUserId, @RequestParam boolean isAdmin) {
		try {
			String jwt = authHeader.substring(7);
			String phone = jwtUtils.getPhoneFromToken(jwt);
			if (!userService.isExistPhone(targetUserId)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
			}
			conversationService.updateAdmin(phone, conversationId, targetUserId, isAdmin);
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			log.error("Failed to update admin: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

//	@PutMapping(value = "/{conversationId}/info", consumes = {"multipart/form-data"})
//	public ResponseEntity<String> updateGroupInfo(
//	    @RequestHeader("Authorization") String authHeader,
//	    @PathVariable String conversationId,
//	    @RequestPart(required = false) String conversationName,
//	    @RequestPart(value = "baseImg", required = false) MultipartFile baseImg
//	) {
//	    try {
//	        // Kiểm tra và lấy phone từ JWT
//	        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing Authorization header");
//	        }
//	        String jwt = authHeader.substring(7);
//	        String phone = jwtUtils.getPhoneFromToken(jwt);
//
//	        // Kiểm tra quyền truy cập
//	        ConversationDetailDto conversation = conversationService.getConversationDetail(conversationId);
//	        if (!conversation.getParticipants().contains(phone)) {
//	            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User is not a member of this group");
//	        }
//
//	        // Xử lý upload ảnh
//	        String imageUrl = null;
//	        if (baseImg != null && !baseImg.isEmpty()) {
//	            try {
//	                imageUrl = awsService.uploadToS3(baseImg);
//	                log.info("Uploaded group image to S3: {}", imageUrl);
//	            } catch (Exception e) {
//	                log.error("Failed to upload group image: {}", e.getMessage());
//	                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload group image");
//	            }
//	        }
//
//	        // Gọi service để cập nhật thông tin nhóm
//	        conversationService.updateGroupInfo(phone, conversationId, conversationName, imageUrl);
//	        return ResponseEntity.ok("Group updated successfully");
//
//	    } catch (RuntimeException e) {
//	        log.error("Failed to update group info: {}", e.getMessage());
//	        if (e.getMessage().equals("Conversation not found")) {
//	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Conversation not found");
//	        }
//	        if (e.getMessage().equals("Only admin or leader can update group info")) {
//	            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admin or leader can update group info");
//	        }
//	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update group info");
//	    } catch (Exception e) {
//	        log.error("Unexpected error: {}", e.getMessage());
//	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred");
//	    }
//	}

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
