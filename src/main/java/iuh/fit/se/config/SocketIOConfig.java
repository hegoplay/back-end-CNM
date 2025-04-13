package iuh.fit.se.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import iuh.fit.se.util.JwtUtils;

@Configuration
public class SocketIOConfig {

    @Value("${socketio.port}")
    private int socketIoPort;

    @Autowired
    private JwtUtils jwtUtil;

    @Bean
    ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }

    @Bean
    SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname("0.0.0.0");
        config.setPort(socketIoPort);

        // Create JacksonJsonSupport with your custom ObjectMapper
        JacksonJsonSupport jsonSupport = new JacksonJsonSupport();
        try {
            // Use reflection to set the ObjectMapper since there's no setter method
            java.lang.reflect.Field mapperField = JacksonJsonSupport.class.getDeclaredField("objectMapper");
            mapperField.setAccessible(true);
            mapperField.set(jsonSupport, objectMapper());
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure JacksonJsonSupport", e);
        }
        config.setJsonSupport(jsonSupport);

        // JWT Authentication
        config.setAuthorizationListener(data -> {
            String token = data.getSingleUrlParam("token");
            if (token != null && jwtUtil.validateToken(token)) {
                return AuthorizationResult.SUCCESSFUL_AUTHORIZATION;
            }
            return AuthorizationResult.FAILED_AUTHORIZATION;
        });

        return new SocketIOServer(config);
    }
}