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

	ConversationDetailDto createGroupChat(String creatorPhone, String conversationName, String conversationImgUrl, List<String> participants);

	void addMembersToGroup(String conversationId, String leaderPhone, List<String> newMemberPhones);

	void removeMemberFromGroup(String conversationId, String leaderPhone, String memberPhone);

	void leaveGroup(String conversationId, String memberPhone, String newLeaderPhone);
}
