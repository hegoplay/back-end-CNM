package iuh.fit.se.client;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class StringeeCallAction {
	
	@Value("${stringee.keySid}")
	private String keySid;
	
	
	@Value("${stringee.keySecret}")
	private String keySecret;
	
	@Value("${stringee.originalPhoneNumber}")
	private String originalPhoneNumber;
	
	public void sendOTPAction(String phoneNumber, String OTP) {
		
        String url = "https://api.stringee.com/v1/call2/callout";
        
        String formattedNumber = formatPhoneNumber(phoneNumber);
        
        System.out.println("Số điện thoại đã được định dạng: " + formattedNumber);
        
        String formattedOTP = formatOTP(OTP);
        
        // Tạo client
        StringeeHttpClient client = new StringeeHttpClient(keySid, keySecret, 3600);
        
        // Tạo dữ liệu call
        Map<String, Object> callData = new HashMap<>();
        
        // From object
        Map<String, String> from = new HashMap<>();
        from.put("type", "external");
        from.put("number", originalPhoneNumber);
        from.put("alias", originalPhoneNumber);
        
        // To array
        Map<String, String> toItem = new HashMap<>();
        toItem.put("type", "external");
        toItem.put("number", formattedNumber);
        toItem.put("alias", formattedNumber);
        
        // Actions array
        Map<String, Object> talkAction = new HashMap<>();
        talkAction.put("action", "talk");
        talkAction.put("text", "Chào bạn, mã OTP của bạn là " + formattedOTP);
        log.info("Mã OTP đã được định dạng: {}", formattedOTP);
        talkAction.put("loop", 4);
        talkAction.put("speed", -1);
        
        // Thêm vào callData
        callData.put("from", from);
        callData.put("to", new Map[]{toItem});
//        callData.put("answer_url", "https://developer.stringee.com/scco_helper/simple_project_answer_url?record=false&appToPhone=auto&recordFormat=mp3");
        callData.put("actions", new Map[]{talkAction});
        
        try {
            // Gọi API
            StringeeHttpResponse response = client.post(url, callData, 15);
            
            // In kết quả
            System.out.println("Status Code: " + response.getStatusCode());
            System.out.println("Response: " + response.getContent());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	
	private String formatPhoneNumber(String phoneNumber) {
		
		String formattedPhoneNumber = phoneNumber;
		
		if (formattedPhoneNumber.startsWith("+")) {
			formattedPhoneNumber = formattedPhoneNumber.substring(1);
		}
		
		else if (phoneNumber.startsWith("0")) {
			formattedPhoneNumber = phoneNumber.substring(1);
			formattedPhoneNumber = "84" + phoneNumber;
		}
		
		return formattedPhoneNumber;
	}
	
	private String formatOTP(String otp) {
		String newOTP = "";
		
		String[] split = otp.split("");
		
		for (int i = 0; i < split.length; i++) {
			
			newOTP += " " + split[i] + ".";
			
		}
		return newOTP;
	}
	
}
