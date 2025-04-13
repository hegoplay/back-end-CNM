package iuh.fit.se.repoImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import iuh.fit.se.model.Conversation;
import iuh.fit.se.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

@Repository
@RequiredArgsConstructor
@Slf4j
public class MessageRepositoryDynamoDBImpl implements iuh.fit.se.repo.MessageRepository {
    
    private final DynamoDbTable<Message> messageTable;
    
    @Override
    public void save(Message message) {
        try {
            // Generate ID if not provided
            if (message.getId() == null || message.getId().isEmpty()) {
                message.setId(UUID.randomUUID().toString());
            }
            
            // Set created time if not provided
            if (message.getCreatedAt() == null) {
                message.setCreatedAt(LocalDateTime.now());
            }
            
            messageTable.putItem(message);
            log.info("Message saved successfully with ID: {}", message.getId());
        } catch (Exception e) {
            log.error("Error saving message: {}", e.getMessage());
            throw new RuntimeException("Failed to save message", e);
        }
    }

    @Override
    public Message getMessageById(String messageId) {
        try {
            Key key = Key.builder()
                    .partitionValue(messageId)
                    .build();
            
            Message message = messageTable.getItem(key);
            
            if (message == null) {
                log.warn("Message not found with ID: {}", messageId);
                return null;
            }
            
            log.info("Retrieved message with ID: {}", messageId);
            return message;
        } catch (Exception e) {
            log.error("Error retrieving message with ID {}: {}", messageId, e.getMessage());
            throw new RuntimeException("Failed to retrieve message", e);
        }
    }


    @Override
    public void updateMessage(Message message) {
        try {
            // First check if the message exists
            Message existingMessage = getMessageById(message.getId());
            if (existingMessage == null) {
                throw new RuntimeException("Message not found with ID: " + message.getId());
            }
            
            messageTable.updateItem(message);
            log.info("Message updated successfully with ID: {}", message.getId());
        } catch (Exception e) {
            log.error("Error updating message with ID {}: {}", message.getId(), e.getMessage());
            throw new RuntimeException("Failed to update message", e);
        }
    }

    @Override
    public void deleteMessage(String messageId) {
        try {
            Key key = Key.builder()
                    .partitionValue(messageId)
                    .build();
            
            messageTable.deleteItem(key);
            log.info("Message deleted successfully with ID: {}", messageId);
        } catch (Exception e) {
            log.error("Error deleting message with ID {}: {}", messageId, e.getMessage());
            throw new RuntimeException("Failed to delete message", e);
        }
    }

	@Override
	public Optional<Message> findById(String id) {
		// TODO Auto-generated method stub
		Key key = Key.builder()
                .partitionValue(id)
                .build();
        
        Message message = messageTable.getItem(key);
		return Optional.ofNullable(message);
	}
}