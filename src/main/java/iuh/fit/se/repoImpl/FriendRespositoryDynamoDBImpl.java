package iuh.fit.se.repoImpl;

import iuh.fit.se.model.User;
import iuh.fit.se.model.dto.UserResponseDto;
import iuh.fit.se.repo.FriendRespository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.Expression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FriendRespositoryDynamoDBImpl implements FriendRespository {

    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final DynamoDbTable<User> userTable;

    public List<User> findByPhone(String phone) {
        DynamoDbTable<User> userTable = dynamoDbEnhancedClient.table("users", TableSchema.fromBean(User.class));

        Expression expression = Expression.builder()
                .expression("phoneNumber = :val")
                .putExpressionValue(":val", AttributeValue.builder().s(phone).build())
                .build();

        Iterable<User> users = userTable.scan(
                ScanEnhancedRequest.builder()
                        .filterExpression(expression)
                        .build()
        ).items();

        List<User> userSearchResults = new ArrayList<>();
        for (User user : users){
            userSearchResults.add(user);
        }

        return userSearchResults;
    }
}
