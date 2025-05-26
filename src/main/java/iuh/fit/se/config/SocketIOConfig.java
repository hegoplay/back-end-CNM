package iuh.fit.se.config;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.Transport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import iuh.fit.se.util.JwtUtils;

@Configuration
public class SocketIOConfig {

    @Value("${socketio.port}")
    private int socketIoPort;

    @Value("${server.ssl.key-store}")
    private String keyStorePath;
    
    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;
    
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
    SocketIOServer socketIOServer() throws Exception {
    	com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname("0.0.0.0");
        config.setPort(socketIoPort);
        
        List<String> allowedOrigins = Arrays.asList(
                "https://localhost:3000",
                "http://localhost:19000",
                "exp://192.168.*:*"
            );
        
        config.setOrigin("*"); // Cho phép tất cả nguồn gốc (có thể thay đổi theo nhu cầu)

        // Cấu hình transport với SSL
        SocketConfig socketConfig = new SocketConfig();
        socketConfig.setReuseAddress(true);
        config.setSocketConfig(socketConfig);
        
        config.setTransports(Transport.WEBSOCKET, Transport.POLLING);
        
        // Cấu hình HTTPS cho phiên bản 2.x
        // Tạo KeyStore từ file
        config.setKeyStore(getKeyStoreInputStream());
        config.setKeyStorePassword(keyStorePassword);
        config.setKeyStoreFormat("PKCS12"); // Thay đổi từ JKS -> PKCS12

        // Áp dụng SSLContext vào config (QUAN TRỌNG)
        config.setSSLProtocol("TLS");
        
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
    
    private InputStream getKeyStoreInputStream() {
        String keystoreFile = keyStorePath.replace("classpath:", "");
        InputStream stream = getClass().getClassLoader().getResourceAsStream(keystoreFile);
        if (stream == null) {
            throw new RuntimeException("Keystore not found: " + keyStorePath);
        }
        return stream;
    }
}