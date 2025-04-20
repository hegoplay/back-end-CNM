package iuh.fit.se.service;

import java.util.List;

import iuh.fit.se.model.dto.conversation.ConversationDetailDto;
import iuh.fit.se.model.dto.conversation.ConversationDto;
import iuh.fit.se.model.dto.conversation.CreateGroupRequest;
import iuh.fit.se.model.dto.conversation.MemberDto;

public interface ConversationService {
	void createFriendConversation(String userId, String friendId);
	
	List<ConversationDto> getConversations(String userId);
	
	ConversationDetailDto getConversationDetail(String conversationId);
	
	void updateLastUpdated(String conversationId);
	
	void markAllMessagesAsRead(String conversationId, String userId);
	
	void deleteFriendConversation(String userId, String friendId);
	
	String createGroup(String userId, CreateGroupRequest request);
    void addMembers(String userId, String conversationId, List<String> memberIds);
    void removeMember(String userId, String conversationId, String targetUserId);
    void updateAdmin(String userId, String conversationId, String targetUserId, boolean isAdmin);
    void updateGroupInfo(String userId, String conversationId, String conversationName, String conversationImgUrl);
    List<MemberDto> searchMembers(String conversationId, String keyword);
    void leaveGroup(String userId, String conversationId);
    List<MemberDto> getGroupMembers(String conversationId);

}
