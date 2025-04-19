package iuh.fit.se.repoImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import iuh.fit.se.model.Call;
import iuh.fit.se.model.Conversation;
import iuh.fit.se.model.enumObj.CallStatus;
import iuh.fit.se.model.enumObj.CallType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@Repository
@RequiredArgsConstructor // Lombok - tự inject dependencies qua constructor
@Slf4j // Lombok - tự tạo logger
public class CallRepositoryDynamoDBImpl implements iuh.fit.se.repo.CallRepository {
	
    private final DynamoDbTable<Call> conversationTable;

	@Override
	public void createCall(Call call) {
		// TODO Auto-generated method stub
		log.info("Creating call with id: {}", call.getId());
		try {
			conversationTable.putItem(call);
			log.debug("Successfully created call with id: {}", call.getId());
		} catch (Exception e) {
			log.error("Error creating call with id {}: {}", call.getId(), e.getMessage());
			throw new RuntimeException("Failed to create call", e);
		}
	}

	@Override
	public Optional<Call> findById(String callId) {
		// TODO Auto-generated method stub
		log.info("Finding call with id: {}", callId);
		try {
			Key key = Key.builder()
					.partitionValue(callId)
					.build();
			Call call = conversationTable.getItem(key);
			if (call == null) {
				log.info("Call with id {} not found", callId);
				return Optional.empty();
			}
			return Optional.of(call);
		} catch (Exception e) {
			log.info("Error finding call with id {}: {}", callId, e.getMessage());
			throw new RuntimeException("Failed to find call", e);
		}
	}

	@Override
	public void deleteById(String id) {
		// TODO Auto-generated method stub
		log.info("Deleting call with id: {}", id);
		try {
			Key key = Key.builder()
					.partitionValue(id)
					.build();
			conversationTable.deleteItem(key);
			log.debug("Successfully deleted call with id: {}", id);
		} catch (Exception e) {
			log.error("Error deleting call with id {}: {}", id, e.getMessage());
			throw new RuntimeException("Failed to delete call", e);
		}
	}

	@Override
	public boolean existsById(String id) {
		// TODO Auto-generated method stub
		log.info("Checking if call exists with id: {}", id);
		try {
			Key key = Key.builder()
					.partitionValue(id)
					.build();
			Call call = conversationTable.getItem(key);
			boolean exists = call != null;
			log.debug("Call with id {} exists: {}", id, exists);
			return exists;
		} catch (Exception e) {
			log.error("Error checking if call exists with id {}: {}", id, e.getMessage());
			throw new RuntimeException("Failed to check if call exists", e);
		}
	}

	@Override
	public void updateCall(Call call) {
		// TODO Auto-generated method stub
		log.info("Updating call with id: {}", call.getId());
		try {
			conversationTable.updateItem(call);
			log.debug("Successfully updated call with id: {}", call.getId());
		} catch (Exception e) {
			log.error("Error updating call with id {}: {}", call.getId(), e.getMessage());
			throw new RuntimeException("Failed to update call", e);
		}
	}


   

	
}
