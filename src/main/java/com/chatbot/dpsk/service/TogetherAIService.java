package com.chatbot.dpsk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TogetherAIService {

    private final HttpClient httpClient;
    private final String togetherApiKey;
    private final String togetherApiUrl;
    private final ObjectMapper objectMapper;

    public TogetherAIService(
            @Value("${together.api.key}") String togetherApiKey,
            @Value("${together.api.url}") String togetherApiUrl) throws Exception {
        this.togetherApiKey = togetherApiKey;
        this.togetherApiUrl = togetherApiUrl;
        this.httpClient = createHttpClientWithDisabledSSL();
        this.objectMapper = new ObjectMapper();
    }



    public String getAIResponse(String userInput) {
        try {
            // Build the request body as a Map
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "meta-llama/Llama-3.3-70B-Instruct-Turbo-Free");
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", "You are a general chatbot like chatGPT."),
                    Map.of("role", "user", "content", userInput)
            ));

            // Convert the request body to JSON
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);

            // Create the HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(togetherApiUrl))
                    .header("Authorization", "Bearer " + togetherApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson, StandardCharsets.UTF_8))
                    .build();

            // Send the request and handle the response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to get response from Together AI: " + response.statusCode() + " - " + response.body());
            }

            // Parse the response
            Map<?, ?> responseMap = objectMapper.readValue(response.body(), Map.class);
            List<?> choices = (List<?>) responseMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("No choices found in the response.");
            }

            Map<?, ?> message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
            String content = (String) message.get("content");

            // Format the content for better readability
            return content.replace("\\n", "\n").replace("\\t", "    ");

        } catch (Exception e) {
            return "Error contacting Together AI: " + e.getMessage();
        }
    }

    private HttpClient createHttpClientWithDisabledSSL() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());

        return HttpClient.newBuilder()
                .sslContext(sc)
                .build();
    }
}