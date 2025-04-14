package iuh.fit.se.repo;

import iuh.fit.se.model.User;
import iuh.fit.se.model.dto.UserResponseDto;

import java.util.List;

public interface FriendRespository {
    List<User> findByPhone(String phone);


}
