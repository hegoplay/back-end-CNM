package iuh.fit.se.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import iuh.fit.se.model.dto.UserResponseDto;
import iuh.fit.se.service.ConversationService;
import iuh.fit.se.service.FriendService;
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

	FriendService friendService;
	UserService userService;
	ConversationService conversationService;
	JwtUtils jwtUtils;
	
	private record PhoneObj (String phone){
		
	}
	
//	@PostMapping("/accept")
//	public ResponseEntity<Void> addFriend(@RequestHeader("Authorization") String authHeader, @RequestBody PhoneObj friendPhone) {
//		// Kiểm tra header và loại bỏ prefix "Bearer " nếu có
//		String jwt = authHeader.substring(7);
//		
//		// Lấy phone từ token
//		String phone = jwtUtils.getPhoneFromToken(jwt);
//		
//		userService.acceptRequest(phone, friendPhone.phone);
//		conversationService.createFriendConversation(phone, friendPhone.phone);
//		
//		return ResponseEntity.ok().build();
//	}
	
//	@GetMapping("/")
//	public ResponseEntity<List<UserResponseDto>> getFriends(@RequestHeader("Authorization") String authHeader) {
//
//    FriendService friendService;
//    JwtUtils jwtUtils;


    @PostMapping("/try-to-search")
    public ResponseEntity<Map<String, Object>> tryToSearch(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> requestBody) {
//        {
//            anything
//        }
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

        List<UserResponseDto> response = friendService.getFriendsList(userPhone);
        log.info("GetFriendsList: {}", response);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/find-person-by-phone")
    public ResponseEntity<List<UserResponseDto>> findPersonsByPhone(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
//    {
//        "phone" : "+9999999999"
//    }
        String phone = request.get("phone");
        log.info("Finding users with phone: {}", phone);


        // Validate input
        if (phone == null || phone.trim().isEmpty()) {
            log.warn("Empty or null phone provided");
            return ResponseEntity.badRequest().build();
        }

        // Call friendService to find users
        List<UserResponseDto> response = friendService.findPersonByPhone(phone.trim());

        if (response == null || response.isEmpty()) {
            log.info("No users found for phone: {}", phone);
            return ResponseEntity.notFound().build();
        }

        log.info("Returning {} users for phone: {}", response.size(), phone);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/find-friends-by-name-keyword")
    public ResponseEntity<List<UserResponseDto>> findFriendsByNameKeyword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request){
//        {
//            "nameKeyword" : "Manh"
//        }
        String jwt = authHeader.substring(7);
        String userPhone = jwtUtils.getPhoneFromToken(jwt);

        String nameKeyword = request.get("nameKeyword");
        log.info("Finding users with nameKeyword: {}", nameKeyword);


        //Call friendService to find users
        List<UserResponseDto> response = friendService.findFriendsByName(userPhone, nameKeyword);

        if (response == null || response.isEmpty()) {
            log.info("No users found for nameKeyword: {}", nameKeyword);
            return ResponseEntity.notFound().build();
        }
        log.info("Returning {} users for phone: {}", response.size(), userPhone);
        return ResponseEntity.ok(response);

    }

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
			conversationService.createFriendConversation(receiverPhoneNumber, senderPhoneNumber);
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
