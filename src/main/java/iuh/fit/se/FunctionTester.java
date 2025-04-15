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
        System.out.println("Tested function");
    }
}
