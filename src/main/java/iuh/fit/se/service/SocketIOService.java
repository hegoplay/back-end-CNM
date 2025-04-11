package iuh.fit.se.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;

import iuh.fit.se.util.JwtUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SocketIOService {

    @Autowired
    private SocketIOServer socketIOServer;

    @Autowired
    private JwtUtils jwtUtil;

    @PostConstruct
    public void start() {
        socketIOServer.addConnectListener(client -> {
            String token = client.getHandshakeData().getSingleUrlParam("token");
            String username = jwtUtil.getPhoneFromToken(token); // Token đã được xác thực ở AuthorizationListener
            log.info("Client connected: {} with username: {}", client.getSessionId(), username);
            client.set("username", username);

            client.sendEvent("message", "Welcome, " + username);
        });

        socketIOServer.addEventListener("message", String.class, (client, message, ackRequest) -> {
            String username = client.get("username");
            log.info("Received message from {}: {}", username, message);
            client.sendEvent("message", "Server received: " + message);
        });

        socketIOServer.addDisconnectListener(client -> {
            log.info("Client disconnected: {}", client.getSessionId());
        });

        socketIOServer.start();
    }

    @PreDestroy
    public void stop() {
        socketIOServer.stop();
    }

    public void sendMessageToClient(String username, String message) {
        for (SocketIOClient client : socketIOServer.getAllClients()) {
            if (username.equals(client.get("username"))) {
                client.sendEvent("message", message);
            }
        }
    }
}
