package iuh.fit.se.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import iuh.fit.se.mapper.ConversationMapper;
import iuh.fit.se.mapper.MessageMapper;
import iuh.fit.se.model.Conversation;
import iuh.fit.se.model.Message;
import iuh.fit.se.model.User;
import iuh.fit.se.model.dto.conversation.ConversationDetailDto;
import iuh.fit.se.model.dto.conversation.ConversationDto;
import iuh.fit.se.model.dto.message.MessageResponseDTO;
import iuh.fit.se.model.enumObj.ConversationType;
import iuh.fit.se.repo.ConversationRepository;
import iuh.fit.se.repo.MessageRepository;
import iuh.fit.se.repo.UserRepository;
import iuh.fit.se.service.ConversationService;
import iuh.fit.se.service.MessageNotifier;
import iuh.fit.se.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationServiceAWSImpl implements ConversationService {

	@Value("${aws.region}")
	private String region;
	private final PasswordEncoder passwordEncoder;
	private final ConversationRepository conversationRepository;
	private final MessageRepository messageRepository;
	private final UserRepository userRepository;
	private final DynamoDbTable<User> userTable;
	private final DynamoDbTable<Conversation> conversationTable;
	private final DynamoDbEnhancedClient enhancedClient;
	private final MessageNotifier messageNotifier;
	
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
		
		String conversationId = userPhone + "_" + friendPhone;
		
		if(user.getConversations().contains(conversationId)) {
			log.warn("Conversation already exists");
			return;
		}
		if(friend.getConversations().contains(conversationId)) {
			log.warn("Conversation already exists");
			return;
		}
		
		if (user.getConversations() == null) {
			user.setConversations(new ArrayList<>());
		}
		if (friend.getConversations() == null) {
			friend.setConversations(new ArrayList<>());
		}
		
		user.getConversations().add(conversationId);
		friend.getConversations().add(conversationId);
		
		Conversation conversation = Conversation.builder()
						.id(userPhone + "_" + friendPhone)
						.callInProgress(false)
						.type(ConversationType.PRIVATE)
						.participants(List.of(userPhone, friendPhone))
						.createdAt(LocalDateTime.now())
						.updatedAt(LocalDateTime.now())
						.messages(List.of())
						.currentCallId(null)
						.build();
		try {
	        enhancedClient.transactWriteItems(request -> request
	            .addPutItem(conversationTable, conversation)
	            .addPutItem(userTable, user)
	            .addPutItem(userTable, friend)
	        );
	        log.info("Transaction completed successfully");
	        ConversationDetailDto conversationDetailDto = getConversationDetail(conversationId);
	        messageNotifier.initConversation(conversationDetailDto, conversationId);
	    } catch (TransactionCanceledException e) {
	        log.error("Transaction cancelled: {}", e.cancellationReasons());
	        throw new RuntimeException("Transaction failed", e);
	    }
	}
/**
 * cái phone này là của người dùng đang đăng nhập
 */
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
			conversations.sort((c1, c2) -> c2.getUpdatedAt().compareTo(c1.getUpdatedAt()));
			List<ConversationDto> conversationDtos = new ArrayList<>();
			for (Conversation conversation : conversations) {
				conversationDtos.add(ConversationMapper.INSTANCE.fromConversationToDto(conversation));
			}
			return conversationDtos;
		} catch (Exception e) {
			log.error("Error getting conversations: {}", e.getMessage());
			throw new RuntimeException("Error getting conversations", e);
		}		
	}

	@Override
	public ConversationDetailDto getConversationDetail(String conversationId) {
		// TODO Auto-generated method stub
		Conversation conversation = conversationRepository.findById(conversationId);
		if (conversation == null) {
//				log.warn("Conversation with id {} not found", conversationId);
			throw new RuntimeException("Conversation not found");
		}
		ConversationDetailDto conversationDetailDto = ConversationMapper.INSTANCE.fromConversationToDetailDto(conversation);
		
		log.info("Conversation detail: {}", conversationDetailDto);
		
		List<String> messageIds = conversation.getMessages();
		List<Message> messagesList = messageRepository.findMessagesByConversationId(conversationId);
		List<MessageResponseDTO> messages = new ArrayList<>();
		
		for (Message message : messagesList) {
			MessageResponseDTO messageResponseDTO = MessageMapper.INSTANCE.toMessageResponseDto(message);
			messages.add(messageResponseDTO);
		}
		
		
		conversationDetailDto.setMessageDetails((messages));
		
		messageNotifier.initConversation(conversationDetailDto, conversationId);
		
		return conversationDetailDto;
	}

	@Override
	public void updateLastUpdated(String conversationId) {
		// TODO Auto-generated method stub
		Conversation conversation = conversationRepository.findById(conversationId);
		if (conversation == null) {
			log.warn("Conversation with id {} not found", conversationId);
			throw new RuntimeException("Conversation not found");
		}
		conversation.setUpdatedAt(LocalDateTime.now());
		conversationRepository.save(conversation);
	}
	@Override
	public void markAllMessagesAsRead(String conversationId, String userId) {
		// TODO Auto-generated method stub
		
		Conversation conversation = conversationRepository.findById(conversationId);
		if (conversation == null) {
			log.warn("Conversation with id {} not found", conversationId);
			throw new RuntimeException("Conversation not found");
		}
		List<String> messageIds = conversation.getMessages();
		if (messageIds == null || messageIds.isEmpty()) {
			log.warn("No messages found in conversation with id {}", conversationId);
			return;
		}
		for (String messageId : messageIds) {
			Message message = messageRepository.getMessageById(messageId);
			if (message != null && !message.getSeenBy().contains(userId)) {
				message.getSeenBy().add(userId);
				messageRepository.save(message);
			}
		}
		// Notify the user about the read status
		messageNotifier.notifyAllMessagesRead(conversationId, userId);
		log.info("All messages in conversation {} marked as read by user {}", conversationId, userId);
	}

}
