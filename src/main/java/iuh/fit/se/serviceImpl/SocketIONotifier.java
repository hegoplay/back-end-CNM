package iuh.fit.se.serviceImpl;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.corundumstudio.socketio.SocketIOServer;

import iuh.fit.se.model.dto.conversation.ConversationDetailDto;
import iuh.fit.se.model.dto.message.MessageResponseDTO;
import iuh.fit.se.service.MessageNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocketIONotifier implements MessageNotifier {
    private final SocketIOServer socketIOServer;
//    private com.corundumstudio.socketio.SocketIONamespace chatNamespace = socketIOServer.addNamespace("/chat");
    private final String NAMESPACE = "/chat";
    @Override
    public void notifyNewMessage(MessageResponseDTO message) {
        socketIOServer.addNamespace(NAMESPACE).getRoomOperations(message.getConversationId())
            .sendEvent("new_message", message);
    }

    @Override
    public void notifyMessageRecalled(String conversationId, String messageId) {
    	log.info("notifyMessageRecalled: conversationId = {}, messageId = {}", conversationId, messageId);	
        socketIOServer.addNamespace(NAMESPACE).getRoomOperations(conversationId)
            .sendEvent("message_recalled", Map.of(
                "messageId", messageId,
                "conversationId", conversationId
            ));
    }

    @Override
    public void notifyReactionAdded(String conversationId, String messageId, String emoji, String userId) {
        socketIOServer.addNamespace(NAMESPACE).getRoomOperations(conversationId)
            .sendEvent("reaction_added", Map.of(
                "messageId", messageId,
                "emoji", emoji,
                "userId", userId,
                "conversationId", conversationId
            ));
    }

	@Override
	public void initConversation(ConversationDetailDto conversationDetail, String userId) {
		// TODO Auto-generated method stub
		socketIOServer.addNamespace(NAMESPACE).getRoomOperations(conversationDetail.getId())
			.sendEvent("initial_messages", conversationDetail);
	}

	

}
