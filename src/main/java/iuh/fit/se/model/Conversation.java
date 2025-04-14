package iuh.fit.se.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import iuh.fit.se.model.converter.TypeConversationConverter;
import iuh.fit.se.model.enumObj.ConversationType;
import lombok.Builder;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Builder
@Data
@lombok.experimental.FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Conversation {

	private String id;
	private ConversationType type;
	private List<String> participants;
	private List<String> messages;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private Boolean callInProgress;
	private String currentCallId;


	@DynamoDbPartitionKey
	@DynamoDbAttribute("id")
	public String getId() {
		return id;
	}

	@DynamoDbAttribute("type")
	@DynamoDbConvertedBy(TypeConversationConverter.class)
	public ConversationType getType() {
		return type;
	}

	public void addMessageId(String messageId) {
		if (messages == null) {
			messages = new ArrayList<>();
		}
		messages.add(messageId);
	}

	public boolean removeMessageId(String messageId) {
		if (messages != null) {
			// Check if the messageId exists in the list
			if (!messages.contains(messageId)) {
				return false; // Message ID not found
			}
			messages.remove(messageId);
			return true;
		}
		return false; // Messages list is null
		
	}
}
