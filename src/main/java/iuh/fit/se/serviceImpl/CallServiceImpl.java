package iuh.fit.se.serviceImpl;


import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import iuh.fit.se.model.Call;
import iuh.fit.se.model.Conversation;
import iuh.fit.se.model.User;
import iuh.fit.se.model.enumObj.CallStatus;
import iuh.fit.se.model.enumObj.CallType;
import iuh.fit.se.repo.CallRepository;
import iuh.fit.se.repo.ConversationRepository;
import iuh.fit.se.repo.UserRepository;
import iuh.fit.se.service.CallService;
import iuh.fit.se.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CallServiceImpl implements CallService {

	CallRepository callRepository;
//	CallMapper callMapper;
	ConversationRepository conversationRepository;
	UserService userService;

	@Override
	public Call createCall(String callId, String initiatorPhone, String conversationId, CallType callType) {
		// TODO Auto-generated method stub
		// 1. Chuyển đổi DTO thành entity
		Call call = new Call();
		call.setId(callId);
		call.setStatus(CallStatus.PENDING);
		call.setStartTime(LocalDateTime.now());
		call.setCallType(callType);
		call.setInitiatorId(initiatorPhone);
		call.setConversationId(conversationId);
		Conversation conversation = conversationRepository
				.findById(call.getConversationId());
		if (conversation == null) {
			throw new RuntimeException("Conversation not found");
		}
		List<String> participantIds = conversation.getParticipants().stream()
				.filter(p -> p != initiatorPhone)
				.toList();
		call.setParticipants(participantIds);
		// 2. Lưu cuộc gọi vào cơ sở dữ liệu
		callRepository.createCall(call);
		
		// 3. Cập nhật trạng thái cuộc gọi đối với user
		userService.updateCallStatus(initiatorPhone, callId);
		
		// 3. Cập nhật trạng thái cuộc gọi trong Conversation
		conversation.setCurrentCallId(call.getId());
		conversation.setCallInProgress(true);
		conversation.setUpdatedAt(LocalDateTime.now());
		conversationRepository.save(conversation);
		
		return call;
	}

	@Override
	public void endCall(String callId) {
		// TODO Auto-generated method stub
		Call call = callRepository.findById(callId).orElse(null);
        if (call != null) {
			call.setStatus(CallStatus.ENDED);
			call.setEndTime(LocalDateTime.now());
			callRepository.updateCall(call);
			log.info("Call {} ended", callId);
		}
        Conversation conversation = conversationRepository.findById(call.getConversationId());
        if (conversation != null) {
        	conversation.setCallInProgress(false);
        	conversation.setCurrentCallId(null);
        	conversationRepository.save(conversation);
        }
        // Gửi sự kiện call_ended đến tất cả user trong room (addition)
        
        
	}

}
