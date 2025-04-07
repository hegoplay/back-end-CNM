package iuh.fit.se.service;

import iuh.fit.se.model.dto.LoginRequest;
import iuh.fit.se.model.dto.LoginResponse;
import iuh.fit.se.model.dto.RegisterRequest;

public interface UserService {
	boolean isExistPhone(String phone);
	
	void createUser(RegisterRequest request, String avatarUrl);
	
	LoginResponse login(LoginRequest request);
}
