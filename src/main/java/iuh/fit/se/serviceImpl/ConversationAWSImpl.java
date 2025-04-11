package iuh.fit.se.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import iuh.fit.se.model.Conversation;
import iuh.fit.se.model.ConversationType;
import iuh.fit.se.model.User;
import iuh.fit.se.model.dto.ConversationDto;
import iuh.fit.se.repo.ConversationRepository;
import iuh.fit.se.repo.UserRepository;
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
	private final UserRepository userRepository;
	private final JwtUtils jwtUtils;
	
	@Override
	public void createFriendConversation(String userPhone, String friendPhone) {
		// TODO Auto-generated method stub
		User user = userRepository.findByPhone(userPhone);
		if (user == null) {
			log.warn("User with id {} not found", userPhone);
			return;
		}
		User friend = userRepository.findByPhone(friendPhone);
		if (friend == null) {
			log.warn("User with id {} not found", friendPhone);
			return;
		}
		user.getConversations().add(userPhone + "_" + friendPhone);
		friend.getConversations().add(userPhone + "_" + friendPhone);
		
		Conversation conversation = Conversation.builder()
						.id(userPhone + "_" + friendPhone)
						.callInProgress(false)
						.type(ConversationType.PRIVATE)
						.participants(List.of(userPhone, friendPhone))
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
		try {
			userRepository.save(user);
			userRepository.save(friend);
		} catch (Exception e) {
			log.error("Error saving user: {}", e.getMessage());
			throw new RuntimeException("Error saving user", e);
		}
	}

	@Override
	public List<ConversationDto> getConversations(String phone) {
		// TODO Auto-generated method stub
		try {
//			List<Conversation> conversations = conversationRepository.findByUserId(userId);
			User user = userRepository.findByPhone(phone);
			if (user == null) {
				log.warn("User with id {} not found", phone);
				return List.of();
			}
			List<Conversation> conversations = new ArrayList<>();
			for (String conversationId : user.getConversations()) {
				Conversation conversation = conversationRepository.findById(conversationId);
				if (conversation != null) {
					conversations.add(conversation);
				}
			}
			return conversations.stream()
					.map(conversation -> ConversationDto.builder()
							.id(conversation.getId())
							.type(conversation.getType())
							.participants(conversation.getParticipants())
							.lastMessage(conversation.getLastMessage())
							.createdAt(conversation.getCreatedAt())
							.updatedAt(conversation.getUpdatedAt())
							.callInProgress(conversation.getCallInProgress())
							.currentCallId(conversation.getCurrentCallId())
							.build())
					.toList();
		} catch (Exception e) {
			log.error("Error getting conversations: {}", e.getMessage());
			throw new RuntimeException("Error getting conversations", e);
		}		
	}

}
