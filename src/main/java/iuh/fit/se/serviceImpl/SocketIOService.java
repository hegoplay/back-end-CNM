package iuh.fit.se.serviceImpl;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import iuh.fit.se.model.Conversation;
import iuh.fit.se.model.User;
import iuh.fit.se.model.dto.conversation.ConversationDetailDto;
import iuh.fit.se.model.dto.conversation.ConversationDto;
import iuh.fit.se.model.dto.message.*;
import iuh.fit.se.repo.ConversationRepository;
import iuh.fit.se.repo.UserRepository;
import iuh.fit.se.service.ConversationService;
import iuh.fit.se.service.MessageService;
import iuh.fit.se.util.JwtUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SocketIOService {

    private final SocketIOServer socketIOServer;
    private final UserRepository userRepository;
    private final ConversationService conversationService;
    private final JwtUtils jwtUtils;

    @PostConstruct
    public void start() {
        // Tạo namespace /chat
        com.corundumstudio.socketio.SocketIONamespace chatNamespace = socketIOServer.addNamespace("/chat");
        
        // Xử lý kết nối trong namespace
        chatNamespace.addConnectListener(client -> {
            String token = client.getHandshakeData().getSingleUrlParam("token");
            try {
                String username = jwtUtils.getPhoneFromToken(token);
                client.set("username", username);
                log.info("Client connected to chat namespace: {} - {}", client.getSessionId(), username);

                // Load và gửi danh sách conversation
                Optional<User> userOpt = Optional.of(userRepository.findByPhone(username));
                userOpt.ifPresentOrElse(
                    user -> {
                        user.getConversations().forEach(conv -> client.joinRoom(conv));
                        List<ConversationDto> conversations = conversationService.getConversations(username);
//                      load danh sách conversationrge
                        client.sendEvent("initial_conversations", conversations);
                    },
                    () -> {
                        log.error("User not found: {}", username);
                        client.sendEvent("auth_error", "USER_NOT_FOUND");
                        client.disconnect();
                    }
                );
                
                
            } catch (Exception e) {
                log.error("Authentication failed: {}", e.getMessage());
                client.sendEvent("auth_error", "INVALID_TOKEN");
                client.disconnect();
            }
        });

        // Đăng ký các sự kiện trong namespace
//        registerChatEvents(chatNamespace);
        
        socketIOServer.start();
        log.info("Socket.IO server started with /chat namespace");
    }


    @PreDestroy
    public void stop() {
        if (socketIOServer != null) {
            socketIOServer.stop();
            log.info("Socket.IO server stopped");
        }
    }
}