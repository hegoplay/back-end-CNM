package iuh.fit.se.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import iuh.fit.se.model.dto.LoginRequest;
import iuh.fit.se.model.dto.LoginResponse;
import iuh.fit.se.model.dto.RegisterRequest;
import iuh.fit.se.model.dto.RegisterResponse;
import iuh.fit.se.service.AwsService;
import iuh.fit.se.service.UserService;
import iuh.fit.se.util.JwtUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
public class AuthController {

//    AuthenticationManager authenticationManager;

    JwtUtils jwtUtils;
    AwsService awsService;
    UserService userService;
    

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        String token = jwtUtils.generateTokenFromUsername(loginRequest.getPhone());
        log.debug("Token: {}", token);
        return ResponseEntity.ok(new LoginResponse(loginRequest.getPhone(), token));
    }
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Boolean>> sendOtp(@RequestBody LoginRequest request) {
//        log.info("OTP sent: {}", request.getPhone());
        Map<String, Boolean> response = new HashMap<>();
        awsService.sendOtp(request.getPhone());
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> request) {
    	log.info("verify otp: {}", request);
        String phone = request.get("phone");
        String otp = request.get("otp");
        Map<String, Object> response = new HashMap<>();
        boolean verifyOtp = awsService.verifyOtp(phone, otp);
        log.info("verify otp: {}", verifyOtp);
        if (verifyOtp) {
        	response.put("success", true);
        	response.put("message", "veryfy otp success");
		} else {
			response.put("success", false);
			response.put("message", "veryfy otp failed");
		}
        return ResponseEntity.ok(response);
    }
    
    
    @PostMapping("/check-phone")
    public ResponseEntity<Map<String, Boolean>> checkPhone(@RequestBody Map<String, String> request) {
		String phone = request.get("phone");
		Map<String, Boolean> response = new HashMap<>();
		boolean isExist = userService.isExistPhone(phone);
		log.info("check phone: {}", isExist);
		response.put("isExist", isExist);
		return ResponseEntity.ok(response);
	}
    
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@ModelAttribute RegisterRequest request) throws Exception {
    	log.info(request.toString());
        String avatarUrl = awsService.uploadToS3(request.getAvatar());
        userService.createUser(request, avatarUrl);
        RegisterResponse response = new RegisterResponse();
        response.setSuccess(true);
        response.setMessage("Đăng ký thành công");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/login-with-password")
    public ResponseEntity<LoginResponse> loginWithPassword(@RequestBody LoginRequest loginRequest) {
//		String token = jwtUtils.generateTokenFromUsername(loginRequest.getPhone());
    	LoginResponse loginResponse = userService.login(loginRequest);
		if (loginResponse == null) {
			return ResponseEntity.badRequest().body(null);
		}
		log.info("Login with password: {}", loginRequest.getPhone());
		return ResponseEntity.ok(loginResponse);
	}
    
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Boolean>> logout(@RequestBody Map<String, String> request) {
		log.info("Logout: {}", request.get("phone"));
		Map<String, Boolean> response = new HashMap<>();
		response.put("success", true);
		return ResponseEntity.ok(response);
	}
    
}
