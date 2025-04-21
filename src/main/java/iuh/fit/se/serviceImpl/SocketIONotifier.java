package iuh.fit.se.serviceImpl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;

import iuh.fit.se.model.dto.UserResponseDto;
import iuh.fit.se.model.dto.conversation.ConversationDetailDto;
import iuh.fit.se.model.dto.conversation.ConversationDto;
import iuh.fit.se.model.dto.message.MessageResponseDTO;
import iuh.fit.se.service.MessageNotifier;
import iuh.fit.se.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocketIONotifier implements MessageNotifier {
	private final SocketIOServer socketIOServer;
	private final UserService userService;
	private final String NAMESPACE = "/chat";
	// Mapping userId -> SocketIOClient
	private final Map<String, SocketIOClient> userClientMap = new ConcurrentHashMap<>();

	// Lấy namespace đã khởi tạo
	private SocketIONamespace getChatNamespace() {
		return socketIOServer.getNamespace(NAMESPACE);
	}

	// Đăng ký client khi kết nối
	@Override
	public void registerClient(String userId, SocketIOClient client) {
		log.info("Registering client for userId: {}", userId);
		userClientMap.put(userId, client);
	}

	// Xóa client khi ngắt kết nối
	@Override
	public void removeClient(String userId) {
		log.info("Removing client for userId: {}", userId);
		userClientMap.remove(userId);
	}

	@Override
	public SocketIOClient getClient(String userId) {
		log.info("Getting client for userId: {}", userId);
		SocketIOClient client = userClientMap.get(userId);
		return client;
	}

	@Override
	public void notifyNewMessage(MessageResponseDTO message) {
		log.info("Notifying new message: conversationId = {}, messageId = {}",
				message.getConversationId(), message.getId());
		getChatNamespace().getRoomOperations(message.getConversationId())
				.sendEvent("new_message", message);
	}

	@Override
	public void notifyMessageRecalled(String conversationId, String messageId) {
		log.info(
				"Notifying message recalled: conversationId = {}, messageId = {}",
				conversationId, messageId);
		getChatNamespace().getRoomOperations(conversationId)
				.sendEvent("message_recalled", Map.of("messageId", messageId,
						"conversationId", conversationId));
	}

	@Override
	public void notifyReactionAdded(String conversationId, String messageId,
			String emoji, String userId) {
		log.info(
				"Notifying reaction added: conversationId = {}, messageId = {}, emoji = {}, userId = {}",
				conversationId, messageId, emoji, userId);
		getChatNamespace().getRoomOperations(conversationId).sendEvent(
				"reaction_added", Map.of("messageId", messageId, "emoji", emoji,
						"userId", userId, "conversationId", conversationId));
	}

	@Override
	public void initConversation(ConversationDetailDto conversationDetail,
			String userId) {
		log.info("Initializing conversation: conversationId = {}, userId = {}",
				conversationDetail.getId(), userId);
		SocketIOClient client = userClientMap.get(userId);
		log.info("Map: {}", userClientMap);
		if (client != null) {
			client.sendEvent("initial_messages", conversationDetail);
		} else {
			log.warn("No client found for userId: {}", userId);
		}
	}

	public void notifyUnreadCounts(String userId,
			Map<String, Integer> unreadCounts) {
		log.info("Notifying unread counts for user: userId = {}, counts = {}",
				userId, unreadCounts);
		SocketIOClient client = userClientMap.get(userId);
		if (client != null) {
			client.sendEvent("unread_counts", unreadCounts);
		} else {
			log.warn("No client found for userId: {}", userId);
		}
	}

	@Override
	public void notifyAllMessagesRead(String conversationId, String userId) {
		// TODO Auto-generated method stub
		SocketIOClient client = userClientMap.get(userId);
		log.info("Map: {}", userClientMap);
		if (client != null) {
			client.sendEvent("read_conversation",
					Map.of("conversationId", conversationId, "userId", userId));
		} else {
			log.warn("No client found for userId: {}", userId);
		}
	}
	
	@Override
	public void notifyNewConversation(ConversationDto conversationDetail,
			String userId) {
		// TODO Auto-generated method stub
		log.info("Notifying new conversation: conversationId = {}, userId = {}",
				conversationDetail.getId(), userId);
		SocketIOClient client = userClientMap.get(userId);
		log.info("Map: {}", userClientMap);
		if (client != null) {
			client.sendEvent("new_conversation", conversationDetail);
			client.joinRoom(conversationDetail.getId());
		} else {
			log.warn("No client found for userId: {}", userId);
		}

	}

	@Override
	public void notifyRemoveConversation(String conversationId) {
		// TODO Auto-generated method stub
		log.info(
				"Notifying remove conversation: conversationId = {}",
				conversationId);
//
//		if (client != null) {
//			client.sendEvent("delete_conversation", conversationId);
//		} else {
//			log.warn("No client found for userId: {}", userId);
//		}
		getChatNamespace().getRoomOperations(conversationId)
				.sendEvent("delete_conversation", conversationId);
		getChatNamespace().getRoomOperations(conversationId)
				.getClients()
				.stream()
				.forEach(c -> {
					c.leaveRoom(conversationId);
				});
		

	}
	
	@Override
	public void notifyClearConversation(String conversationId) {
		// TODO Auto-generated method stub
		
		getChatNamespace().getRoomOperations(conversationId)
				.sendEvent("clear_conversation", conversationId);
	}


	@Override
	public void sendCallInvitation(String conversationId, String callType,
			String initiatorId) {
		log.info(
				"Sending call invitation: conversationId = {}, callType = {}, initiatorId = {}",
				conversationId, callType, initiatorId);
		UserResponseDto userInfo = userService.getUserInfo(initiatorId);
		if (userInfo == null) {
			log.warn("User not found: {}", initiatorId);
			return;
		}
		getChatNamespace().getRoomOperations(conversationId).getClients()
				.stream()
				.filter(client -> !client.get("username").equals(initiatorId))
				.forEach(client -> {
					client.sendEvent("call_invitation",
							Map.of("conversationId", conversationId, "callType",
									callType, "initiator", userInfo));
				});

		log.info("Sent call invitation to conversation {}", conversationId);
	}


    @Override
    public void notifyMemberAdded(String conversationId, String memberPhone) {
//        log.warn("Hasn't implement logic for notifyMemberAdded function");
        getChatNamespace().getRoomOperations(conversationId)
				.sendEvent("member_added", Map.of("conversationId", conversationId, "memberPhone", memberPhone));
        
    }

    @Override
    public void notifyNewLeader(String conversationId, String memberPhone) {
        log.warn("Hasn't implement logic for notifyMemberRemoved function");
    }

    @Override
    public void notifyMemberLeft(String conversationId, String memberPhone) {
        log.warn("Hasn't implement logic for notifyMemberLeft function");
    }
//    ???
    @Override
    public void notifyGroupEvent(String conversationId, String eventType, List<String> data) {
        log.info("Notifying group event: conversationId = {}, eventType = {}, data = {}", 
                conversationId, eventType, data);
        getChatNamespace().getRoomOperations(conversationId)
            .sendEvent("group_event", Map.of(
                "conversationId", conversationId,
                "eventType", eventType,
                "data", data
            ));
    }

	@Override
	public void notifyConversationUpdate(
			ConversationDetailDto conversationDetail) {
		// TODO Auto-generated method stub
		log.info("Notifying conversation update: conversationId = {}",
				conversationDetail.getId());	
		getChatNamespace().getRoomOperations(conversationDetail.getId())
				.sendEvent("conversation_update", conversationDetail);
	}
	
}