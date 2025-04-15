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
        List<User> resultObjectList = friendRepository.findByPhone(phone);
        List<UserResponseDto> resultDtoList = new ArrayList<>();
        for(User user : resultObjectList) {
            UserResponseDto dto = UserMapper.INSTANCE.toUserResponseDto(user);
            resultDtoList.add(dto);
        }
        return resultDtoList;
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
    
    @Override
    public void sendFriendRequest(String senderPhoneNumber, String receiverPhoneNumber) {

        // Chuẩn hóa sđt
        receiverPhoneNumber = formatPhoneNumber(receiverPhoneNumber);
        senderPhoneNumber = formatPhoneNumber(senderPhoneNumber);
        
        User sender = userRepository.findByPhone(senderPhoneNumber);
        if (sender == null) {
            throw new RuntimeException("Không tìm thấy người gửi: " + senderPhoneNumber);
        }
//        String phoneNumberReceiver = receiverPhoneNumber.trim().replaceAll("\\s+", "");
//        User receiver = userRepository.findByPhone(phoneNumberReceiver);

        User receiver = userRepository.findByPhone(receiverPhoneNumber);
        if (receiver == null) {
            throw new RuntimeException("Không tìm thấy người nhận: " + receiverPhoneNumber);
        }

        if (sender.getFriends().contains(receiverPhoneNumber)) {
            throw new RuntimeException("Hai người đã là bạn bè rồi!");
        }

        if (!receiver.getPendings().contains(senderPhoneNumber)) {
            receiver.getPendings().add(senderPhoneNumber);
            userRepository.save(receiver);
        }
        else {
			throw new RuntimeException("Đã gửi lời mời kết bạn đến người này rồi!");
		}
    }

    @Override
    public void acceptFriendRequest(String receiverPhoneNumber, String senderPhoneNumber) {

        // Chuẩn hóa số điện thoại
        receiverPhoneNumber = formatPhoneNumber(receiverPhoneNumber);
        senderPhoneNumber = formatPhoneNumber(senderPhoneNumber);

        User receiver = userRepository.findByPhone(receiverPhoneNumber);
        if (receiver == null) {
            throw new RuntimeException("Không tìm thấy người nhận: " + receiverPhoneNumber);
        }

        User sender = userRepository.findByPhone(senderPhoneNumber);
        if (sender == null) {
            throw new RuntimeException("Không tìm thấy người gửi: " + senderPhoneNumber);
        }
        
        if (!receiver.getPendings().contains(senderPhoneNumber)) {
            throw new RuntimeException("Không có lời mời kết bạn từ: " + senderPhoneNumber);
        }

        // Xóa lời mời trong Pending
        receiver.getPendings().remove(senderPhoneNumber);

        // Thêm bạn cho cả 2
        if (!receiver.getFriends().contains(senderPhoneNumber)) {
            receiver.getFriends().add(senderPhoneNumber);
        }

        if (!sender.getFriends().contains(receiverPhoneNumber)) {
            sender.getFriends().add(receiverPhoneNumber);
        }

        log.info("Đã thêm bạn thành công: {} và {}", senderPhoneNumber, receiverPhoneNumber);
        
        userRepository.save(receiver);
        userRepository.save(sender);
    }

    private String formatPhoneNumber(String phone) {
        phone = phone.trim().replaceAll("\\s+", "");
        if (!phone.startsWith("+")) {
            phone = "+" + phone;
        }
        return phone;
    }
    //Người gửi huỷ lời mời kết bạn
    @Override
    public void cancelFriendRequest(String senderPhoneNumber, String receiverPhoneNumber) {

        // Chuẩn hóa sđt
        senderPhoneNumber = formatPhoneNumber(senderPhoneNumber);
        receiverPhoneNumber = formatPhoneNumber(receiverPhoneNumber);

        User receiver = userRepository.findByPhone(receiverPhoneNumber);
        if (receiver == null) {
            throw new RuntimeException("Không tìm thấy người nhận: " + receiverPhoneNumber);
        }

        if (receiver.getPendings().contains(senderPhoneNumber)) {
            receiver.getPendings().remove(senderPhoneNumber);
            userRepository.save(receiver);
        } else {
            throw new RuntimeException("Không có lời mời kết bạn từ: " + senderPhoneNumber);
        }
    }
    
    //Từ chối lời mời kết bạn
    
    @Override
    public void rejectFriendRequest(String receiverPhoneNumber, String senderPhoneNumber) {

        receiverPhoneNumber = formatPhoneNumber(receiverPhoneNumber);
        senderPhoneNumber = formatPhoneNumber(senderPhoneNumber);

        User receiver = userRepository.findByPhone(receiverPhoneNumber);
        if (receiver == null) {
            throw new RuntimeException("Không tìm thấy người nhận: " + receiverPhoneNumber);
        }

        if (receiver.getPendings().contains(senderPhoneNumber)) {
            receiver.getPendings().remove(senderPhoneNumber);
            userRepository.save(receiver);
        } else {
            throw new RuntimeException("Không có lời mời kết bạn từ: " + senderPhoneNumber);
        }
    }


    @Override
    public void removeFriend(String userPhoneNumber, String friendPhoneNumber) {
    	// Chuẩn hóa số điện thoại
    	userPhoneNumber = formatPhoneNumber(userPhoneNumber);
    	friendPhoneNumber = formatPhoneNumber(friendPhoneNumber);
    	
        User user = userRepository.findByPhone(userPhoneNumber);
        if (user == null) {
            throw new RuntimeException("Không tìm thấy user: " + userPhoneNumber);
        }

        User friend = userRepository.findByPhone(friendPhoneNumber);
        if (friend == null) {
            throw new RuntimeException("Không tìm thấy bạn: " + friendPhoneNumber);
        }

        // Xoá bạn khỏi list bạn bè
        user.getFriends().remove(friendPhoneNumber);
        friend.getFriends().remove(userPhoneNumber);

        userRepository.save(user);
        userRepository.save(friend);
    }

	@Override
	public List<UserResponseDto> getFriendRequests(String phone) {
		// TODO Auto-generated method stub
		phone = formatPhoneNumber(phone);
		User user = userRepository.findByPhone(phone);
		if (user != null) {
			List<UserResponseDto> friendRequests = user.getPendings().stream()
					.map(friendPhone -> UserMapper.INSTANCE.toUserResponseDto(
							userRepository.findByPhone(friendPhone)))
					.toList();
			return friendRequests;
		} else {
			throw new RuntimeException("User not found");
		}
	}

	@Override
	public boolean isRequestPending(String userPhoneNumber,
			String friendPhoneNumber) {
		// TODO Auto-generated method stub
		userPhoneNumber = formatPhoneNumber(userPhoneNumber);
		friendPhoneNumber = formatPhoneNumber(friendPhoneNumber);
		User user = userRepository.findByPhone(friendPhoneNumber);
		if (user == null) {
			throw new RuntimeException("Không tìm thấy user: " + userPhoneNumber);
		}
		
		return user.getPendings().contains(userPhoneNumber);
	}
	
	@Override
	public boolean isFriend(String userPhoneNumber, String friendPhoneNumber) {
		// TODO Auto-generated method stub
		userPhoneNumber = formatPhoneNumber(userPhoneNumber);
		friendPhoneNumber = formatPhoneNumber(friendPhoneNumber);
		User user = userRepository.findByPhone(userPhoneNumber);
		User friend = userRepository.findByPhone(friendPhoneNumber);
		if (user == null) {
			throw new RuntimeException("Không tìm thấy user: " + userPhoneNumber);
		}
		if (friend == null) {
			throw new RuntimeException("Không tìm thấy bạn: " + friendPhoneNumber);
		}
		
		return user.getFriends().contains(friendPhoneNumber) && friend.getFriends().contains(userPhoneNumber);
	}

}
