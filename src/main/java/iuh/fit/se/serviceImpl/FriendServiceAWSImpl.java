package iuh.fit.se.serviceImpl;

import iuh.fit.se.mapper.UserMapper;
import iuh.fit.se.model.User;
import iuh.fit.se.model.dto.UserResponseDto;
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
        return friendRepository.findByPhone(phone);
    }

    @Override
    public List<UserResponseDto> findFriendsByName(String userPhone, String nameKeyword) {

        List<UserResponseDto> matchedUsers = new ArrayList<>();
        List<String> friendPhoneList = userRepository.findByPhone(userPhone).getFriends();
        if(friendPhoneList != null) {
            log.info("Found {} friends", friendPhoneList.size());
            System.out.println("Found " + friendPhoneList.size() + " friends");
        }
        for(String friendPhone : friendPhoneList){
            User friendUser = userRepository.findByPhone(friendPhone);

            if(friendUser.getName().contains(nameKeyword)){
                UserResponseDto friendInfo = UserMapper.INSTANCE.toUserResponseDto(friendUser);
                matchedUsers.add(friendInfo);
            }
        }
        return matchedUsers;
    }

}
