package iuh.fit.se.service;

import java.util.List;

import iuh.fit.se.model.User;
import iuh.fit.se.model.dto.UserResponseDto;
import iuh.fit.se.model.dto.auth.LoginRequest;
import iuh.fit.se.model.dto.auth.LoginResponse;
import iuh.fit.se.model.dto.auth.RegisterRequest;
import iuh.fit.se.model.dto.user.UserUpdateRequest;
import iuh.fit.se.model.dto.user.UserUpdateRequestJSON;

public interface UserService {
	boolean isExistPhone(String phone);
	
	void createUser(RegisterRequest request, String avatarUrl);
	
	LoginResponse login(LoginRequest request);
	
	void updatePassword(String phone, String password);
	
	UserResponseDto getUserInfo(String phone);
	
	UserResponseDto updateUserInfo(String phone, UserUpdateRequest request);
	
	UserResponseDto updateUserInfo(String phone, UserUpdateRequestJSON request);
	
	void updateUserStatus(String phone, String status);
	
	void deleteUser(String phone);
	
	void logout(String phone);
	
	void changePassword(String jwt, String phone, String password);
	
	User getUserFromToken(String jwt);
	
	List<UserResponseDto> getFriends(String phone);

	void acceptRequest(String phone, String friendPhone);
}
