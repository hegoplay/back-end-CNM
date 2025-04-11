package iuh.fit.se;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import iuh.fit.se.config.AwsConfig;
import iuh.fit.se.model.User;
import iuh.fit.se.service.AwsService;
import iuh.fit.se.service.UserService;
import iuh.fit.se.util.JwtUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sns.SnsClient;

@SpringBootTest
class BackendCmnApplicationTests {

	@MockitoBean
    private AwsConfig awsConfig; // Mock the awsConfig bean
	@MockitoBean
    private SnsClient snsClient; // Mock the SnsClient bean
	@MockitoBean
	private AwsService awsService; // Mock the AwsService bean
	@MockitoBean
	private UserService userService; // Mock the UserService bean
	@MockitoBean
	private JwtUtils jwtUtils; // Mock the JwtUtils bean
	@MockitoBean
	private DynamoDbClient dynamoDbClient; // Mock the DynamoDbClient bean
	@MockitoBean
	private DynamoDbEnhancedClient dynamoDbEnhancedClient; // Mock the DynamoDbEnhancedClient bean
	@MockitoBean
    private DynamoDbTable<User> userTable;

    @Test
    void contextLoads() {
        // Test will pass if context loads successfully
    }
}
