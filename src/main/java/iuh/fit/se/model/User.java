package iuh.fit.se.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Builder;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.experimental.FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@DynamoDbBean
@Builder
@JsonSerialize
@JsonDeserialize
public class User {
	String phoneNumber;
	// Account info
	String password;
	String baseImg;
	String backgroundImg;
	String name;
	String bio;
	boolean isMale;
	LocalDate dateOfBirth;
	String status;
	boolean isOnline;
	LocalDateTime lastOnlineTime;
	LocalDateTime createdAt;
	LocalDateTime updatedAt;

	// Relationships
	List<String> requestList;
	List<String> friends;
	List<String> pendings;
	List<String> mutedConversation;
	List<String> conversations;
	String currentCallId; // nullable

	// Call settings (nested object)
	private CallSettings callSettings;

	// Nested class for call settings
	@Data
	@lombok.NoArgsConstructor
	@lombok.AllArgsConstructor
	@lombok.experimental.FieldDefaults(level = lombok.AccessLevel.PRIVATE)
	@DynamoDbBean
	@JsonSerialize
	@JsonDeserialize
	public static class CallSettings {
		boolean videoEnabled;
		boolean audioEnabled;
		boolean muteMicrophone;
		boolean hideVideo;

	}
	
	@DynamoDbPartitionKey
	public String getPhoneNumber() {
		return phoneNumber;
	}
	
	public void removeConversationId(String conversationId) {
		if (conversations != null) {
			conversations = conversations.stream()
					.filter(id -> !id.equalsIgnoreCase(conversationId))
					.toList();
		}
	}
}
