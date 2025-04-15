package iuh.fit.se.serviceImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;

import iuh.fit.se.model.dto.conversation.ConversationDetailDto;
import iuh.fit.se.model.dto.message.MessageResponseDTO;
import iuh.fit.se.service.MessageNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocketIONotifier implements MessageNotifier {
    private final SocketIOServer socketIOServer;
    private final String NAMESPACE = "/chat";
    // Mapping userId -> SocketIOClient
    private final Map<String, SocketIOClient> userClientMap = new ConcurrentHashMap<>();

    // Lấy namespace đã khởi tạo
    private SocketIONamespace getChatNamespace() {
        return socketIOServer.getNamespace(NAMESPACE);
    }

    // Đăng ký client khi kết nối
    public void registerClient(String userId, SocketIOClient client) {
        log.info("Registering client for userId: {}", userId);
        userClientMap.put(userId, client);
    }

    // Xóa client khi ngắt kết nối
    public void removeClient(String userId) {
        log.info("Removing client for userId: {}", userId);
        userClientMap.remove(userId);
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
        log.info("Notifying message recalled: conversationId = {}, messageId = {}", 
                conversationId, messageId);
        getChatNamespace().getRoomOperations(conversationId)
            .sendEvent("message_recalled", Map.of(
                "messageId", messageId,
                "conversationId", conversationId
            ));
    }

    @Override
    public void notifyReactionAdded(String conversationId, String messageId, String emoji, String userId) {
        log.info("Notifying reaction added: conversationId = {}, messageId = {}, emoji = {}, userId = {}", 
                conversationId, messageId, emoji, userId);
        getChatNamespace().getRoomOperations(conversationId)
            .sendEvent("reaction_added", Map.of(
                "messageId", messageId,
                "emoji", emoji,
                "userId", userId,
                "conversationId", conversationId
            ));
    }
    
    

    @Override
    public void initConversation(ConversationDetailDto conversationDetail, String userId) {
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

    public void notifyUnreadCounts(String userId, Map<String, Integer> unreadCounts) {
        log.info("Notifying unread counts for user: userId = {}, counts = {}", userId, unreadCounts);
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
	            client.sendEvent("read_conversation",  Map.of(
	                "conversationId", conversationId,
	                "userId", userId
	            ));	
	        } else {
	            log.warn("No client found for userId: {}", userId);
	        }
	}

	@Override
	public void notifyNewConversation(ConversationDetailDto conversationDetail,
			String userId) {
		// TODO Auto-generated method stub
		log.info("Notifying new conversation: conversationId = {}, userId = {}", 
				conversationDetail.getId(), userId);
		SocketIOClient client = userClientMap.get(userId);
		log.info("Map: {}", userClientMap);
		if (client != null) {
			client.sendEvent("new_conversation", conversationDetail);
		} else {
			log.warn("No client found for userId: {}", userId);
		}
		
	}
}