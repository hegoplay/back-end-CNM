package iuh.fit.se.serviceImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.corundumstudio.socketio.SocketIOServer;

import iuh.fit.se.model.User;
import iuh.fit.se.model.dto.conversation.ConversationDto;
import iuh.fit.se.repo.MessageRepository;
import iuh.fit.se.repo.UserRepository;
import iuh.fit.se.service.ConversationService;
import iuh.fit.se.util.JwtUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SocketIOService {

    private final SocketIOServer socketIOServer;
    private final UserRepository userRepository;
    private final ConversationService conversationService;
    private final MessageRepository messageRepository;
    private final SocketIONotifier notifier;
    private final JwtUtils jwtUtils;

    @PostConstruct
    public void start() {
        com.corundumstudio.socketio.SocketIONamespace chatNamespace = socketIOServer.addNamespace("/chat");

        chatNamespace.addConnectListener(client -> {
            String token = client.getHandshakeData().getSingleUrlParam("token");
            try {
                String username = jwtUtils.getPhoneFromToken(token);
                client.set("username", username);
                log.info("Client connected to chat namespace: {} - {}", client.getSessionId(), username);

                notifier.registerClient(username, client);

                Optional<User> userOpt = Optional.ofNullable(userRepository.findByPhone(username));
                userOpt.ifPresentOrElse(user -> {
                    List<String> conversationIds = user.getConversations();
                    if (conversationIds != null) {
                        conversationIds.forEach(conv -> client.joinRoom(conv));
                    }

                    // Gửi danh sách hội thoại
                    List<ConversationDto> conversations = conversationService.getConversations(username);
                    client.sendEvent("initial_conversations", conversations);

                    // Đếm số tin nhắn chưa đọc
                    Map<String, Integer> unreadCountMap = new HashMap<>();
                    for (ConversationDto conv : conversations) {
                        int unreadCount = messageRepository.countUnreadMessages(conv.getId(), username);
                        if (unreadCount > 0) {
                            unreadCountMap.put(conv.getId(), unreadCount);
                        }
                    }

                    // Gửi unread_counts
                    notifier.notifyUnreadCounts(username, unreadCountMap);

                }, () -> {
                    log.error("User not found: {}", username);
                    client.sendEvent("auth_error", "USER_NOT_FOUND");
                    client.disconnect();
                });

            } catch (Exception e) {
                log.error("Authentication failed: {}", e.getMessage());
                client.sendEvent("auth_error", "INVALID_TOKEN");
                client.disconnect();
            }
        });

        chatNamespace.addDisconnectListener(client -> {
            String username = client.get("username");
            if (username != null) {
                notifier.removeClient(username);
                log.info("Client disconnected: {} - {}", client.getSessionId(), username);
            }
        });

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