package iuh.fit.se.service;

import java.util.List;

import com.corundumstudio.socketio.SocketIOClient;

import iuh.fit.se.model.dto.conversation.ConversationDetailDto;
import iuh.fit.se.model.dto.conversation.ConversationDto;
import iuh.fit.se.model.dto.message.MessageResponseDTO;
import iuh.fit.se.model.dto.user.UserResponseDto;

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
    void notifyNewConversation(ConversationDto conversationDetail, String userId);
    void notifyRemoveConversation(String conversationId);
    void sendCallInvitation(String conversationId, String callType, UserResponseDto initiator);
    void notifyClearConversation(String conversationId);
    void notifyMemberAdded(String conversationId, String memberPhone);
    void notifyNewLeader(String conversationId, String memberPhone);
    void notifyMemberLeft(String conversationId, String memberPhone);
    void notifyGroupEvent(String conversationId, String eventType, List<String> data);
    void notifyConversationUpdate(ConversationDetailDto conversationDetail);
}
