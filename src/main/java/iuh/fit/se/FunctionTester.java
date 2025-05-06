package iuh.fit.se;


import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import iuh.fit.se.client.StringeeCallAction;
import iuh.fit.se.client.StringeeHttpClient;
import iuh.fit.se.client.StringeeHttpResponse;
import iuh.fit.se.serviceImpl.FriendServiceAWSImpl;
@Component
public class FunctionTester implements CommandLineRunner {

    @Autowired
    private FriendServiceAWSImpl friendServiceAWS;
    
    @Autowired
    private StringeeCallAction stringeeCallAction;

    @Override
    public void run(String... args) {
    	String keySid = "SK.0.KdiEs5F7R3RIIMTsQSINTK1OIdGrUS1s";
        String keySecret = "WTNMc2RUZ2N1REdtbUFtMUZKVnRheUk0TDVhdGZNOWo=";
        String url = "https://api.stringee.com/v1/call2/callout";
        
//        stringeeCallAction.sendOTPAction("84896939481", "123456");
        
//        String token = new StringeeHttpClient(keySid, keySecret, 3600).generateXStringeeAuthHeader();
//        System.out.println("Token: " + token);
        // Tạo client
//        StringeeHttpClient client = new StringeeHttpClient(keySid, keySecret, 3600);
//        
//        // Tạo dữ liệu call
//        Map<String, Object> callData = new HashMap<>();
//        
//        // From object
//        Map<String, String> from = new HashMap<>();
//        from.put("type", "external");
//        from.put("number", "842871010786");
//        from.put("alias", "842871010786");
//        
//        // To array
//        Map<String, String> toItem = new HashMap<>();
//        toItem.put("type", "external");
//        toItem.put("number", "84896939481");
//        toItem.put("alias", "84896939481");
//        
//        // Actions array
//        Map<String, Object> talkAction = new HashMap<>();
//        talkAction.put("action", "talk");
//        talkAction.put("text", "Chào mừng bạn đã đến với Stringee");
//        talkAction.put("loop", 2);
//        
//        // Thêm vào callData
//        callData.put("from", from);
//        callData.put("to", new Map[]{toItem});
//        callData.put("actions", new Map[]{talkAction});
        
//        try {
//            // Gọi API
//            StringeeHttpResponse response = client.post(url, callData, 15);
//            
//            // In kết quả
//            System.out.println("Status Code: " + response.getStatusCode());
//            System.out.println("Response: " + response.getContent());
//            
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
