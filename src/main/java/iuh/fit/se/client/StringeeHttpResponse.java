package iuh.fit.se.client;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class StringeeHttpResponse {
    private final int statusCode;
    private final String content;
    private final Map<String, String> headers;
    private final Gson gson = new Gson();
    
    public StringeeHttpResponse(int statusCode, String content, Map<String, String> headers) {
        this.statusCode = statusCode;
        this.content = content;
        this.headers = headers;
    }
    
    public String getContent() {
        return content;
    }
    
    public JsonObject getJsonContent() {
        return gson.fromJson(content, JsonObject.class);
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public boolean isOk() {
        return statusCode < 400;
    }
}