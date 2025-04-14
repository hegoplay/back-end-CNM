package iuh.fit.se.serviceImpl;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import iuh.fit.se.mapper.MessageMapper;
import iuh.fit.se.model.Conversation;
import iuh.fit.se.model.Message;
import iuh.fit.se.model.dto.message.MessageRequestDTO;
import iuh.fit.se.model.dto.message.MessageResponseDTO;
import iuh.fit.se.model.enumObj.MessageType;
import iuh.fit.se.repo.ConversationRepository;
import iuh.fit.se.repo.MessageRepository;
import iuh.fit.se.service.ConversationService;
import iuh.fit.se.service.MessageNotifier;
import iuh.fit.se.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageServiceAWSImpl implements MessageService {
    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final ConversationRepository conversationRepository;
    private final MessageNotifier messageNotifier; // Thay thế SocketIOService bằng interface
    private final AwsService awsService;

    @Override
    public MessageResponseDTO sendTextMessage(MessageRequestDTO request) {
        validateMessageType(request, MessageType.TEXT);
        Message message = createAndSaveMessage(request);
        updateConversation(message);
        notifyParticipants(message);
        return MessageMapper.INSTANCE.toMessageResponseDto(message);
    }

    @Override
    public MessageResponseDTO sendMediaMessage(MessageRequestDTO request, MultipartFile mediaFile) {
        validateMessageType(request, MessageType.MEDIA);
        try {
            String mediaUrl = awsService.uploadToS3(mediaFile);
            request.setContent(mediaUrl);
            Message message = createAndSaveMessage(request);
            updateConversation(message);
            notifyParticipants(message);
            return MessageMapper.INSTANCE.toMessageResponseDto(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload media file", e);
        }
    }

    @Override
    public MessageResponseDTO sendFileMessage(MessageRequestDTO request, MultipartFile file) {
        try {
            validateMessageType(request, MessageType.FILE, MessageType.MEDIA);
            String fileUrl = awsService.uploadToS3(file);
            request.setContent(fileUrl);
            Message message = createAndSaveMessage(request);
            updateConversation(message);
            notifyParticipants(message);
            return MessageMapper.INSTANCE.toMessageResponseDto(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @Override
    public MessageResponseDTO sendCallEventMessage(MessageRequestDTO request) {
        validateMessageType(request, MessageType.CALL);
        if (request.getCallEvent() == null) {
            throw new IllegalArgumentException("Call event data is required");
        }
        Message message = createAndSaveMessage(request);
        notifyParticipants(message);
        return MessageMapper.INSTANCE.toMessageResponseDto(message);
    }

    @Override
    public void recallMessage(String messageId, String userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("Only sender can recall message");
        }

        message.setRecalled(true);
        messageRepository.save(message);
//      xóa media hoặc file đính kèm
        if (message.getType() == MessageType.MEDIA || message.getType() == MessageType.FILE) {
			awsService.deleteFromS3PrefixCloudFront(message.getContent());
		}
        
        // Sử dụng MessageNotifier thay vì SocketIOService trực tiếp
        messageNotifier.notifyMessageRecalled(message.getConversationId(), messageId);
    }

    @Override
    public MessageResponseDTO reactToMessage(String messageId, String userId, String emoji) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Initialize reactions list if null
        if (message.getReactions() == null) {
            message.setReactions(new ArrayList<>());
        }

        // Add or update reaction
        message.getReactions().stream()
                .filter(r -> r.getEmoji().equals(emoji))
                .findFirst()
                .ifPresentOrElse(
                        r -> r.getUsers().add(userId),
                        () -> message.getReactions().add(
                                new Message.Reaction(emoji, List.of(userId))
                        )
                );

        messageRepository.save(message);
        
        // Sử dụng MessageNotifier
        messageNotifier.notifyReactionAdded(message.getConversationId(), messageId, emoji, userId);
        
        return MessageMapper.INSTANCE.toMessageResponseDto(message);
    }

    @Override
    public List<MessageResponseDTO> getMessagesByConversation(String conversationId) {
        if (!conversationRepository.existsById(conversationId)) {
            throw new RuntimeException("Conversation not found");
        }
        
        Conversation conversation = conversationRepository.findById(conversationId);
        
        if (conversation.getMessages().isEmpty()) {
            return new ArrayList<>();
        }

        return conversation.getMessages().stream()
                .map(messageRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(MessageMapper.INSTANCE::toMessageResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public MessageResponseDTO getMessageById(String messageId) {
        return messageRepository.findById(messageId)
                .map(MessageMapper.INSTANCE::toMessageResponseDto)
                .orElseThrow(() -> new RuntimeException("Message not found"));
    }

    private Message createAndSaveMessage(MessageRequestDTO request) {
        validateMessageRequest(request);
        
        Message message = MessageMapper.INSTANCE.fromMessageRequestDto(request);
        
        // Set additional fields not mapped by MapStruct
        if (message.getId() == null) {
            message.setId(UUID.randomUUID().toString());
        }
        message.setCreatedAt(LocalDateTime.now());
        message.setReactions(Optional.ofNullable(message.getReactions()).orElse(new ArrayList<>()));
        message.setSeenBy(Optional.ofNullable(message.getSeenBy()).orElse(new ArrayList<>()));
        message.setRecalled(false);

        messageRepository.save(message);
        conversationService.updateLastUpdated(request.getConversationId());
        
        return message;
    }

    private void validateMessageRequest(MessageRequestDTO request) {
        // Check conversation exists
        if (!conversationRepository.existsById(request.getConversationId())) {
            throw new RuntimeException("Conversation not found");
        }

        // Check sender is in conversation
        Conversation conversation = conversationRepository.findById(request.getConversationId());
        if (!conversation.getParticipants().contains(request.getSenderId())) {
            throw new RuntimeException("Sender not in conversation");
        }
    }

    private void validateMessageType(MessageRequestDTO request, MessageType ...expectedType) {
        if (request.getType() == null) {
			throw new IllegalArgumentException("Message type is required");
		}

		if (!Arrays.asList(expectedType).contains(request.getType())) {
			throw new IllegalArgumentException("Invalid message type");
		}
    }

    private void notifyParticipants(Message message) {
        MessageResponseDTO response = MessageMapper.INSTANCE.toMessageResponseDto(message);
        messageNotifier.notifyNewMessage(response);
    }
    private void updateConversation(Message msg) {
		Conversation conversation = conversationRepository.findById(msg.getConversationId());
		if (conversation == null) {
			throw new RuntimeException("Conversation not found");
		}
		conversation.setUpdatedAt(LocalDateTime.now());
		conversation.addMessageId(msg.getId());
		conversationRepository.save(conversation);
	}

	@Override
	public MessageResponseDTO markMessageAsRead(String messageId, String userId) {
		// TODO Auto-generated method stub
		Message message = messageRepository.findById(messageId)
				.orElseThrow(() -> new RuntimeException("Message not found"));
		if (message.getSeenBy() == null) {
			message.setSeenBy(new ArrayList<>());
		}
		if (!message.getSeenBy().contains(userId)) {
			message.getSeenBy().add(userId);
		}
		messageRepository.save(message);
		// Sử dụng MessageNotifier cái này lập trình sau
		
		// messageNotifier.notifyMessageRead(message.getConversationId(), messageId, userId);
		
		return MessageMapper.INSTANCE.toMessageResponseDto(message);
		
	
	}

}