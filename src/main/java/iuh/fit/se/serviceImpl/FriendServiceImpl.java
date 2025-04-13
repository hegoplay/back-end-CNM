package iuh.fit.se.serviceImpl;


import iuh.fit.se.model.User;
import iuh.fit.se.repo.UserRepository;
import iuh.fit.se.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FriendServiceImpl implements FriendService {

    private final UserRepository userRepository;

    @Autowired
    public FriendServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
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
}
