package iuh.fit.se.service;

import iuh.fit.se.model.dto.conversation.ConversationDetailDto;
import iuh.fit.se.model.dto.message.MessageResponseDTO;

public interface MessageNotifier {
    void notifyNewMessage(MessageResponseDTO message);
//    thu hồi tin nhắn
    void notifyMessageRecalled(String conversationId, String messageId);
    void notifyReactionAdded(String conversationId, String messageId, String emoji, String userId);
    void notifyAllMessagesRead(String conversationId, String userId);
    void initConversation(ConversationDetailDto conversationDetail, String userId);
    void notifyNewConversation(ConversationDetailDto conversationDetail, String userId);
    void notifyRemoveConversation(String conversationId, String userId);

    void notifyMemberAdded(String conversationId, String memberPhone);
    void notifyNewLeader(String conversationId, String memberPhone);
    void notifyMemberLeft(String conversationId, String memberPhone);
}
