package iuh.fit.se.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import iuh.fit.se.model.dto.UserResponseDto;
import iuh.fit.se.model.dto.message.MessageRequestDTO;
import iuh.fit.se.model.dto.message.MessageResponseDTO;
import iuh.fit.se.service.MessageService;
import iuh.fit.se.service.UserService;
import iuh.fit.se.util.JwtUtils;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;
    private final UserService userService;
    private final JwtUtils jwtUtils;

    private String getCurrentUserPhone(String authHeader) {
        String jwt = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        return jwtUtils.getPhoneFromToken(jwt);
    }

   
    
    @PostMapping("/text")
    public ResponseEntity<MessageResponseDTO> sendTextMessage(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody MessageRequestDTO request) {
        String phone = getCurrentUserPhone(authHeader);
        request.setSenderId(phone);
        return ResponseEntity.ok(messageService.sendTextMessage(request));
    }

    // Gửi tin nhắn media (ảnh/video)
    @PostMapping(value = "/media", consumes = "multipart/form-data")
/**
 * 
 * @param authHeader cái này là JWT
 * @param request là cái body của request, lúc gửi phải để theo định dang: {..., "request": {...}, mediaFile: file}
 * @param mediaFile là file đính kèm
 * @return
 */
    public ResponseEntity<MessageResponseDTO> sendMediaMessage(
            @RequestHeader("Authorization") String authHeader,
            @RequestPart MessageRequestDTO request,
            @RequestPart MultipartFile mediaFile) {
        String phone = getCurrentUserPhone(authHeader);
        request.setSenderId(phone);
        return ResponseEntity.ok(messageService.sendMediaMessage(request, mediaFile));
    }

    // Gửi file đính kèm
    @PostMapping(value = "/file", consumes = "multipart/form-data")
    public ResponseEntity<MessageResponseDTO> sendFileMessage(
            @RequestHeader("Authorization") String authHeader,
            @RequestPart MessageRequestDTO request,
            @RequestPart MultipartFile file) {
        String phone = getCurrentUserPhone(authHeader);
        request.setSenderId(phone);
        return ResponseEntity.ok(messageService.sendFileMessage(request, file));
    }

    // Gửi sự kiện cuộc gọi
    @PostMapping("/call-event")
    public ResponseEntity<MessageResponseDTO> sendCallEventMessage(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody MessageRequestDTO request) {
        String phone = getCurrentUserPhone(authHeader);
        request.setSenderId(phone);
        return ResponseEntity.ok(messageService.sendCallEventMessage(request));
    }

    // Thu hồi tin nhắn
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> recallMessage(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String messageId) {
        String phone = getCurrentUserPhone(authHeader);
        messageService.recallMessage(messageId, phone);
        return ResponseEntity.ok().build();
    }

    // Thêm reaction vào tin nhắn
    @PostMapping("/{messageId}/reactions")
    public ResponseEntity<MessageResponseDTO> addReaction(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String messageId,
            @RequestParam String emoji) {
        String phone = getCurrentUserPhone(authHeader);
        return ResponseEntity.ok(messageService.reactToMessage(messageId, phone, emoji));
    }

    // Lấy danh sách tin nhắn trong conversation
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<MessageResponseDTO>> getConversationMessages(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String conversationId) {
        String phone = getCurrentUserPhone(authHeader);
        // Kiểm tra xem người dùng có quyền truy cập vào conversation này không
        // Có thể thêm kiểm tra user có trong conversation không
        
        return ResponseEntity.ok(messageService.getMessagesByConversation(conversationId));
    }

    // Lấy chi tiết 1 tin nhắn
    @GetMapping("/{messageId}")
    public ResponseEntity<MessageResponseDTO> getMessageDetails(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String messageId) {
        String phone = getCurrentUserPhone(authHeader);
        return ResponseEntity.ok(messageService.getMessageById(messageId));
    }
}