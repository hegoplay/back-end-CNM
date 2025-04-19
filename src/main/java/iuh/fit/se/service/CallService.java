package iuh.fit.se.service;

import iuh.fit.se.model.Call;
import iuh.fit.se.model.enumObj.CallType;

// interface này có nhiệm vụ lưu trữ lịch sử cuộc gọi
public interface CallService {
	Call createCall(String callId, String initiatorPhone, String conversationId, CallType callType);
	void endCall(String callId);
}
