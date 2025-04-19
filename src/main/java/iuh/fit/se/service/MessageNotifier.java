package iuh.fit.se.service;

import com.corundumstudio.socketio.SocketIOClient;

import iuh.fit.se.model.dto.conversation.ConversationDetailDto;
import iuh.fit.se.model.dto.message.MessageResponseDTO;

public interface MessageNotifier {
	
	void registerClient(String userId, SocketIOClient client);
	void removeClient(String userId);
	public SocketIOClient getClient(String userId);
    void notifyNewMessage(MessageResponseDTO message);
//    thu hồi tin nhắn
    void notifyMessageRecalled(String conversationId, String messageId);
    void notifyReactionAdded(String conversationId, String messageId, String emoji, String userId);
    void notifyAllMessagesRead(String conversationId, String userId);
    void initConversation(ConversationDetailDto conversationDetail, String userId);
    void notifyNewConversation(ConversationDetailDto conversationDetail, String userId);
    void notifyRemoveConversation(String conversationId, String userId);
    void sendCallInvitation(String conversationId, String callType, String initiatorId);
    void notifyClearConversation(String conversationId, String userId);
}
