package iuh.fit.se.service;

import iuh.fit.se.model.dto.UserResponseDto;
import iuh.fit.se.model.dto.search.FindPeopleByNameKeywordResponse;

import java.util.List;

public interface FriendService {
    List<UserResponseDto> getFriendsList(String phone);

    List<UserResponseDto> findPersonByPhone(String phone);

    FindPeopleByNameKeywordResponse findPeopleByNameKeyword(String userPhone, String nameKeyword);
}