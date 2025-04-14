package iuh.fit.se.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import iuh.fit.se.model.dto.message.MessageRequestDTO;
import iuh.fit.se.model.dto.message.MessageResponseDTO;

public interface MessageService {
	MessageResponseDTO sendTextMessage(MessageRequestDTO request);
    void recallMessage(String messageId, String userId);
    MessageResponseDTO reactToMessage(String messageId, String userId, String emoji);
    List<MessageResponseDTO> getMessagesByConversation(String conversationId);
    MessageResponseDTO getMessageById(String messageId);
	MessageResponseDTO sendMediaMessage(MessageRequestDTO request, MultipartFile mediaFile);
	
	MessageResponseDTO sendFileMessage(MessageRequestDTO request, MultipartFile file);
	MessageResponseDTO sendCallEventMessage(MessageRequestDTO request);
//	MessageResponseDTO sendLocationMessage(MessageRequestDTO request);
	MessageResponseDTO markMessageAsRead(String messageId, String userId);
	
	
//	List<MessageResponseDTO> getMessagesByUser(String userId);
//	List<MessageResponseDTO> getMessagesByGroup(String groupId);
//	List<MessageResponseDTO> getMessagesByUserAndGroup(String userId, String groupId);
//	List<MessageResponseDTO> getMessagesByUserAndConversation(String userId, String conversationId);
//	List<MessageResponseDTO> getMessagesByGroupAndConversation(String groupId, String conversationId);
}