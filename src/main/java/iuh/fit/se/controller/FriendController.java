package iuh.fit.se.controller;

import java.util.List;

import iuh.fit.se.model.dto.search.FindPeopleByNameKeywordResponse;
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

import iuh.fit.se.service.FriendService;

import iuh.fit.se.util.JwtUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
public class FriendController {
		
	ConversationService conversationService;
	JwtUtils jwtUtils;
	UserService userService;	
	FriendService friendService;
	
	private record PhoneObj (String phone){
		
	}
	
	@PostMapping("/accept")
	public ResponseEntity<Void> addFriend(@RequestHeader("Authorization") String authHeader, @RequestBody PhoneObj friendPhone) {
		// Kiểm tra header và loại bỏ prefix "Bearer " nếu có
		String jwt = authHeader.substring(7);
		
		// Lấy phone từ token
		String phone = jwtUtils.getPhoneFromToken(jwt);
		
		userService.acceptRequest(phone, friendPhone.phone);
		conversationService.createFriendConversation(phone, friendPhone.phone);
		
		return ResponseEntity.ok().build();
	}
	
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

}
