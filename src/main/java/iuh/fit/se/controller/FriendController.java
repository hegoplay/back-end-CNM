package iuh.fit.se.controller;

import iuh.fit.se.service.FriendService;
import iuh.fit.se.service.UserService;
import iuh.fit.se.util.JwtUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/friends")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
public class FriendController {

	FriendService friendService;
	UserService userService;
	JwtUtils jwtUtils;

	// Endpoint tạo token (tạm thời)
	@GetMapping("/generate-token")
	public ResponseEntity<String> generateToken(@RequestParam String phoneNumber) {
		String token = jwtUtils.generateTokenFromPhone(phoneNumber);
		return ResponseEntity.ok(token);
	}

	// gửi lời mời kết bạn
	@PostMapping("/send-request")
	public ResponseEntity<String> sendFriendRequest(@RequestHeader("Authorization") String authorizationHeader,
			@RequestParam String receiverPhoneNumber) {
		try {
			String senderPhoneNumber = extractPhoneNumberFromToken(authorizationHeader);
			log.info("User {} is sending friend request to {}", senderPhoneNumber, receiverPhoneNumber);
			friendService.sendFriendRequest(senderPhoneNumber, receiverPhoneNumber);
			return ResponseEntity.ok("Friend request sent successfully");
		} catch (Exception e) {
			log.error("Error sending friend request: {}", e.getMessage());
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	// chấp nhận lời mời kết bạn
	@PostMapping("/accept-request")
	public ResponseEntity<String> acceptFriendRequest(@RequestHeader("Authorization") String authorizationHeader,
			@RequestParam String senderPhoneNumber) {
		try {
			String receiverPhoneNumber = extractPhoneNumberFromToken(authorizationHeader);
			log.info("User {} is accepting friend request from {}", receiverPhoneNumber, senderPhoneNumber);
			friendService.acceptFriendRequest(receiverPhoneNumber, senderPhoneNumber);
			return ResponseEntity.ok("Friend request accepted successfully");
		} catch (Exception e) {
			log.error("Error accepting friend request: {}", e.getMessage());
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	// Người gửi huỷ lời mời kết bạn
	@DeleteMapping("/cancel-request")
	public ResponseEntity<String> cancelFriendRequest(@RequestHeader("Authorization") String authorizationHeader,
			@RequestParam String receiverPhoneNumber) {
		try {
			String senderPhoneNumber = extractPhoneNumberFromToken(authorizationHeader);
			log.info("User {} is canceling friend request to {}", senderPhoneNumber, receiverPhoneNumber);
			friendService.cancelFriendRequest(senderPhoneNumber, receiverPhoneNumber);
			return ResponseEntity.ok("Friend request canceled successfully");
		} catch (Exception e) {
			log.error("Error canceling friend request: {}", e.getMessage());
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	// Từ chối lời mời kết bạn
	@DeleteMapping("/reject-request")
	public ResponseEntity<String> rejectFriendRequest(@RequestHeader("Authorization") String authorizationHeader,
			@RequestParam String senderPhoneNumber) {
		try {
			// Lấy số điện thoại người nhận từ Token
			String receiverPhoneNumber = extractPhoneNumberFromToken(authorizationHeader);
			log.info("User {} is rejecting friend request from {}", receiverPhoneNumber, senderPhoneNumber);

			// Gọi service xử lý từ chối
			friendService.rejectFriendRequest(receiverPhoneNumber, senderPhoneNumber);

			return ResponseEntity.ok("Friend request rejected successfully");
		} catch (Exception e) {
			log.error("Error rejecting friend request: {}", e.getMessage());
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	// hủy kết bạn
	@DeleteMapping("/remove-friend")
	public ResponseEntity<String> removeFriend(@RequestHeader("Authorization") String authorizationHeader,
			@RequestParam String friendPhoneNumber) {
		try {
			String userPhoneNumber = extractPhoneNumberFromToken(authorizationHeader);
			log.info("User {} is removing friend {}", userPhoneNumber, friendPhoneNumber);
			friendService.removeFriend(userPhoneNumber, friendPhoneNumber);
			return ResponseEntity.ok("Friend removed successfully");
		} catch (Exception e) {
			log.error("Error removing friend: {}", e.getMessage());
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

//    private String extractPhoneNumberFromToken(String authorizationHeader) {
//        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
//            throw new RuntimeException("Invalid or missing Authorization header");
//        }
//        String token = authorizationHeader.substring(7);
//        String phoneNumber = jwtUtils.getPhoneFromToken(token); 
//        if (phoneNumber == null) {
//            throw new RuntimeException("Invalid token: phoneNumber not found");
//        }
//        return phoneNumber;
//    }
	private String extractPhoneNumberFromToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			log.error("Invalid or missing Authorization header: {}", authorizationHeader);
			throw new RuntimeException("Invalid or missing Authorization header");
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

}