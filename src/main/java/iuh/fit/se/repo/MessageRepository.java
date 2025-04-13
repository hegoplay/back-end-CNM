package iuh.fit.se.repo;

import java.util.Optional;

import iuh.fit.se.model.Message;

public interface MessageRepository {
		// Define methods for CRUD operations on messages
	void save(Message message);

	Message getMessageById(String messageId);
	
	Optional<Message> findById(String id);

	void updateMessage(Message message);

	void deleteMessage(String messageId);
}
