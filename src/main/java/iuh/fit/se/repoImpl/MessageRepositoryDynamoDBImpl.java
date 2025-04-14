package iuh.fit.se.repoImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.swing.SortOrder;

import org.springframework.stereotype.Repository;

import iuh.fit.se.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.IndexStatus;

@Repository
@RequiredArgsConstructor
@Slf4j
public class MessageRepositoryDynamoDBImpl implements iuh.fit.se.repo.MessageRepository {
    
    private final DynamoDbTable<Message> messageTable;
    private final software.amazon.awssdk.services.dynamodb.DynamoDbClient dynamoDbClient;	
    
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

	@Override
    public List<Message> findMessagesByConversationId(String conversationId) {
        try {
            String indexName = "conversationId-createdAt-index";

            // Kiểm tra trạng thái GSI
            if (!isGsiActive("messages", indexName)) {
                log.warn("GSI {} is not active", indexName);
                throw new IllegalStateException("GSI " + indexName + " is not ready for querying");
            }

            // Truy vấn GSI với sắp xếp giảm dần (mới nhất trước)
            QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder()
                .partitionValue(conversationId)
                .build());
            QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .scanIndexForward(false) // Sắp xếp giảm dần theo createdAt (mới nhất trước)
                .build();

            DynamoDbIndex<Message> index = messageTable.index(indexName);
            List<Message> messages = new ArrayList<>();
            Iterator<Page<Message>> pageIterator = index.query(request).iterator();

            // Lấy tất cả tin nhắn (không giới hạn)
            while (pageIterator.hasNext()) {
                Page<Message> page = pageIterator.next();
                messages.addAll(page.items());
            }

            return messages;

        } catch (DynamoDbException e) {
            log.error("Error retrieving messages for conversation ID {}: {}", conversationId, e.getMessage());
            throw new RuntimeException("Failed to retrieve messages", e);
        }
    }

    // Kiểm tra trạng thái GSI
    private boolean isGsiActive(String tableName, String indexName) {
        try {
            DescribeTableResponse response = dynamoDbClient.describeTable(DescribeTableRequest.builder()
                .tableName(tableName)
                .build());
            return response.table().globalSecondaryIndexes().stream()
                .filter(index -> index.indexName().equals(indexName))
                .anyMatch(index -> index.indexStatus() == IndexStatus.ACTIVE);
        } catch (DynamoDbException e) {
            log.error("Error checking GSI status for table {} and index {}: {}", tableName, indexName, e.getMessage());
            return false;
        }
    }

	@Override
	public int countUnreadMessages(String conversationId, String userId) {
		// TODO Auto-generated method stub
		QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder()
				.partitionValue(conversationId)
				.build());
		QueryEnhancedRequest request = QueryEnhancedRequest.builder()
				.queryConditional(queryConditional)
				.scanIndexForward(false) // Sắp xếp giảm dần theo createdAt (mới nhất trước)
				.build();

		DynamoDbIndex<Message> index = messageTable.index("conversationId-createdAt-index");
		List<Message> messages = new ArrayList<>();
		Iterator<Page<Message>> pageIterator = index.query(request).iterator();

		// Lấy tất cả tin nhắn (không giới hạn)
		while (pageIterator.hasNext()) {
			Page<Message> page = pageIterator.next();
			messages.addAll(page.items());
		}

		// Đếm số tin nhắn chưa đọc
		int unreadCount = 0;
		for (Message message : messages) {
			if (!message.getSeenBy().contains(userId) && !message.getSenderId().equals(userId)) {
				unreadCount++;
			}
		}

		return unreadCount;
	}
}