package iuh.fit.se.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.JwtException;
import iuh.fit.se.mapper.UserMapper;
import iuh.fit.se.model.User;
import iuh.fit.se.model.dto.UserResponseDto;
import iuh.fit.se.model.dto.auth.LoginRequest;
import iuh.fit.se.model.dto.auth.LoginResponse;
import iuh.fit.se.model.dto.user.UserUpdateRequest;
import iuh.fit.se.model.dto.user.UserUpdateRequestJSON;
import iuh.fit.se.repo.UserRepository;
import iuh.fit.se.util.JwtUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
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

	private final DynamoDbTable<User> userTable;

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
	public void createUser(iuh.fit.se.model.dto.auth.RegisterRequest request, String avatarUrl) {

		log.info("register user: {}", request);

		User user = User.builder().phoneNumber(request.getPhone())
				.password(passwordEncoder.encode(request.getPassword()).toString()).baseImg(avatarUrl)
				.isMale(request.isGender()).dateOfBirth(request.getDateOfBirth()).name(request.getName())
				.status("offline").isOnline(false).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
				.requestList(new java.util.ArrayList<>()).friends(new java.util.ArrayList<>())
				.pendings(new java.util.ArrayList<>()).mutedConversation(new java.util.ArrayList<>())
				.currentCallId(null).callSettings(new User.CallSettings(false, false, false, false)).build();

		log.info("User created: {}", user);
		userRepository.save(user);
	}

	@Override
	public LoginResponse login(LoginRequest request) {
		// TODO Auto-generated method stub
//		return null;
		User user = userRepository.findByPhone(request.getPhone());
		if (user == null) {
			return null;
		}

		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			return null;
		}

		user.setOnline(true);
		user.setStatus("online");
		user.setLastOnlineTime(LocalDateTime.now());
		user.setUpdatedAt(LocalDateTime.now());

		userRepository.save(user);

		return new LoginResponse(request.getPhone(), jwtUtils.generateTokenFromPhone(request.getPhone()));
	}

	@Override
	public void updatePassword(String phone, String password) {
		// TODO Auto-generated method stub

		User user = userRepository.findByPhone(phone);
		if (user == null) {
			throw new RuntimeException("User not found");
		}
		user.setPassword(passwordEncoder.encode(password));
		user.setUpdatedAt(LocalDateTime.now());
		userRepository.save(user);
		log.info("User password updated: {}", user);
	}

	@Override
	public UserResponseDto getUserInfo(String phone) {
		// TODO Auto-generated method stub
		User user = userRepository.findByPhone(phone);
		UserResponseDto dto = UserMapper.INSTANCE.toUserResponseDto(user);
		return dto;
	}

	@Override
	public UserResponseDto updateUserInfo(String phone, UserUpdateRequest request) {
		// TODO Auto-generated method stub
		User user = userRepository.findByPhone(phone);
		if (user == null) {
			throw new RuntimeException("User not found");
		}
		User updatedUser = UserMapper.INSTANCE.fromUserUpdateRequestMapToUser(request, user);
		log.info("User updated: {}", updatedUser);
		updatedUser.setUpdatedAt(LocalDateTime.now());
		userRepository.save(updatedUser);
		UserResponseDto dto = UserMapper.INSTANCE.toUserResponseDto(updatedUser);
		log.info("User response: {}", dto);
		return dto;

	}

	@Override
	public UserResponseDto updateUserInfo(String phone, UserUpdateRequestJSON request) {
		// TODO Auto-generated method stub
		User user = userRepository.findByPhone(phone);
		if (user == null) {
			throw new RuntimeException("User not found");
		}
		User updatedUser = UserMapper.INSTANCE.fromUserUpdateRequestJSONMapToUser(request, user);
		log.info("User updated: {}", updatedUser);
		updatedUser.setUpdatedAt(LocalDateTime.now());
		userRepository.save(updatedUser);
		UserResponseDto dto = UserMapper.INSTANCE.toUserResponseDto(updatedUser);
		log.info("User response: {}", dto);
		return dto;
	}

	@Override
	public void updateUserStatus(String phone, String status) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteUser(String phone) {
		// TODO Auto-generated method stub
		userRepository.deleteByPhone(phone);
	}

	@Override
	public void logout(String phone) {
		// TODO Auto-generated method stub
		User user = userRepository.findByPhone(phone);
		if (user != null) {
			user.setOnline(false);
			user.setStatus("offline");
			user.setUpdatedAt(LocalDateTime.now());
			userRepository.save(user);
		} else {
			throw new RuntimeException("User not found");
		}

	}

	@Override
	public void changePassword(String jwt, String phone, String password) {
		// TODO Auto-generated method stub
		String phoneFromToken;
		try {
			phoneFromToken = jwtUtils.getPhoneFromToken(jwt);
		} catch (JwtException e) {
			throw new RuntimeException("Invalid or expired token");
		}

		// Kiểm tra quyền
		if (!phoneFromToken.equals(phoneFromToken)) {
			throw new RuntimeException("You can only change your own password");
		}

		// Cập nhật mật khẩu
		updatePassword(phoneFromToken, phone);
		log.info("Password changed for: {}", phone);
	}

	@Override
	public User getUserFromToken(String token) {
		try {
			String phoneNumber = jwtUtils.getPhoneFromToken(token);
			log.info("Extracted phone number from token: {}", phoneNumber);

			User user = userTable.getItem(r -> r.key(k -> k.partitionValue(phoneNumber)));

			if (user == null) {
				log.warn("User with phone number {} not found in database.", phoneNumber);
			} else {
				log.info("User found: {}", user);
			}

			return user;
		} catch (Exception e) {
			log.error("Error extracting user from token: ", e);
			return null;
		}
	}

	@Override
	public List<UserResponseDto> getFriends(String phone) {
		// TODO Auto-generated method stub
		User user = userRepository.findByPhone(phone);
		if (user != null) {
			List<UserResponseDto> friends = user.getFriends().stream()
					.map(friendPhone -> UserMapper.INSTANCE.toUserResponseDto(userRepository.findByPhone(friendPhone)))
					.toList();
			return friends;
		}
		else {
			throw new RuntimeException("User not found");
		}
	}

	@Override
	public void acceptRequest(String phone, String friendPhone) {

		// TODO Auto-generated method stub

		User user = userRepository.findByPhone(phone);
		User friend = userRepository.findByPhone(friendPhone);

		if (user == null || friend == null) {
			throw new RuntimeException("User or friend not found");
		}

		if (user.getPendings().contains(friendPhone)) {
			user.getPendings().remove(friendPhone);
			user.getFriends().add(friendPhone);
			friend.getFriends().add(phone);
			userRepository.save(user);
			userRepository.save(friend);

		} else {

			throw new RuntimeException("Friend request not found");

		}

	}

}
