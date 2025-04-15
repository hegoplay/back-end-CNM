package iuh.fit.se.serviceImpl;

import iuh.fit.se.mapper.UserMapper;
import iuh.fit.se.model.User;
import iuh.fit.se.model.dto.UserResponseDto;
import iuh.fit.se.model.dto.search.FindPeopleByNameKeywordResponse;
import iuh.fit.se.repo.FriendRespository;
import iuh.fit.se.repo.UserRepository;
import iuh.fit.se.service.FriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendServiceAWSImpl implements FriendService {

    // Injecting the repository
    private final FriendRespository friendRepository;
    private final UserRepository userRepository;

    @Override
    public List<UserResponseDto> getFriendsList(String phone) {
        List<UserResponseDto> friendsList = new ArrayList<>();
        List<String> friendPhoneList = userRepository.findByPhone(phone).getFriends();
        for(String friendPhone : friendPhoneList) {
            User user = userRepository.findByPhone(friendPhone);
            UserResponseDto dto = UserMapper.INSTANCE.toUserResponseDto(user);
            friendsList.add(dto);
        }
        return friendsList;
    }

    @Override
    public List<UserResponseDto> findPersonByPhone(String phone) {
        List<User> resultObjectList = friendRepository.findByPhone(phone);
        List<UserResponseDto> resultDtoList = new ArrayList<>();
        for(User user : resultObjectList) {
            UserResponseDto dto = UserMapper.INSTANCE.toUserResponseDto(user);
            resultDtoList.add(dto);
        }
        return resultDtoList;
    }

    @Override
    public FindPeopleByNameKeywordResponse findPeopleByNameKeyword(String userPhone, String nameKeyword) {
        List<UserResponseDto> matchedFriends = new ArrayList<>();
        List<FindPeopleByNameKeywordResponse.UserWithSharedGroups> matchedOthersWithSharedGroups = new ArrayList<>();
        List<UserResponseDto> matchedContacted = new ArrayList<>();

        List<String> friendPhoneList = userRepository.findByPhone(userPhone).getFriends();
        if (friendPhoneList != null) {
            log.info("Found {} friends", friendPhoneList.size());
        }

        for (String friendPhone : friendPhoneList) {
            User friendUser = userRepository.findByPhone(friendPhone);
            if (friendUser == null) {
                log.info("Friend null: {}", friendPhone);
                continue;
            }

            if (friendUser.getName().contains(nameKeyword)) {
                UserResponseDto friendInfo = UserMapper.INSTANCE.toUserResponseDto(friendUser);
                matchedFriends.add(friendInfo);

                // Sample: Add to matchedOthersWithSharedGroups with dummy group name
                List<String> dummyGroups = new ArrayList<>();
                dummyGroups.add("same group");
                matchedOthersWithSharedGroups.add(new FindPeopleByNameKeywordResponse.UserWithSharedGroups(friendInfo, dummyGroups));

                // Sample: Add to matchedContacted
                matchedContacted.add(friendInfo);
            }
        }

        return new FindPeopleByNameKeywordResponse(matchedFriends, matchedOthersWithSharedGroups, matchedContacted);
    }
}
