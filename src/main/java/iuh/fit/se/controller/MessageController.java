package iuh.fit.se.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import iuh.fit.se.model.dto.message.MessageRequestDTO;
import iuh.fit.se.model.dto.message.MessageResponseDTO;
import iuh.fit.se.model.dto.message.ReactionRequestDTO;
import iuh.fit.se.service.MessageService;
import iuh.fit.se.service.UserService;
import iuh.fit.se.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
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
/**
 * 
 * @param authHeader cái này là JWT
 * @param request là cái body của request, lúc gửi phải để theo định dang: {..., "request": {...}, mediaFile: file}
 * @param mediaFile là file đính kèm
 * @return
 */
    /**
     * 
     * @param authHeader cái này là JWT
     * @param request là cái body của request, lúc gửi phải để theo định dang: {..., "request": {...}, mediaFile: file}
     * @param mediaFile là file đính kèm (ảnh/video/..)
     * mediaType sẽ cần phải được chỉnh ở phía client
     * @return
     */
    // Gửi file đính kèm
    @PostMapping(value = "/file", consumes = "multipart/form-data")
    public ResponseEntity<MessageResponseDTO> sendFileMessage(
            @RequestHeader(value = "Authorization") String authHeader,
            @RequestPart(value = "request") MessageRequestDTO request,
            @RequestPart(value = "file") MultipartFile file) {
        try {
            log.info("Bắt đầu xử lý yêu cầu gửi tệp");

            // Kiểm tra authHeader
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.error("Header Authorization không hợp lệ: {}", authHeader);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Header Authorization không hợp lệ");
            }

            // Kiểm tra file
            if (file == null || file.isEmpty()) {
                log.error("Tệp rỗng hoặc không hợp lệ");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tệp rỗng hoặc không hợp lệ");
            }
            log.info("File nhận được: name={}, size={}", file.getOriginalFilename(), file.getSize());

            // Kiểm tra request
            if (request == null || request.getConversationId() == null || request.getType() == null) {
                log.error("Request không hợp lệ: {}", request);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request không hợp lệ hoặc thiếu thông tin");
            }
            log.info("Request nhận được: {}", request);

            // Lấy số điện thoại từ token
            log.info("Xử lý authHeader: {}", authHeader);
            String phone;
            try {
                phone = getCurrentUserPhone(authHeader);
                log.info("Phone extracted: {}", phone);
            } catch (Exception e) {
                log.error("Lỗi khi lấy phone từ token: {}", e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không thể lấy thông tin người dùng từ token", e);
            }

            // Gán senderId và gọi service
            request.setSenderId(phone);
            log.info("Gửi tệp đính kèm với senderId: {}", phone);
            MessageResponseDTO response = messageService.sendFileMessage(request, file);
            
            log.info("Gửi tệp thành công: response={}", response);

            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            log.error("Lỗi xử lý yêu cầu: status={}, message={}", e.getStatusCode(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Lỗi server không xác định: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi server không xác định", e);
        }
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
            @RequestBody ReactionRequestDTO reactionRequest) {
        String phone = getCurrentUserPhone(authHeader);
        return ResponseEntity.ok(messageService.reactToMessage(messageId, phone, reactionRequest.getEmoji()));
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

    
}