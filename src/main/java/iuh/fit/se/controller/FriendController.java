package iuh.fit.se.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import iuh.fit.se.model.dto.search.FindPeopleByNameKeywordResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import iuh.fit.se.model.dto.UserResponseDto;
import iuh.fit.se.model.dto.friend.SendFriendRequestDto;
import iuh.fit.se.service.ConversationService;
import iuh.fit.se.service.FriendService;
import iuh.fit.se.service.UserService;
import iuh.fit.se.util.FormatUtils;
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

	FriendService friendService;
	UserService userService;
	ConversationService conversationService;
	JwtUtils jwtUtils;

	private record PhoneObj(String phone) {

	}

	@PostMapping("/try-to-search")
	public ResponseEntity<Map<String, Object>> tryToSearch(
			@RequestHeader("Authorization") String authHeader,
			@RequestBody Map<String, Object> requestBody) {
		// {
		// anything
		// }
		log.info("Auth Header: {}", authHeader);
		log.info("Received request body: {}", requestBody);

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("message", "Search successful!");
		response.put("data", requestBody);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/get-friends-list")
	public ResponseEntity<List<UserResponseDto>> getFriendsList(
			@RequestHeader("Authorization") String authHeader) {
		String jwt = authHeader.substring(7);
		String userPhone = jwtUtils.getPhoneFromToken(jwt);

		List<UserResponseDto> response = friendService
				.getFriendsList(userPhone);
		log.info("GetFriendsList: {}", response);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/find-person-by-phone")
	public ResponseEntity<List<UserResponseDto>> findPersonsByPhone(
			@RequestHeader("Authorization") String authHeader,
			@RequestBody Map<String, String> request) {
		// {
		// "phone" : "+9999999999"
		// }
		String phone = request.get("phone");
		log.info("Finding users with phone: {}", phone);

		// Validate input
		if (phone == null || phone.trim().isEmpty()) {
			log.warn("Empty or null phone provided");
			return ResponseEntity.badRequest().build();
		}

		// Call friendService to find users
		List<UserResponseDto> response = friendService
				.findPersonByPhone(phone.trim());

		if (response == null || response.isEmpty()) {
			log.info("No users found for phone: {}", phone);
			return ResponseEntity.notFound().build();
		}

		log.info("Returning {} users for phone: {}", response.size(), phone);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/find-people-by-name-keyword")
    public ResponseEntity<FindPeopleByNameKeywordResponse> findPeopleByNameKeyword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
//    {
//        "nameKeyword" : "Manh"
//    }
        String jwt = authHeader.substring(7);
        String userPhone = jwtUtils.getPhoneFromToken(jwt);

        String nameKeyword = request.get("nameKeyword");
        log.info("Finding people with nameKeyword: {}", nameKeyword);

        // Call friendService to get combined response
        FindPeopleByNameKeywordResponse response = friendService.findPeopleByNameKeyword(userPhone, nameKeyword);

        // If result is null then return an empty object
        if (response == null ||
                (response.getFriends().isEmpty()
                        && response.getOthersWithSharedGroups().isEmpty()
                        && response.getContacted().isEmpty())) {
            log.info("No matching people found for nameKeyword: {}", nameKeyword);
            return ResponseEntity.notFound().build();
        }

        log.info("Returning people search results for phone: {}", userPhone);
        return ResponseEntity.ok(response);
    }

	// Endpoint tạo token (tạm thời)
	@GetMapping("/generate-token")
	public ResponseEntity<String> generateToken(
			@RequestParam String phoneNumber) {
		String token = jwtUtils.generateTokenFromPhone(phoneNumber);
		return ResponseEntity.ok(token);
	}

	// gửi lời mời kết bạn
	@PostMapping("/send-request")
	public ResponseEntity<String> sendFriendRequest(
			@RequestHeader("Authorization") String authorizationHeader,
			@RequestParam String receiverPhoneNumber) {
		try {
			String senderPhoneNumber = extractPhoneNumberFromToken(
					authorizationHeader);
			senderPhoneNumber = FormatUtils.formatPhoneNumber(senderPhoneNumber);
			log.info("User {} is sending friend request to {}",
					senderPhoneNumber, receiverPhoneNumber);
			friendService.sendFriendRequest(senderPhoneNumber,
					receiverPhoneNumber);
			return ResponseEntity.ok("Friend request sent successfully");
		} catch (Exception e) {
			log.error("Error sending friend request: {}", e.getMessage());
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	// chấp nhận lời mời kết bạn
	@PostMapping("/accept-request")
	public ResponseEntity<String> acceptFriendRequest(
			@RequestHeader("Authorization") String authorizationHeader,
			@RequestParam String senderPhoneNumber) {
		try {
			String receiverPhoneNumber = extractPhoneNumberFromToken(
					authorizationHeader);
			senderPhoneNumber = FormatUtils.formatPhoneNumber(senderPhoneNumber);
			log.info("User {} is accepting friend request from {}",
					receiverPhoneNumber, senderPhoneNumber);
			friendService.acceptFriendRequest(receiverPhoneNumber,
					senderPhoneNumber);
			
			conversationService.createFriendConversation(receiverPhoneNumber,
					senderPhoneNumber);
			return ResponseEntity.ok("Friend request accepted successfully");
		} catch (Exception e) {
			log.error("Error accepting friend request: {}", e.getMessage());
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	// Người gửi huỷ lời mời kết bạn
	@DeleteMapping("/cancel-request")
	public ResponseEntity<String> cancelFriendRequest(
			@RequestHeader("Authorization") String authorizationHeader,
			@RequestParam String receiverPhoneNumber) {
		try {
			String senderPhoneNumber = extractPhoneNumberFromToken(
					authorizationHeader);
			receiverPhoneNumber = FormatUtils.formatPhoneNumber(receiverPhoneNumber);
			log.info("User {} is canceling friend request to {}",
					senderPhoneNumber, receiverPhoneNumber);
			friendService.cancelFriendRequest(senderPhoneNumber,
					receiverPhoneNumber);
//			conversationService.deleteFriendConversation(senderPhoneNumber,
//					receiverPhoneNumber);
			return ResponseEntity.ok("Friend request canceled successfully");
		} catch (Exception e) {
			log.error("Error canceling friend request: {}", e.getMessage());
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	// Hủy kết bạn
	@DeleteMapping("/reject-request")
	public ResponseEntity<String> rejectFriendRequest(
			@RequestHeader("Authorization") String authorizationHeader,
			@RequestParam String senderPhoneNumber) {
		try {
			// Lấy số điện thoại người nhận từ Token
			String receiverPhoneNumber = extractPhoneNumberFromToken(
					authorizationHeader);
			senderPhoneNumber = FormatUtils.formatPhoneNumber(senderPhoneNumber);
			log.info("User {} is rejecting friend request from {}",
					receiverPhoneNumber, senderPhoneNumber);

			// Gọi service xử lý từ chối
			friendService.rejectFriendRequest(receiverPhoneNumber,
					senderPhoneNumber);
			// Xóa cuộc trò chuyện nếu có
			conversationService.deleteFriendConversation(receiverPhoneNumber,
					senderPhoneNumber);
			return ResponseEntity.ok("Friend request rejected successfully");
		} catch (Exception e) {
			log.error("Error rejecting friend request: {}", e.getMessage());
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	// hủy kết bạn
	@DeleteMapping("/remove-friend")
	public ResponseEntity<String> removeFriend(
			@RequestHeader("Authorization") String authorizationHeader,
			@RequestParam String friendPhoneNumber) {
		try {
			String userPhoneNumber = extractPhoneNumberFromToken(
					authorizationHeader);
			friendPhoneNumber = FormatUtils.formatPhoneNumber(friendPhoneNumber);
			log.info("User {} is removing friend {}", userPhoneNumber,
					friendPhoneNumber);
			friendService.removeFriend(userPhoneNumber, friendPhoneNumber);
			conversationService.deleteFriendConversation(userPhoneNumber,
					friendPhoneNumber);
			return ResponseEntity.ok("Friend removed successfully");
		} catch (Exception e) {
			log.error("Error removing friend: {}", e.getMessage());
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@GetMapping("/get-friend-requests")
	public ResponseEntity<List<UserResponseDto>> getFriendRequests(
			@RequestHeader("Authorization") String authorizationHeader) {
		try {
			String userPhoneNumber = extractPhoneNumberFromToken(
					authorizationHeader);
			log.info("User {} is getting friend requests", userPhoneNumber);
			List<UserResponseDto> friendRequests = friendService
					.getFriendRequests(userPhoneNumber);
			return ResponseEntity.ok(friendRequests);
		} catch (Exception e) {
			log.error("Error getting friend requests: {}", e.getMessage());
			return ResponseEntity.badRequest().body(null);
		}
	}

	private String extractPhoneNumberFromToken(String authorizationHeader) {
		if (authorizationHeader == null
				|| !authorizationHeader.startsWith("Bearer ")) {
			log.error("Invalid or missing Authorization header: {}",
					authorizationHeader);
			throw new RuntimeException(
					"Invalid or missing Authorization header");
		}
		String token = authorizationHeader.substring(7);
		log.info("Token received: {}", token);

		String phoneNumber = jwtUtils.getPhoneFromToken(token);
		if (phoneNumber == null) {
			log.error("Invalid token: phoneNumber not found");
			throw new RuntimeException("Invalid token: phoneNumber not found");
		}

		log.info("Phone number extracted from token: {}", phoneNumber);
		return phoneNumber;
	}

	@GetMapping("/check-request-status/{senderPhoneNumber}")
	public ResponseEntity<Map<String, Boolean>> checkRequestStatus(
			@RequestHeader("Authorization") String authorizationHeader,
			@PathVariable String senderPhoneNumber) {
		String userPhoneNumber = extractPhoneNumberFromToken(
				authorizationHeader);
		try {
			log.info("User {} is checking request status from {}",
					userPhoneNumber, senderPhoneNumber);
			boolean isPending = friendService.isRequestPending(userPhoneNumber,
					senderPhoneNumber);
			Map<String, Boolean> response = new HashMap<>();
			response.put("isPending", isPending);

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("Error checking request status: {}", e.getMessage());
			return ResponseEntity.badRequest().body(null);
		}
	}
	@GetMapping("/check-friend-status/{friendPhoneNumber}")
	public ResponseEntity<Map<String, Boolean>> checkFriendStatus(
			@RequestHeader("Authorization") String authorizationHeader,
			@PathVariable String friendPhoneNumber) {
		String userPhoneNumber = extractPhoneNumberFromToken(
				authorizationHeader);
		try {
			log.info("User {} is checking friend status with {}",
					userPhoneNumber, friendPhoneNumber);
			boolean isFriend = friendService.isFriend(userPhoneNumber,
					friendPhoneNumber);
			Map<String, Boolean> response = new HashMap<>();
			response.put("isFriend", isFriend);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("Error checking friend status: {}", e.getMessage());
			return ResponseEntity.badRequest().body(null);
		}
	}

}
