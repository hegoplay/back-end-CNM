package iuh.fit.se.repo;

import iuh.fit.se.model.dto.UserResponseDto;

import java.util.List;

public interface FriendRespository {
    List<UserResponseDto> findByPhone(String phone);


}
