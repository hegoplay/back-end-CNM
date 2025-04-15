package iuh.fit.se;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NgrokService {

    @Value("${app.usingDynamicPort:true}")
    private boolean usingDynamicPort;

    private final ServerProperties serverProperties;
    private Process ngrokProcess;
    private static final String MOCK_API_URL = "https://67f9e5cd094de2fe6ea29dfd.mockapi.io/API_URL";

    public NgrokService(ServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationReady() {
        if (!usingDynamicPort) {
            log.info("Dynamic port is disabled. Skipping ngrok and mock API operations.");
            return;
        }

        try {
            int port = serverProperties.getPort() != null ? serverProperties.getPort() : 8080;
            String publicUrl = createPublicApi(port);
            log.info("Public API URL: {}", publicUrl);

            clearMockApi();
            postUrlToApi(publicUrl);
        } catch (Exception e) {
            log.error("Error creating public API: {}", e.getMessage());
        }
    }

    public String createPublicApi(int port) throws Exception {
        String ngrokPath = "C:\\Users\\luong\\AppData\\Roaming\\npm\\ngrok.cmd";
        ProcessBuilder processBuilder = new ProcessBuilder(ngrokPath, "http", String.valueOf(port));
        processBuilder.redirectErrorStream(true);
        ngrokProcess = processBuilder.start();

        Thread.sleep(2000);

        String publicUrl = null;
        try {
            URL url = new URL("http://localhost:4040/api/tunnels");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String json = response.toString();
                int urlIndex = json.indexOf("\"public_url\":\"") + 14;
                int urlEnd = json.indexOf("\"", urlIndex);
                publicUrl = json.substring(urlIndex, urlEnd);

                if (publicUrl.startsWith("http://")) {
                    int httpsIndex = json.indexOf("\"public_url\":\"https://", urlIndex);
                    if (httpsIndex != -1) {
                        httpsIndex += 14;
                        int httpsEnd = json.indexOf("\"", httpsIndex);
                        publicUrl = json.substring(httpsIndex, httpsEnd);
                    }
                }
            }
            conn.disconnect();
        } catch (Exception e) {
            if (ngrokProcess != null) {
                ngrokProcess.destroy();
            }
            throw new Exception("Failed to retrieve ngrok public URL: " + e.getMessage());
        }

        if (publicUrl == null || publicUrl.isEmpty()) {
            if (ngrokProcess != null) {
                ngrokProcess.destroy();
            }
            throw new Exception("Could not retrieve ngrok public URL.");
        }

        return publicUrl;
    }

    private void clearMockApi() {
        try {
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<List> response = restTemplate.exchange(
                    MOCK_API_URL,
                    HttpMethod.GET,
                    null,
                    List.class
            );

            List<Map<String, Object>> entries = response.getBody();
            if (entries == null || entries.isEmpty()) {
                log.info("No existing entries to delete in the mock API.");
                return;
            }

            for (Map<String, Object> entry : entries) {
                String id = entry.get("id").toString();
                String deleteUrl = MOCK_API_URL + "/" + id;

                restTemplate.delete(deleteUrl);
                log.info("Deleted entry with ID: {}", id);
            }

            log.info("Successfully cleared all data from the mock API.");
        } catch (Exception e) {
            log.error("Failed to clear mock API data: {}", e.getMessage());
        }
    }

    private void postUrlToApi(String publicUrl) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String jsonPayload = "{\"API_URL\": \"" + publicUrl + "\"}";
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            restTemplate.postForObject(MOCK_API_URL, request, String.class);
            log.info("Successfully posted URL {} to API", publicUrl);
        } catch (Exception e) {
            log.error("Failed to post URL {} to API: {}", publicUrl, e.getMessage());
        }
    }
}
