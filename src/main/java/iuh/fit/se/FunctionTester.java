package iuh.fit.se;


import iuh.fit.se.model.dto.UserResponseDto;
import iuh.fit.se.serviceImpl.FriendServiceAWSImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
@Component
public class FunctionTester implements CommandLineRunner {

    @Autowired
    private FriendServiceAWSImpl friendServiceAWS;

    @Override
    public void run(String... args) {
        String phone = "+84376626025";
        String nameKeyword = "Who";
        List<UserResponseDto> results = friendServiceAWS.findFriendsByName(phone, nameKeyword);

        if (results.isEmpty()) {
            System.out.println("No user found with keyword: " + nameKeyword);
        } else {
            results.forEach(user -> {
                System.out.println("User found:\n" + user.toString());
            });
        }
    }
}
