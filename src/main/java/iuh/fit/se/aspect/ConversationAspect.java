package iuh.fit.se.aspect;

import java.time.LocalDateTime;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import iuh.fit.se.model.Conversation;
import iuh.fit.se.model.dto.message.MessageRequestDTO;
import iuh.fit.se.model.dto.message.MessageResponseDTO;
import iuh.fit.se.repo.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ConversationAspect {

    private final ConversationRepository conversationRepository;

    /**
     * Cập nhật updatedAt của conversation sau khi các phương thức được đánh dấu bởi @UpdateConversation được gọi thành công.
     */
    @AfterReturning(
            pointcut = "@annotation(iuh.fit.se.annotation.UpdateConversation)",
            returning = "result"
    )
    public void updateConversationUpdatedAt(JoinPoint joinPoint, Object result) {
        log.info("Aspect triggered for method: {}", joinPoint.getSignature().getName());

        String conversationId = null;

        // Nếu kết quả trả về là MessageResponseDTO, lấy conversationId từ đó
        if (result instanceof MessageResponseDTO messageResponseDTO) {
            conversationId = messageResponseDTO.getConversationId();
        }

        // Nếu conversationId không tìm thấy trong result, kiểm tra tham số
        if (conversationId == null) {
            for (Object arg : joinPoint.getArgs()) {
                if (arg instanceof MessageRequestDTO requestDTO) {
                    conversationId = requestDTO.getConversationId();
                    break;
                } else if (arg instanceof String && conversationRepository.existsById((String) arg)) {
                    conversationId = (String) arg;
                    break;
                }
            }
        }

        // Cập nhật updatedAt nếu tìm thấy conversationId
        if (conversationId != null) {
            log.info("Updating updatedAt for conversation: {}", conversationId);
            // Tìm kiếm conversation trong cơ sở dữ liệu
            Conversation conversation = conversationRepository.findById(conversationId);
            if (conversation != null) {
                conversation.setUpdatedAt(LocalDateTime.now());
                conversationRepository.save(conversation);
            } else {
                log.warn("Conversation not found: {}", conversationId);
            }
        } else {
            log.warn("No conversationId found for method: {}", joinPoint.getSignature().getName());
        }
    }
}