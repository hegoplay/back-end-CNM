package iuh.fit.se.serviceImpl;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;

import iuh.fit.se.model.Call;
import iuh.fit.se.model.enumObj.CallStatus;
import iuh.fit.se.repo.CallRepository;
import iuh.fit.se.repo.ConversationRepository;
import iuh.fit.se.repo.UserRepository;
import iuh.fit.se.service.CallService;
import iuh.fit.se.service.UserService;
import iuh.fit.se.util.JwtUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SocketIOCallService {

    private final SocketIOServer socketIOServer;
    private final CallRepository callDynamoRepository;
    private final CallService callService;
    private final UserService userService;
    private final JwtUtils jwtUtils;
    

    private final Map<String, SocketIOClient> userClientMap = new ConcurrentHashMap<>(); // userId -> SocketIOClient
    private final Map<String, Set<String>> rooms = new ConcurrentHashMap<>(); // roomId -> Set<userId>
    private final Map<String, String> roomToCallId = new ConcurrentHashMap<>(); // roomId -> callId
    private final Map<String, String> roomToConversationId = new ConcurrentHashMap<>(); // roomId -> conversationId

    private static final String NAMESPACE = "/call";
    public static final String getPhone = "username";
    
    public SocketIONamespace getCallNamespace() {
        return socketIOServer.getNamespace(NAMESPACE);
    }

    @PostConstruct
	public void start() {
		SocketIONamespace callNamespace = socketIOServer.addNamespace(NAMESPACE);

		callNamespace.addConnectListener(client -> {
			String token = client.getHandshakeData().getSingleUrlParam("token");
			String roomId = client.getHandshakeData().getSingleUrlParam("roomId");
			try {
				String username = jwtUtils.getPhoneFromToken(token);
				client.set(getPhone, username);

				if (username == null || roomId == null) {
	                log.warn("Invalid connection: userId = {}, roomId = {}", username, roomId);
	                client.sendEvent("auth_error", "INVALID_DATA");
	                client.disconnect();
	                return;
	            }
				
				registerClient(username, client);
//				kiểm tra room
				int roomSize = callNamespace.getRoomOperations(roomId).getClients().size();
				Set<String> roomUsers = rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet());
				if (roomSize >= 2) {
	                log.warn("Room {} is full", roomId);
	                client.sendEvent("room_full");
	                client.disconnect();
	                return;
	            }
				roomUsers.add(username);
				client.joinRoom(roomId);
				log.info("User {} joined room {} on connect", username, roomId);
				
				// Gửi sự kiện user_joined đến các user khác trong room
				getCallNamespace().getRoomOperations(roomId).getClients().stream()
                .filter(c -> !getUserIdFromClient(c).equals(username))
                .forEach(c -> c.sendEvent("user_joined", Map.of("userId", username)));
				
				// Nếu đây là user thứ 2 (User B), cập nhật trạng thái cuộc gọi
				String callId = roomToCallId.get(roomId);
				String conversationId = roomToConversationId.get(roomId);
				if (roomUsers.size() == 2) {
	                if (callId != null && conversationId != null) {
	                    Call call = callDynamoRepository.findById(callId).orElse(null);
	                    call.setStatus(CallStatus.ONGOING);
	                    
	                   log.info("Call {} is now ONGOING in room {}", callId, roomId);
	                } else {
	                    log.warn("Call or conversation not found for room {}", roomId);
	                }
	            }
				if (roomUsers.size() >=2) {
					userService.updateCallStatus(username, callId);
				}

			} catch (Exception e) {
				log.error("Authentication failed: {}", e.getMessage());
				client.sendEvent("auth_error", "INVALID_TOKEN");
				client.disconnect();
			}
		});

		callNamespace.addDisconnectListener(client -> {
			String username = getUserIdFromClient(client);
			if (username != null) {
				log.info("Client disconnected: {} - {}", client.getSessionId(),
						username);
			}
//			messageNotifier.removeClient(username);
			handleLeaveCall(client, username);	
		});

		// Xử lý offer
        callNamespace.addEventListener("offer", Map.class, (client, data, ackSender) -> {
            String senderId = getUserIdFromClient(client);
            String targetUserId = (String) data.get("targetUserId");
            String roomId = (String) data.get("roomId");
            Object offer = data.get("offer");

            if (senderId == null || targetUserId == null || roomId == null) {
                log.warn("Invalid offer: senderId = {}, targetUserId = {}, roomId = {}", senderId, targetUserId, roomId);
                return;
            }

            SocketIOClient targetClient = userClientMap.get(targetUserId);
            if (targetClient != null) {
                targetClient.sendEvent("offer", Map.of(
                        "offer", offer,
                        "senderId", senderId
                ));
                log.info("Sent offer from {} to {} in room {}", senderId, targetUserId, roomId);
            } else {
                log.warn("Target user {} not found for offer in room {}", targetUserId, roomId);
            }
        });

        // Xử lý answer
        callNamespace.addEventListener("answer", Map.class, (client, data, ackSender) -> {
            String senderId = getUserIdFromClient(client);
            String targetUserId = (String) data.get("targetUserId");
            String roomId = (String) data.get("roomId");
            Object answer = data.get("answer");

            if (senderId == null || targetUserId == null || roomId == null) {
                log.warn("Invalid answer: senderId = {}, targetUserId = {}, roomId = {}", senderId, targetUserId, roomId);
                return;
            }

            SocketIOClient targetClient = userClientMap.get(targetUserId);
            if (targetClient != null) {
                targetClient.sendEvent("answer", Map.of(
                        "answer", answer,
                        "senderId", senderId
                ));
                log.info("Sent answer from {} to {} in room {}", senderId, targetUserId, roomId);
            } else {
                log.warn("Target user {} not found for answer in room {}", targetUserId, roomId);
            }
        });

        // Xử lý ICE candidate
        callNamespace.addEventListener("ice_candidate", Map.class, (client, data, ackSender) -> {
            String senderId = getUserIdFromClient(client);
            String targetUserId = (String) data.get("targetUserId");
            String roomId = (String) data.get("roomId");
            Object candidate = data.get("candidate");

            if (senderId == null || targetUserId == null || roomId == null) {
                log.warn("Invalid ice_candidate: senderId = {}, targetUserId = {}, roomId = {}", senderId, targetUserId, roomId);
                return;
            }

            SocketIOClient targetClient = userClientMap.get(targetUserId);
            if (targetClient != null) {
                targetClient.sendEvent("ice_candidate", Map.of(
                        "candidate", candidate,
                        "senderId", senderId
                ));
                log.info("Sent ICE candidate from {} to {} in room {}", senderId, targetUserId, roomId);
            } else {
                log.warn("Target user {} not found for ICE candidate in room {}", targetUserId, roomId);
            }
        });

        // Xử lý rời cuộc gọi
        callNamespace.addEventListener("leave_call", Map.class, (client, data, ackSender) -> {
            String userId = getUserIdFromClient(client);
            String roomId = (String) data.get("roomId");
            if (userId != null && roomId != null) {
                handleLeaveCall(client, userId, roomId);
            }
        });
	}
    
    

        
    

    // Đăng ký client
    public void registerClient(String userId, SocketIOClient client) {
        log.info("Registering client for userId: {}", userId);
        userClientMap.put(userId, client);
    }

    // Xóa client
    public void removeClient(String userId) {
        log.info("Removing client for userId: {}", userId);
        userClientMap.remove(userId);
    }

    // Xử lý rời cuộc gọi
    private void handleLeaveCall(SocketIOClient client, String userId) {
        String roomId = findRoomForUser(userId);
        if (roomId != null) {
            handleLeaveCall(client, userId, roomId);
        }
    }
