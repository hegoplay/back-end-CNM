package iuh.fit.se.service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import iuh.fit.se.model.dto.auth.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class AwsService {

    @Value("${aws.region}")
    private String region;

    private final SnsClient snsClient;
    private final S3Client s3Client;
    private final DynamoDbClient dynamoDbClient;
    private final PasswordEncoder passwordEncoder;

    private Map<String, String> otpStore = new HashMap<>(); // Lưu OTP tạm thời
    
    // Tạo và gửi OTP qua SNS
    public String sendOtp(String phone) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        String message = "Mã OTP của bạn là: " + otp;
        
        log.info("Gửi OTP: {} đến số điện thoại: {}", otp, phone);
        
        PublishRequest request = PublishRequest.builder()
                .phoneNumber(phone)
                .message(message)
                .build();

        
        // Lưu OTP vào bộ nhớ tạm thời
        otpStore.put(phone, otp);
        String storedOtp = otpStore.get(phone);
        log.info("OTP đã lưu: {}", storedOtp);
        // Gửi OTP qua SNS
        
        try{
//        	PublishResponse publish = snsClient.publish(request);
//        	log.info("Gửi OTP thành công, messageId: {}", publish);
        }
        catch (Exception e) {
			log.error("Gửi OTP thất bại: {}", e.getMessage());
		}
        
        
        return otp; // Trả về OTP để lưu tạm (có thể dùng Redis hoặc in-memory)
    }
    
    public boolean verifyOtp(String phone, String otp) {
		String storedOtp = otpStore.get(phone);
		log.info("Xác thực OTP: {} cho số điện thoại: {}", otp, phone);
		// Kiểm tra OTP
		log.info("OTP đã lưu: {}", otpStore);
		
		if (storedOtp != null && storedOtp.equals(otp)) {
			otpStore.remove(phone); // Xóa OTP sau khi xác thực
			return true;
		}
		return false;
	}

    // Upload ảnh lên S3
    public String uploadToS3(MultipartFile file) throws Exception {
    	log.info("Upload file: {}", file.getOriginalFilename());
        String bucketName = "zalas3bucket";
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + fileName);
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        s3Client.putObject(putObjectRequest, convFile.toPath());
        convFile.delete();

        return "https://dc7q18mlu9m5b.cloudfront.net/" + fileName;
    }
}