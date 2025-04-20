package iuh.fit.se.repoImpl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import iuh.fit.se.model.ConversationMember;
import iuh.fit.se.model.dto.UserResponseDto;
import iuh.fit.se.repo.ConversationMemberRepository;
import iuh.fit.se.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ConversationMemberRepositoryImpl implements ConversationMemberRepository {

    private final DynamoDbTable<ConversationMember> conversationMemberTable;
    private final UserService userService;

    @Override
    public List<ConversationMember> findByConversationId(String conversationId) {
        try {
            QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder()
                .partitionValue(conversationId)
                .build());
            
            QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .build();

            List<ConversationMember> members = conversationMemberTable.query(request)
                    .stream()
                    .flatMap(page -> page.items().stream())
                    .collect(Collectors.toList());

            log.info("Retrieved {} members for conversation ID {}", members.size(), conversationId);
            return members;

        } catch (DynamoDbException e) {
            log.error("Error retrieving members for conversation ID {}: {}", conversationId, e.getMessage());
            throw new RuntimeException("Failed to retrieve conversation members", e);
        }
    }

    @Override
    public void save(ConversationMember member) {
        try {
            if (member.getConversationId() == null || member.getConversationId().isEmpty()) {
                throw new IllegalArgumentException("Conversation ID cannot be null or empty");
            }
            if (member.getUserId() == null || member.getUserId().isEmpty()) {
                throw new IllegalArgumentException("User ID cannot be null or empty");
            }

            conversationMemberTable.putItem(member);
            log.info("Conversation member saved successfully: conversationId={}, userId={}", 
                     member.getConversationId(), member.getUserId());
        } catch (Exception e) {
            log.error("Error saving conversation member: {}", e.getMessage());
            throw new RuntimeException("Failed to save conversation member", e);
        }
    }

    @Override
    public void delete(String conversationId, String userId) {
        try {
            Key key = Key.builder()
                    .partitionValue(conversationId)
                    .sortValue(userId)
                    .build();

            conversationMemberTable.deleteItem(key);
            log.info("Conversation member deleted: conversationId={}, userId={}", conversationId, userId);
        } catch (Exception e) {
            log.error("Error deleting conversation member: {}", e.getMessage());
            throw new RuntimeException("Failed to delete conversation member", e);
        }
    }

    @Override
    public boolean exists(String conversationId, String userId) {
        try {
            Key key = Key.builder()
                    .partitionValue(conversationId)
                    .sortValue(userId)
                    .build();

            boolean exists = conversationMemberTable.getItem(key) != null;
            log.debug("Checked existence: conversationId={}, userId={}, exists={}", conversationId, userId, exists);
            return exists;

        } catch (DynamoDbException e) {
            log.error("Error checking existence of member: {}", e.getMessage());
            throw new RuntimeException("Failed to check existence of conversation member", e);
        }
    }

    @Override
    public void updateAdmin(String conversationId, String userId, boolean isAdmin) {
        try {
            Key key = Key.builder()
                    .partitionValue(conversationId)
                    .sortValue(userId)
                    .build();

            ConversationMember member = conversationMemberTable.getItem(key);
            if (member != null) {
                member.setAdmin(isAdmin);
                conversationMemberTable.updateItem(member);
                log.info("Updated admin status: conversationId={}, userId={}, isAdmin={}", 
                         conversationId, userId, isAdmin);
            } else {
                log.warn("Member not found: conversationId={}, userId={}", conversationId, userId);
                throw new IllegalArgumentException("Member not found in conversation");
            }

        } catch (Exception e) {
            log.error("Error updating admin status: {}", e.getMessage());
            throw new RuntimeException("Failed to update admin status", e);
        }
    }

    @Override
    public List<ConversationMember> searchMembers(String conversationId, String keyword) {
        try {
            // Lấy tất cả thành viên của conversation
            List<ConversationMember> members = findByConversationId(conversationId);
            
            // Lọc thành viên dựa trên tên người dùng từ UserService.getUserInfo
            List<ConversationMember> filteredMembers = members.stream()
                .filter(member -> {
                    try {
                        UserResponseDto userDto = userService.getUserInfo(member.getUserId());
                        return userDto != null && userDto.getName() != null && 
                               userDto.getName().toLowerCase().contains(keyword.toLowerCase());
                    } catch (Exception e) {
                        log.warn("Error fetching user info for userId={}: {}", member.getUserId(), e.getMessage());
                        return false;
                    }
                })
                .collect(Collectors.toList());

            log.info("Found {} members matching keyword '{}' in conversationId={}", 
                     filteredMembers.size(), keyword, conversationId);
            return filteredMembers;

        } catch (Exception e) {
            log.error("Error searching members for conversationId={}: {}", conversationId, e.getMessage());
            throw new RuntimeException("Failed to search conversation members", e);
        }
    }
}