package iuh.fit.se.repo;

import java.util.List;

import iuh.fit.se.model.ConversationMember;

public interface ConversationMemberRepository {

	List<ConversationMember> findByConversationId(String conversationId);
	
	void save(ConversationMember member);

    void delete(String conversationId, String userId);

    boolean exists(String conversationId, String userId);

    void updateAdmin(String conversationId, String userId, boolean isAdmin);

    List<ConversationMember> searchMembers(String conversationId, String keyword);
}