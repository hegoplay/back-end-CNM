package iuh.fit.se.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import iuh.fit.se.model.dto.call.CallResponseDto;
import iuh.fit.se.service.CallService;
import iuh.fit.se.service.ConversationService;
import iuh.fit.se.service.MessageNotifier;
import iuh.fit.se.util.JwtUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/call")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
public class CallController {
	
	CallService callService;;

    @GetMapping("/{callId}")
    public ResponseEntity<CallResponseDto> getCallDetail(@PathVariable String callId, @RequestHeader("Authorization") String authHeader) {
		CallResponseDto callDetail = callService.findById(callId);
		if (callDetail == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		return ResponseEntity.ok(callDetail);
	}
    

    
}