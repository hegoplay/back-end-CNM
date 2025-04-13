package iuh.fit.se.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import iuh.fit.se.model.Conversation;
import iuh.fit.se.model.Message;
import iuh.fit.se.model.User;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class AwsConfig {

	@Value("${aws.accessKeyId}")
	private String accessKeyId;

	@Value("${aws.secretKey}")
	private String secretKey;

	@Value("${aws.region}")
	private String region;

	@Bean
	S3Client s3Client() {
		AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretKey);
		return S3Client.builder().region(Region.AP_SOUTHEAST_1) // Thay bằng region của bạn
				.credentialsProvider(StaticCredentialsProvider.create(credentials)).build();
	}

	@Bean
	SnsClient snsClient() {
		AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretKey);
		return SnsClient.builder().region(Region.AP_SOUTHEAST_1) // Thay bằng region của bạn
				.credentialsProvider(StaticCredentialsProvider.create(credentials)).build();
	}

	@Bean
	DynamoDbClient dynamoDbClient() {
		AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretKey);
		return DynamoDbClient.builder().region(Region.AP_SOUTHEAST_1) // Thay bằng region của bạn
				.credentialsProvider(StaticCredentialsProvider.create(credentials)).build();
	}

	@Bean
	DynamoDbEnhancedClient dynamoDbEnchancedClient() {
//		AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretKey);
		return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient()).extensions().build();
	}
	
	@Bean
    DynamoDbTable<User> userTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table("users", TableSchema.fromBean(User.class));
    }
	
	@Bean
    DynamoDbTable<Conversation> conversationTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table("conversations", TableSchema.fromBean(Conversation.class));
    }
	
	@Bean
    DynamoDbTable<Message> messageTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table("messages", TableSchema.fromBean(Message.class));
    }
}