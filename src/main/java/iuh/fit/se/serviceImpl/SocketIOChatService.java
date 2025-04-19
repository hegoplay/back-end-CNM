package iuh.fit.se.serviceImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;

import iuh.fit.se.model.Call;
import iuh.fit.se.model.User;
import iuh.fit.se.model.dto.call.StartCallDto;
import iuh.fit.se.model.dto.conversation.ConversationDto;
import iuh.fit.se.model.dto.message.MessageRequestDTO;
import iuh.fit.se.model.enumObj.MessageType;
import iuh.fit.se.repo.MessageRepository;
import iuh.fit.se.repo.UserRepository;
import iuh.fit.se.service.CallService;
import iuh.fit.se.service.ConversationService;
import iuh.fit.se.service.MessageNotifier;
import iuh.fit.se.service.MessageService;
import iuh.fit.se.util.JwtUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SocketIOChatService {

	private final SocketIOServer socketIOServer;
	private final UserRepository userRepository;
	private final ConversationService conversationService;
	private final MessageRepository messageRepository;
	private final MessageNotifier messageNotifier;
	private final JwtUtils jwtUtils;
	private final CallService callService;
	private final MessageService messageService;
	private final SocketIOCallService socketIOCallService;
	
	@PostConstruct
	public void start() {
		SocketIONamespace chatNamespace = socketIOServer.addNamespace("/chat");

		chatNamespace.addConnectListener(client -> {
			String token = client.getHandshakeData().getSingleUrlParam("token");
			try {
				String username = jwtUtils.getPhoneFromToken(token);
				client.set("username", username);
				messageNotifier.registerClient(username, client);

				Optional<User> userOpt = Optional
						.ofNullable(userRepository.findByPhone(username));
				userOpt.ifPresentOrElse(user -> {
					List<String> conversationIds = user.getConversations();
					if (conversationIds != null) {
						conversationIds.forEach(conv -> client.joinRoom(conv));
					}

					// Gửi danh sách hội thoại
					List<ConversationDto> conversations = conversationService
							.getConversations(username);
					client.sendEvent("initial_conversations", conversations);

					// Đếm số tin nhắn chưa đọc
					Map<String, Integer> unreadCountMap = new HashMap<>();
					for (ConversationDto conv : conversations) {
						int unreadCount = messageRepository
								.countUnreadMessages(conv.getId(), username);
						if (unreadCount > 0) {
							unreadCountMap.put(conv.getId(), unreadCount);
						}
					}

					// Gửi unread_counts
					client.sendEvent("unread_counts", unreadCountMap);

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
				log.info("Client disconnected: {} - {}", client.getSessionId(),
						username);
			}
			messageNotifier.removeClient(username);
		});

		// Xử lý sự kiện bắt đầu cuộc gọi video
		chatNamespace.addEventListener("start_call", StartCallDto.class,
				(client, startCallDto, ackSender) -> {
					String username = client.get("username");
					String conversationId = startCallDto.getConversationId();

					if (username == null) {
						client.sendEvent("call_error", "NOT_AUTHENTICATED");
						return;
					}

					Call call = callService.createCall(startCallDto.getCallId(),username, conversationId, startCallDto.getCallType());

					// Gửi lời mời gọi video đến tất cả participants

					messageService.sendCallEventMessage(MessageRequestDTO.builder()
							.conversationId(conversationId)
							.senderId(username)
							.type(MessageType.CALL)
							.callId(startCallDto.getCallId())
							.build());
					
					messageNotifier.sendCallInvitation(conversationId, "video",
							username);


					
//					luu thông tin callId vào room 
					socketIOCallService.storeCallMapping(call.getId(), call.getId(), conversationId);
					
					log.info("Call started by {} in conversation {}", username,
							conversationId);
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

	// Helper method để tìm client theo username
	private SocketIOClient findClientByUsername(String username) {
		for (SocketIOClient client : socketIOServer.getNamespace("/chat")
				.getAllClients()) {
			if (username.equals(client.get("username"))) {
				return client;
			}
		}
		return null;
	}
}