//chưa có notify
    private void handleLeaveCall(SocketIOClient client, String userId, String roomId) {
        Set<String> roomUsers = rooms.get(roomId);
        if (roomUsers != null) {
            roomUsers.remove(userId);
            client.leaveRoom(roomId);
            log.info("User {} left room {}", userId, roomId);
            
            // Gửi sự kiện user_left đến các user còn lại
            getCallNamespace().getRoomOperations(roomId).getClients().stream()
                    .filter(c -> !getUserIdFromClient(c).equals(userId))
                    .forEach(c -> c.sendEvent("user_left", Map.of("userId", userId)));

            // Cập nhật trạng thái user
            String callId = roomToCallId.get(roomId);
//            if (callId != null) {
//                callDynamoRepository.updateUserCall(userId, null);
//            }
            userService.updateCallStatus(userId, null);
 
            // Nếu phòng rỗng, kết thúc cuộc gọi
            if (roomUsers.isEmpty()) {
                if (callId != null) {
                    callService.endCall(callId);
                	log.info("Call {} ended, room {} removed", callId, roomId);
                }
                
                rooms.remove(roomId);
                roomToCallId.remove(roomId);
                
            }
        }
    }

    // Tìm room của user
    private String findRoomForUser(String userId) {
        return 	rooms.entrySet().stream()
                .filter(entry -> entry.getValue().contains(userId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

 // Lấy conversationId từ callId (giả định)
    private String getConversationIdFromCall(String callId) {
        // Trong thực tế, cần truy vấn DynamoDB hoặc lưu ánh xạ
    	Call call = callDynamoRepository.findById(callId).orElse(null);
        return call.getConversationId();
    }

 // Lấy userId từ client
    private String getUserIdFromClient(SocketIOClient client) {
        return (String) client.get(getPhone);
    }


    // Lưu callId và roomId (gọi từ SocketIOChatService)
    public void storeCallMapping(String roomId, String callId, String conversationId) {
        roomToCallId.put(roomId, callId);
        roomToConversationId.put(roomId, conversationId);
    }
}