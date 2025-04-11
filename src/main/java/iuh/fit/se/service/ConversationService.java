package iuh.fit.se.service;

import java.util.List;

import iuh.fit.se.model.dto.ConversationDto;

public interface ConversationService {
	void createFriendConversation(String userId, String friendId);
	
	List<ConversationDto> getConversations(String userId);
}
