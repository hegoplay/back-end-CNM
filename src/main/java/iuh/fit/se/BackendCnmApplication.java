package iuh.fit.se;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
@EnableAspectJAutoProxy
public class BackendCnmApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendCnmApplication.class, args);
		// Đặt múi giờ mặc định là GMT+7 (Asia/Ho Chi Minh)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
	}
}
