package iuh.fit.se.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import iuh.fit.se.model.dto.auth.ForgotPasswordRequest;
import iuh.fit.se.model.dto.auth.LoginRequest;
import iuh.fit.se.model.dto.auth.LoginResponse;
import iuh.fit.se.model.dto.auth.PhoneRequest;
import iuh.fit.se.model.dto.auth.RegisterRequest;
import iuh.fit.se.model.dto.auth.RegisterResponse;
import iuh.fit.se.service.UserService;
import iuh.fit.se.serviceImpl.AwsService;
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

    private record PhoneObj (String phone){

    }

    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Kết nối thành công!");
        log.info("Connection test");
        return ResponseEntity.ok(response);
    }


    /**
     *
     * @param loginRequest
     * phone
     * password
     * @return
     *  phone
     *  token = jwt
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
//        String token = jwtUtils.generateTokenFromUsername(loginRequest.getPhone());
        log.info("Login request: {}", loginRequest);
        return ResponseEntity.ok(userService.login(loginRequest));
    }
    /**
     *
     * @param request
     * phone
     * @return
     * success or not
     */
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Boolean>> sendOtp(@RequestBody String phone) {
//        log.info("OTP sent: {}", request.getPhone());
        Map<String, Boolean> response = new HashMap<>();
        awsService.sendOtp(phone);
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
    /**
     *
     * @param request
     * phone
     * otp
     * @return
     * success: true or false
     * message: verify otp success or failed
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> request) {
//    	log.info("verify otp: {}", request);
        String phone = request.get("phone");
        String otp = request.get("otp");
        Map<String, Object> response = new HashMap<>();
        boolean verifyOtp = awsService.verifyOtp(phone, otp);
//        log.info("verify otp: {}", verifyOtp);
        if (verifyOtp) {
            response.put("success", true);
            response.put("message", "veryfy otp success");
        } else {
            response.put("success", false);
            response.put("message", "veryfy otp failed");
        }
        return ResponseEntity.ok(response);
    }


    
/**
 *     
 * @param phone
 * @return
 * isExist
 */
    

    @PostMapping("/check-phone")
    public ResponseEntity<Map<String, Boolean>> checkPhone(@RequestBody PhoneRequest request) {
        String cleanPhone = request.getPhone().replace("\"", "").trim();
        boolean exists = userService.isExistPhone(cleanPhone);
        return ResponseEntity.ok(Map.of("isExist", exists));
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

    @PostMapping(path = {"/forgot-password"})
    public ResponseEntity<Map<String, Boolean>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        userService.updatePassword(request.getPhone(), request.getPassword());
        log.info("Update password: {}", request.getPhone());
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()") // Yêu cầu user phải đăng nhập
    public ResponseEntity<Map<String, Boolean>> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ForgotPasswordRequest request) {
        // Kiểm tra header cơ bản
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String jwt = authHeader.substring(7);

        // Gọi service
        userService.changePassword(jwt, request.getPhone(), request.getPassword());

        // Trả về response thành công
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("Authorization") String authHeader) {
        // Kiểm tra header và loại bỏ prefix "Bearer " nếu có
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).build(); // 401 nếu header không hợp lệ
        }

        // Lấy JWT bằng cách loại bỏ "Bearer " prefix
        String jwt = authHeader.substring(7);

        // Lấy phone từ token
        String phone = jwtUtils.getPhoneFromToken(jwt);
        userService.logout(phone);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Logout success");

        return ResponseEntity.ok(response); // 200 với data
    }

}