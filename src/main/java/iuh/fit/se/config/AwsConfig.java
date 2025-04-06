package iuh.fit.se.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
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
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
        		accessKeyId, 
        		secretKey
        );
        return S3Client.builder()
            .region(Region.AP_SOUTHEAST_1) // Thay bằng region của bạn
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build();
    }
    
    @Bean
    SnsClient snsClient() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
        		accessKeyId, 
        		secretKey
        );
        return SnsClient.builder()
            .region(Region.AP_SOUTHEAST_1) // Thay bằng region của bạn
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build();
    }
    
    @Bean
    DynamoDbClient dynamoDbClient() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
        		accessKeyId, 
        		secretKey
        );
        return DynamoDbClient.builder()
            .region(Region.AP_SOUTHEAST_1) // Thay bằng region của bạn
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build();
    }
}