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

import iuh.fit.se.model.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

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
        // Gửi OTP qua SNS
        
//        snsClient.publish(request);
        
        return otp; // Trả về OTP để lưu tạm (có thể dùng Redis hoặc in-memory)
    }
    
    public boolean verifyOtp(String phone, String otp) {
		String storedOtp = otpStore.get(phone);
		log.info("Xác thực OTP: {} cho số điện thoại: {}", otp, phone);
		// Kiểm tra OTP
		log.info("OTP đã lưu: {}", storedOtp);
		
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

//     Lưu thông tin vào DynamoDB
    public void saveToDynamoDB(RegisterRequest request, String avatarUrl) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("_id", AttributeValue.builder().s(UUID.randomUUID().toString()).build());
        item.put("phoneNumber", AttributeValue.builder().s(request.getPhone()).build());
        item.put("name", AttributeValue.builder().s(request.getName()).build());
        item.put("date_of_birth", AttributeValue.builder().s(request.getDateOfBirth()).build());
        item.put("gender", AttributeValue.builder().bool(request.isGender()).build());
        item.put("base_img", AttributeValue.builder().s(avatarUrl).build());
        item.put("password", AttributeValue.builder().s(passwordEncoder.encode(request.getPassword())).build());

        log.info("saveToDynamoDB: ",item);
        
        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName("users")
                .item(item)
                .build();

        dynamoDbClient.putItem(putItemRequest);
    }
}