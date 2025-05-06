package iuh.fit.se.client;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


import com.google.gson.Gson;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class StringeeHttpClient {
    private final String keySid;
    private final String keySecret;
    private final int expiredAfter;
    private Map<String, String> options;
    
    private static final int DEFAULT_TIMEOUT = 60;
    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();
    
    public StringeeHttpClient(String keySid, String keySecret, int expiredAfter) {
        this.keySid = keySid;
        this.keySecret = keySecret;
        this.expiredAfter = expiredAfter;
        this.options = new HashMap<>();
        
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .build();
    }
    
    public void setOption(Map<String, String> options) {
        this.options = options;
    }
    
    public StringeeHttpResponse post(String url, Object data) throws IOException {
        return post(url, data, DEFAULT_TIMEOUT);
    }
    
    public StringeeHttpResponse post(String url, Object data, Integer timeout) throws IOException {
        String jsonData = data instanceof String ? (String) data : gson.toJson(data);
        return request(url, "POST", jsonData, new HashMap<>(), timeout);
    }
    
    public StringeeHttpResponse get(String url) throws IOException {
        return get(url, DEFAULT_TIMEOUT);
    }
    
    public StringeeHttpResponse get(String url, Integer timeout) throws IOException {
        return request(url, "GET", null, new HashMap<>(), timeout);
    }
    
    public String generateXStringeeAuthHeader() {
        long now = System.currentTimeMillis() / 1000;
        long exp = now + expiredAfter;
        
        Map<String, Object> header = new HashMap<>();
        header.put("cty", "stringee-api;v=1");
        
        return Jwts.builder()
                .header().add(header).and()
                .id(keySid + "-" + now)
                .issuer(keySid)
                .expiration(new Date(exp * 1000))
                .claim("rest_api", true)
                .signWith(Keys.hmacShaKeyFor(keySecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
    
    public StringeeHttpResponse request(String url, String method, String data, 
                                      Map<String, String> headers, Integer timeout) throws IOException {
        OkHttpClient client = timeout != null ? 
            httpClient.newBuilder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build() : 
            httpClient;
        
        Request.Builder requestBuilder = new Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .addHeader("X-STRINGEE-AUTH", generateXStringeeAuthHeader());
        
        // Add custom headers
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            requestBuilder.addHeader(entry.getKey(), entry.getValue());
        }
        
        // Set method and body
        if (method.equalsIgnoreCase("GET")) {
            requestBuilder.get();
        } else if (method.equalsIgnoreCase("POST")) {
            RequestBody body = RequestBody.create(data, MediaType.parse("application/json"));
            requestBuilder.post(body);
        } else if (method.equalsIgnoreCase("PUT")) {
            RequestBody body = RequestBody.create(data, MediaType.parse("application/json"));
            requestBuilder.put(body);
        } else if (method.equalsIgnoreCase("HEAD")) {
            requestBuilder.head();
        } else {
            RequestBody body = data != null ? 
                RequestBody.create(data, MediaType.parse("application/json")) : 
                null;
            requestBuilder.method(method.toUpperCase(), body);
        }
        
        Request request = requestBuilder.build();
        
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            Map<String, String> responseHeaders = new HashMap<>();
            for (String name : response.headers().names()) {
                responseHeaders.put(name, response.headers().get(name));
            }
            
            return new StringeeHttpResponse(response.code(), responseBody, responseHeaders);
        }
    }
}