package iuh.fit.se.repoImpl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import iuh.fit.se.model.Conversation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@Repository
@RequiredArgsConstructor // Lombok - tự inject dependencies qua constructor
@Slf4j // Lombok - tự tạo logger
public class ConversationRepositoryDynamoDBImpl implements iuh.fit.se.repo.ConversationRepository {
	
    private final DynamoDbTable<Conversation> conversationTable;

    @Override
    public Conversation findById(String id) {
        log.info("Finding conversation with id: {}", id);
        try {
            Key key = Key.builder()
                    .partitionValue(id)
                    .build();
            Conversation conversation = conversationTable.getItem(key);
            if (conversation == null) {
                log.info("Conversation with id {} not found", id);
            }
            return conversation;
        } catch (Exception e) {
            log.info("Error finding conversation with id {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to find conversation", e);
        }
    }

    @Override
    public void save(Conversation conversation) {
        log.info("Saving conversation with id: {}", conversation.getId());
        try {
            conversationTable.putItem(conversation);
            log.debug("Successfully saved conversation with id: {}", conversation.getId());
        } catch (Exception e) {
            log.error("Error saving conversation with id {}: {}", conversation.getId(), e.getMessage());
            throw new RuntimeException("Failed to save conversation", e);
        }
    }

    @Override
    public void deleteById(String id) {
        log.info("Deleting conversation with id: {}", id);
        try {
            Key key = Key.builder()
                    .partitionValue(id)
                    .build();
            conversationTable.deleteItem(key);
            log.debug("Successfully deleted conversation with id: {}", id);
        } catch (Exception e) {
            log.error("Error deleting conversation with id {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to delete conversation", e);
        }
    }

    @Override
    public boolean existsById(String id) {
        log.info("Checking if conversation exists with id: {}", id);
        try {
            Key key = Key.builder()
                    .partitionValue(id)
                    .build();
            Conversation conversation = conversationTable.getItem(key);
            boolean exists = conversation != null;
            log.debug("Conversation with id {} exists: {}", id, exists);
            return exists;
        } catch (Exception e) {
            log.error("Error checking existence of conversation with id {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to check conversation existence", e);
        }
    }

    @Override
    public void updateLastUpdated(String conversationId) {
        log.info("Updating lastUpdated for conversation: {}", conversationId);
        try {
            // 1. Lấy conversation hiện tại
            Key key = Key.builder()
                    .partitionValue(conversationId)
                    .build();
            Conversation conversation = conversationTable.getItem(key);
            
            if (conversation == null) {
                log.warn("Conversation not found with id: {}", conversationId);
                throw new RuntimeException("Conversation not found");
            }
            
            // 2. Cập nhật thời gian
            conversation.setUpdatedAt(LocalDateTime.now());
            
            // 3. Lưu lại vào DynamoDB
            conversationTable.updateItem(conversation);
            log.debug("Successfully updated lastUpdated for conversation: {}", conversationId);
            
        } catch (Exception e) {
            log.error("Failed to update lastUpdated for conversation {}: {}", conversationId, e.getMessage());
            throw new RuntimeException("Failed to update conversation timestamp", e);
        }
    }

	@Override
	public boolean existsByParticipantsContainingAndId(String userPhone, String conversationId) {
		// TODO Auto-generated method stub
		Conversation conversation = findById(conversationId);
		if (conversation != null) {
			if (conversation.getParticipants() != null && conversation.getParticipants().contains(userPhone)) {
				return true;
			} else {
				log.warn("User {} is not a participant in conversation {}", userPhone, conversationId);
			}
		} else {
			log.warn("Conversation with id {} not found", conversationId);
		}
		return false;
	}

	
}
