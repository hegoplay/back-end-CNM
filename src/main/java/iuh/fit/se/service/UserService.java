package iuh.fit.se.service;

import iuh.fit.se.model.dto.UserResponseDto;
import iuh.fit.se.model.dto.auth.LoginRequest;
import iuh.fit.se.model.dto.auth.LoginResponse;
import iuh.fit.se.model.dto.auth.RegisterRequest;

public interface UserService {
	boolean isExistPhone(String phone);
	
	void createUser(RegisterRequest request, String avatarUrl);
	
	LoginResponse login(LoginRequest request);
	
	void updatePassword(String phone, String password);
	
	UserResponseDto getUserInfo(String phone);
}
