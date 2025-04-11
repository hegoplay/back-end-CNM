package iuh.fit.se.repo;

import iuh.fit.se.model.Conversation;

public interface ConversationRepository {
	Conversation findById(String id);
	
	void save(Conversation conversation);
	
	void deleteById(String id);
	
	boolean existsById(String id);
	
	
}
