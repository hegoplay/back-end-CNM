package iuh.fit.se.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.SocketIOServer;

import iuh.fit.se.util.JwtUtils;


@Configuration
public class SocketIOConfig {

    @Value("${socketio.port}")
    private int socketIoPort;

    @Autowired
    private JwtUtils jwtUtil;

    @Bean
    SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname("0.0.0.0");
        config.setPort(socketIoPort);

        // Xác thực JWT khi kết nối
        config.setAuthorizationListener(data -> {
            String token = data.getSingleUrlParam("token");
            if (token != null && jwtUtil.validateToken(token)) {
                return AuthorizationResult.SUCCESSFUL_AUTHORIZATION; // Cho phép kết nối
            } else {
                return AuthorizationResult.FAILED_AUTHORIZATION; // Từ chối kết nối
            }
        });

        return new SocketIOServer(config);
    }
}