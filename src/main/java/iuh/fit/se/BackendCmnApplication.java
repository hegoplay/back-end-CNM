package iuh.fit.se;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class BackendCmnApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendCmnApplication.class, args);
	}
}
