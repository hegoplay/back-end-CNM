package iuh.fit.se.serviceImpl;

import java.sql.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import iuh.fit.se.model.User;
import iuh.fit.se.model.dto.LoginRequest;
import iuh.fit.se.model.dto.LoginResponse;
import iuh.fit.se.repo.UserRepository;
import iuh.fit.se.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceAWSImpl implements iuh.fit.se.service.UserService {

	@Value("${aws.region}")
	private String region;

	private final DynamoDbClient dynamoDbClient;
//	private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
	private final PasswordEncoder passwordEncoder;
	private final UserRepository userRepository;
	private final JwtUtils jwtUtils;

	@Override
	public boolean isExistPhone(String phone) {
		// TODO Auto-generated method stub
		try {
			// Giả sử bạn có bảng 'Users' với partition key là 'phone'
			return userRepository.existsByPhone(phone);
		} catch (DynamoDbException e) {
			// Xử lý lỗi nếu cần
			log.error("Error checking phone existence: {}", e.getMessage());
			throw new RuntimeException("Error checking phone existence", e);
		}
	}

	@Override
	public void createUser(iuh.fit.se.model.dto.RegisterRequest request, String avatarUrl) {

		
		log.info("register user: {}", request);
		
		
		
		User user = User.builder()
				.phoneNumber(request.getPhone())
				.password(passwordEncoder.encode(request.getPassword()).toString())
				.baseImg(avatarUrl)
				.gender(request.isGender())
				.dateOfBirth(request.getDateOfBirth())
				.name(request.getName())
				.status("offline")
				.isOnline(false)
				.createdAt(new java.util.Date().toString())
				.updatedAt(new java.util.Date().toString())
				.requestList(new java.util.ArrayList<>())
				.friends(new java.util.ArrayList<>())
				.pendings(new java.util.ArrayList<>())
				.mutedConversation(new java.util.ArrayList<>())
				.currentCallId(null)
				.callSettings(new User.CallSettings(false, false, false, false))
				.build();

		log.info("User created: {}", user);
		userRepository.save(user);
	}

	@Override
	public LoginResponse login(LoginRequest request) {
		// TODO Auto-generated method stub
//		return null;
		User user = userRepository.findByPhone(request.getPhone());
		if (user == null) {
			log.error("User not found with phone: {}", request.getPhone());
			return null;
		}

		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			log.error("Invalid password for phone: {}", request.getPhone());
			return null;
		}

		user.setOnline(true);
		user.setStatus("online");
		user.setLastOnlineTime(new java.util.Date().toString());
		user.setUpdatedAt(new java.util.Date().toString());

		userRepository.save(user);

		return new LoginResponse(request.getPhone(),jwtUtils.generateTokenFromUsername(request.getPhone()));
	}

	@Override
	public void updatePassword(String phone, String password) {
		// TODO Auto-generated method stub
		
		User user = userRepository.findByPhone(phone);
		if (user == null) {
			throw new RuntimeException("User not found");
		}
		user.setPassword(passwordEncoder.encode(password));
		user.setUpdatedAt(new java.util.Date().toString());
		userRepository.save(user);
		log.info("User password updated: {}", user);
	}

}
