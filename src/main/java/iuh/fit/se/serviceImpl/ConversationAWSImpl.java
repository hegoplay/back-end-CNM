package iuh.fit.se.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import iuh.fit.se.model.Conversation;
import iuh.fit.se.model.ConversationType;
import iuh.fit.se.repo.ConversationRepository;
import iuh.fit.se.service.ConversationService;
import iuh.fit.se.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationAWSImpl implements ConversationService {

	@Value("${aws.region}")
	private String region;
	private final PasswordEncoder passwordEncoder;
	private final ConversationRepository conversationRepository;
	private final JwtUtils jwtUtils;
	
	@Override
	public void createFriendConversation(String userId, String friendId) {
		// TODO Auto-generated method stub
		Conversation conversation = Conversation.builder()
						.id(userId + "_" + friendId)
						.callInProgress(false)
						.type(ConversationType.PRIVATE)
						.participants(List.of(userId, friendId))
						.createdAt(LocalDateTime.now())
						.updatedAt(LocalDateTime.now())
						.lastMessage("")
						.currentCallId(null)
						.build();
		try {
			conversationRepository.save(conversation);
		} catch (Exception e) {
			log.error("Error creating conversation: {}", e.getMessage());
			throw new RuntimeException("Error creating conversation", e);
		}
	}

}
