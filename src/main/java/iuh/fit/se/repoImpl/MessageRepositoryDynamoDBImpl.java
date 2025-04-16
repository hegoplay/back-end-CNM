package iuh.fit.se.repoImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import iuh.fit.se.model.Message;
import iuh.fit.se.model.enumObj.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.IndexStatus;
import software.amazon.awssdk.services.s3.S3Client;

@Repository
@RequiredArgsConstructor
@Slf4j
public class MessageRepositoryDynamoDBImpl implements iuh.fit.se.repo.MessageRepository {
    
    private final DynamoDbTable<Message> messageTable;
    private final software.amazon.awssdk.services.dynamodb.DynamoDbClient dynamoDbClient;	
    private final software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient enhancedClient;
    private static final int BATCH_SIZE = 25;
    private static final int THREAD_POOL_SIZE = 4; // Adjust based on table capacity and environment
    private static final long SHUTDOWN_TIMEOUT_MINUTES = 5;
    private final S3Client s3Client;
    
    @Value("${cloudfront.url}")
	private String cloudFrontUrl;

	private String bucketName = "zalas3bucket";
    
    
    
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
	@SuppressWarnings("thay đổi thứ tự duyệt tin nhắn ")
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
                .limit(200) // Giới hạn số lượng tin nhắn trả về
                .scanIndexForward(true) // Sắp xếp tăng dần theo createdAt (cũ nhất trước)
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

	@Override
    public void deleteMessagesByConversationId(String conversationId) {
        try {
            String indexName = "conversationId-createdAt-index";

            // 1. Query all messages using GSI
            QueryConditional queryConditional = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(conversationId).build());

            DynamoDbIndex<Message> index = messageTable.index(indexName);

            // 2. Collect all messages
            List<Message> allMessages = new ArrayList<>();
            index.query(queryConditional).stream()
                .forEach(page -> allMessages.addAll(page.items()));

            if (allMessages.isEmpty()) {
                log.info("No messages found for conversation ID: {}", conversationId);
                return;
            }

            // 3. Create an ExecutorService
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            AtomicInteger totalDeleted = new AtomicInteger(0);

            try {
                // 4. Split messages into batches and submit tasks
                List<List<Message>> batches = partitionList(allMessages, BATCH_SIZE);
                List<Runnable> tasks = batches.stream().map(batch -> (Runnable) () -> {
                    try {
                        // Create WriteBatch for delete operations
                        WriteBatch.Builder<Message> writeBatchBuilder = WriteBatch.builder(Message.class)
                            .mappedTableResource(messageTable);

                        // Add each message to the delete batch
                        for (Message message : batch) {
                        	if (message.getType() == MessageType.MEDIA || message.getType() == MessageType.FILE) {
                        		deleteFromS3PrefixCloudFront(message.getContent());
                        	}
                            writeBatchBuilder.addDeleteItem(DeleteItemEnhancedRequest.builder()
                                .key(k -> k.partitionValue(message.getId())) // Adjust based on table's key schema
                                .build());
                        }

                        // Build and execute the batch write request
                        BatchWriteItemEnhancedRequest batchRequest = BatchWriteItemEnhancedRequest.builder()
                            .writeBatches(writeBatchBuilder.build())
                            .build();

                        enhancedClient.batchWriteItem(batchRequest);
                        totalDeleted.addAndGet(batch.size());
                    } catch (DynamoDbException e) {
                        log.error("Failed to delete batch for conversation ID {}: {}", conversationId, e.getMessage());
                        throw new RuntimeException("Batch delete failed", e);
                    }
                }).collect(Collectors.toList());

                // Submit all tasks
                tasks.forEach(executor::submit);

                // 5. Shutdown and await completion
                executor.shutdown();
                if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                    log.warn("Executor did not terminate within {} minutes for conversation ID: {}", 
                        SHUTDOWN_TIMEOUT_MINUTES, conversationId);
                    executor.shutdownNow();
                }

                log.info("Deleted {} messages for conversation ID: {}", totalDeleted.get(), conversationId);
            } finally {
                if (!executor.isShutdown()) {
                    executor.shutdownNow();
                }
            }
        } catch (DynamoDbException | InterruptedException e) {
            log.error("Error deleting messages for conversation ID {}: {}", conversationId, e.getMessage());
            throw new RuntimeException("Failed to delete messages", e);
        }
    }

    // Utility method to partition a list into sublists of specified size
    private <T> List<List<T>> partitionList(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
    
    private void deleteFromS3(String fileName) {
		s3Client.deleteObject(b -> b.bucket(bucketName).key(fileName));
	}

	private void deleteFromS3PrefixCloudFront(String url) {
		String fileName = url.replace(cloudFrontUrl, "");
		deleteFromS3(fileName);
	}
}