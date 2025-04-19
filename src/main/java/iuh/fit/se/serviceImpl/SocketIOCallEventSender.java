package iuh.fit.se.serviceImpl;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SocketIOCallEventSender {

    private final SocketIOServer socketIOServer;

    public void sendToRoom(String roomId, String eventName, Object data) {
        socketIOServer.getRoomOperations(roomId).sendEvent(eventName, data);
        log.debug("Sent event {} to room {}: {}", eventName, roomId, data);
    }

    public void sendToClient(String username, String eventName, Object data,
                             Map<String, SocketIOClient> callClients) {
        SocketIOClient client = callClients.get(username);
        if (client != null) {
            client.sendEvent(eventName, data);
            log.debug("Sent event {} to client {}: {}", eventName, username, data);
        } else {
            log.warn("Client {} not found for event {}", username, eventName);
        }
    }

    public void broadcastToRoomExceptSender(String roomId, UUID senderSessionId,
                                           String eventName, Object data) {
        socketIOServer.getRoomOperations(roomId).getClients().stream()
                .filter(c -> !c.getSessionId().equals(senderSessionId))
                .forEach(c -> c.sendEvent(eventName, data));
    }
}