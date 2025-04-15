package iuh.fit.se.service;
import iuh.fit.se.model.dto.UserResponseDto;

import java.util.List;

public interface FriendService {
    List<UserResponseDto> getFriendsList(String phone);

    List<UserResponseDto> findPersonByPhone(String phone);

    List<UserResponseDto> findFriendsByName(String userPhone, String nameKeyword);
    
//  Lấy danh ách lời mời kết bạn
    List<UserResponseDto> getFriendRequests(String phone);

    // Gửi lời mời kết bạn
    void sendFriendRequest(String senderPhoneNumber, String receiverPhoneNumber);

    // Chấp nhận lời mời kết bạn
    void acceptFriendRequest(String receiverPhoneNumber, String senderPhoneNumber);
    
    //Người gửi huỷ lời mời kết bạn
    void cancelFriendRequest(String senderPhoneNumber, String receiverPhoneNumber);
    
    //Từ chối lời mời kết bạn
    void rejectFriendRequest(String receiverPhoneNumber, String senderPhoneNumber);
    
    // Xóa bạn
    void removeFriend(String userPhoneNumber, String friendPhoneNumber);
    
    // Kiểm tra xem lời mời kết bạn
    boolean isRequestPending(String userPhoneNumber, String friendPhoneNumber);
    
    // Kiểm tra xem đã là bạn hay chưa
    boolean isFriend(String userPhoneNumber, String friendPhoneNumber);
    
    
}