package iuh.fit.se.repoImpl;

import java.util.Map;

import org.springframework.stereotype.Repository;

import iuh.fit.se.model.User;
import iuh.fit.se.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

@Repository
@RequiredArgsConstructor // Lombok - tự inject dependencies qua constructor
@Slf4j // Lombok - tự tạo logger
public class UserRepositoryDynamoDBImpl implements UserRepository {

	private final DynamoDbClient dynamoDbClient;
	private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
	private final DynamoDbTable<User> userTable;
	
	
	@Override
    public User findByPhone(String phone) {
        // Tạo key với partition key là phoneNumber
        Key key = Key.builder()
            .partitionValue(phone)
            .build();

        // Sử dụng getItem để lấy User từ DynamoDB
        return userTable.getItem(key);
    }

	@Override
	public void save(User user) {
		// TODO Auto-generated method stub
		dynamoDbEnhancedClient.table("users", TableSchema.fromBean(User.class)).putItem(user);
	}

	@Override
	public boolean existsByPhone(String phone) {
		// TODO Auto-generated method stub
		GetItemRequest getItemRequest = GetItemRequest.builder().tableName("users")
				.key(Map.of("phoneNumber", AttributeValue.builder().s(phone).build())).build();

		GetItemResponse response = dynamoDbClient.getItem(getItemRequest);

		// Nếu item tồn tại, map sẽ không rỗng
		return !response.item().isEmpty();
	}

}
