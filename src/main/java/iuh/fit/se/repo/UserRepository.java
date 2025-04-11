package iuh.fit.se.repo;

import java.util.List;

import iuh.fit.se.model.User;

public interface UserRepository {
	
	User findByPhone(String phone);
	
	void save(User user);
	
	boolean existsByPhone(String phone);
	
	void deleteByPhone(String phone);
	
}
