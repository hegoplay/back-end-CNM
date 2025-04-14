package iuh.fit.se.service;

import iuh.fit.se.model.dto.UserResponseDto;

import java.util.List;

public interface FriendService {
    List<UserResponseDto> getFriendsList(String phone);

    List<UserResponseDto> findPersonByPhone(String phone);

    List<UserResponseDto> findFriendsByName(String userPhone, String nameKeyword);
}