package iuh.fit.se.service;

import java.util.List;

import iuh.fit.se.model.dto.conversation.ConversationDetailDto;
import iuh.fit.se.model.dto.conversation.ConversationDto;

public interface ConversationService {
	void createFriendConversation(String userId, String friendId);
	
	List<ConversationDto> getConversations(String userId);
	
	ConversationDetailDto getConversationDetail(String conversationId);
	
	void updateLastUpdated(String conversationId);
	
	void markAllMessagesAsRead(String conversationId, String userId);
	
	void deleteFriendConversation(String userId, String friendId);
	
}
