package iuh.fit.se.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
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
	private final ConversationRepository conversationRepository;
	private final MessageRepository messageRepository;
	private final UserRepository userRepository;
	private final DynamoDbTable<User> userTable;
	private final DynamoDbTable<Conversation> conversationTable;
	private final DynamoDbEnhancedClient enhancedClient;
	private final MessageNotifier messageNotifier;
	private final MessageMapper messageMapper;
	private final ConversationMapper conversationMapper;
	
	@Override
	public void createFriendConversation(String userPhone, String friendPhone) {
		// TODO Auto-generated method stub
		log.info("Start creating conversation");
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
						.conversationName("ban")
						.conversationImgUrl("xxx")
						.leader(userPhone)
						.admins(List.of(userPhone, friendPhone))
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
	        messageNotifier.notifyNewConversation(conversationDetailDto, userPhone);
	        messageNotifier.notifyNewConversation(conversationDetailDto, friendPhone);
	        
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
				ConversationDto conversationDto = conversationMapper.fromConversationToDto(conversation);
//		Thêm tin nhắn cuối vào conversationDto		
				appendLastMessageIntoConversationDto(conversationDto);
				reDefineConversationNameAndImgUrl(conversationDto, phone);
				
				
				
				conversationDtos.add(conversationDto);
			}
			return conversationDtos;
		} catch (Exception e) {
			log.error("Error getting conversations: {}", e.getMessage());
			throw new RuntimeException("Error getting conversations", e);
		}		
	}

	
	@Override
	public ConversationDetailDto getConversationDetail(String conversationId, String phone) {
		// TODO Auto-generated method stub
		Conversation conversation = conversationRepository.findById(conversationId);
		if (conversation == null) {
//				log.warn("Conversation with id {} not found", conversationId);
			throw new RuntimeException("Conversation not found");
		}
		log.info(conversation.toString());
		ConversationDetailDto conversationDetailDto = conversationMapper.fromConversationToDetailDto(conversation);
		
		log.info("Conversation detail: {}", conversationDetailDto);
		
		List<Message> messagesList = messageRepository.findMessagesByConversationId(conversationId);
		List<MessageResponseDTO> messages = new ArrayList<>();
		
		for (Message message : messagesList) {
			MessageResponseDTO messageResponseDTO = messageMapper.toMessageResponseDto(message);
			messages.add(messageResponseDTO);
		}
		
		conversationDetailDto.setMessageDetails((messages));
		
		reDefineConversationNameAndImgUrl(conversationDetailDto, phone);
		
		log.info("Conversation detail after redefine: {}", conversationDetailDto);
		
		return conversationDetailDto;
	}
	
	@Override
	public ConversationDetailDto getConversationDetail(String conversationId) {
		// TODO Auto-generated method stub
		Conversation conversation = conversationRepository.findById(conversationId);
		if (conversation == null) {
//				log.warn("Conversation with id {} not found", conversationId);
			throw new RuntimeException("Conversation not found");
		}
		log.info(conversation.toString());
		ConversationDetailDto conversationDetailDto = conversationMapper.fromConversationToDetailDto(conversation);
		
		log.info("Conversation detail: {}", conversationDetailDto);
		
		List<Message> messagesList = messageRepository.findMessagesByConversationId(conversationId);
		List<MessageResponseDTO> messages = new ArrayList<>();
		
		for (Message message : messagesList) {
			MessageResponseDTO messageResponseDTO = messageMapper.toMessageResponseDto(message);
			messages.add(messageResponseDTO);
		}
		
		conversationDetailDto.setMessageDetails((messages));
		
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
	@Override
	public void deleteFriendConversation(String userId, String friendId) {
		// TODO Auto-generated method stub
		String conversationId = userId + "_" + friendId;
		User user = userRepository.findByPhone(userId);
		if (user == null) {
			log.warn("User with id {} not found", userId);
			return;
		}
		User friend = userRepository.findByPhone(friendId);
		if (friend == null) {
			log.warn("User with id {} not found", friendId);
			return;
		}
		Conversation conversation = conversationRepository.findById(conversationId);
		if (conversation == null) {
			conversation = conversationRepository.findById(friendId + "_" + userId);
			if (conversation == null) {
				
				log.warn("Conversation with id {} not found", conversationId);
				return;
			}
		}
		if (user.getConversations() != null) {
			log.info("User conversation {}", user.getConversations());
//			log.info(conversationId);
			user.removeConversationId(conversation.getId());
			log.info("User conversation after delete {}", user.getConversations());
		}
		if (friend.getConversations() != null) {
			friend.removeConversationId(conversation.getId());;
		}
		
		messageRepository.deleteMessagesByConversationId(conversation.getId());
		conversationRepository.deleteById(conversation.getId());
		userRepository.save(user);
		userRepository.save(friend);
		messageNotifier.notifyRemoveConversation(conversation.getId(), userId);
		messageNotifier.notifyRemoveConversation(conversation.getId(), friendId);
	}

	private void appendLastMessageIntoConversationDto(ConversationDto dto) {
		if (dto.getMessages() == null || dto.getMessages().isEmpty()) {
			dto.setLastMessage(null);
			return;
		}
		String lastMessageId = dto.getMessages().get(dto.getMessages().size() - 1);
		if (lastMessageId != null) {
			Message lastMessage = messageRepository.getMessageById(lastMessageId);
			MessageResponseDTO lastMessageDto = messageMapper.toMessageResponseDto(lastMessage);
			dto.setLastMessage(lastMessageDto);
		}
	}
	
	private void reDefineConversationNameAndImgUrl(ConversationDetailDto conversationDto,
			String phone) {
			String conversationId = conversationDto.getId();
//			cài đặt lại conversation name và conversation img url
			if (conversationDto.getType() == ConversationType.PRIVATE) {
				String[] split = conversationId.split("_");
				String otherUserId = split[0].equals(phone) ? split[1] : split[0];
				User otherUser = userRepository.findByPhone(otherUserId);
				if (otherUser != null) {
					conversationDto.setConversationName(otherUser.getName());
					conversationDto.setConversationImgUrl(otherUser.getBaseImg());
				} else {
					log.warn("User with id {} not found", otherUserId);
				}
			}
	}
	
	private void reDefineConversationNameAndImgUrl(ConversationDto conversationDto,
			String phone) {
			String conversationId = conversationDto.getId();
//			cài đặt lại conversation name và conversation img url
			if (conversationDto.getType() == ConversationType.PRIVATE) {
				String[] split = conversationId.split("_");
				String otherUserId = split[0].equals(phone) ? split[1] : split[0];
				User otherUser = userRepository.findByPhone(otherUserId);
				if (otherUser != null) {
					conversationDto.setConversationName(otherUser.getName());
					conversationDto.setConversationImgUrl(otherUser.getBaseImg());
				} else {
					log.warn("User with id {} not found", otherUserId);
				}
			}
	}
	@Override
	public ConversationDto getConversationById(String conversationId) {
		// TODO Auto-generated method stub

	
		Conversation conversation = conversationRepository.findById(conversationId);
		if (conversation == null) {
			log.warn("Conversation with id {} not found", conversationId);
			throw new RuntimeException("Conversation not found");
		}
		ConversationDto conversationDto = conversationMapper.fromConversationToDto(conversation);
		appendLastMessageIntoConversationDto(conversationDto);
		return conversationDto;
	}
	@Override
	public void updateConversationInCall(String conversationId,
			boolean inCall) {
		// TODO Auto-generated method stub
		Conversation conversation = conversationRepository.findById(conversationId);
		if (conversation == null) {
			log.warn("Conversation with id {} not found", conversationId);
			throw new RuntimeException("Conversation not found");
		}
		conversation.setCallInProgress(inCall);
		conversationRepository.save(conversation);
		log.info("Conversation {} updated to inCall: {}", conversationId, inCall);
	}
	
}
