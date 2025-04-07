package iuh.fit.se.model;

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
	boolean gender;
	String dateOfBirth;
	String status;
	boolean isOnline;
	String lastOnlineTime;
	String createdAt;
	String updatedAt;

	// Relationships
	List<String> requestList;
	List<String> friends;
	List<String> pendings;
	List<String> mutedConversation;
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
}
