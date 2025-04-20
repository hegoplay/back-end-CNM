package iuh.fit.se.service;

import java.util.List;

import iuh.fit.se.model.dto.conversation.ConversationDetailDto;
import iuh.fit.se.model.dto.conversation.ConversationDto;

/*
 * 
 * phó nhóm thì không được xóa thành (admins)
 * phó nhóm được xóa nhóm (admins)
 * thành viên thì không được xóa
 */

public interface ConversationService {
	void createFriendConversation(String userId, String friendId);
	
	List<ConversationDto> getConversations(String userId);
	
	ConversationDetailDto getConversationDetail(String conversationId);
	
	ConversationDetailDto getConversationDetail(String conversationId, String phone);
	
	ConversationDto getConversationById(String conversationId);
	
	void updateLastUpdated(String conversationId);
	
	void updateConversationInCall(String conversationId, boolean inCall);
	
	void markAllMessagesAsRead(String conversationId, String userId);
	
	void deleteFriendConversation(String userId, String friendId);

	
	ConversationDetailDto createGroupChat(String creatorPhone, String conversationName, String conversationImgUrl, List<String> participants);
	
	/*
	 * ai cũng làm được
	 */
	void addMembersToGroup(String conversationId, String leaderPhone, List<String> newMemberPhones);

	/*
	 * admin vs leader làm được
	 */
	void removeMemberFromGroup(String conversationId, String leaderPhone, String memberPhone);

	
	void leaveGroup(String conversationId, String memberPhone, String newLeaderPhone);

	/*
	 * leader làm được
	 */
	
	void deleteGroup(String conversationId, String userPhone);
}
