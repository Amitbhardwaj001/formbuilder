package com.yourform.formbuilder.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    public String generateSummary(String data) {

        RestTemplate restTemplate = new RestTemplate();

        // ✅ Use stable model (free + works)
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + apiKey;

        // ✅ FIX: create headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // ✅ Correct Gemini request body
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", "Summarize this survey data:\n" + data);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(textPart));

        Map<String, Object> request = new HashMap<>();
        request.put("contents", List.of(content));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(url, entity, Map.class);

            Map body = response.getBody();
            System.out.println("GEMINI RESPONSE = " + body);

            // ✅ Safe parsing
            List candidates = (List) body.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return "No response from Gemini";
            }

            Map first = (Map) candidates.get(0);
            Map contentMap = (Map) first.get("content");
            List parts = (List) contentMap.get("parts");
            Map textObj = (Map) parts.get(0);

            return textObj.get("text").toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Gemini Exception: " + e.getMessage();
        }
    }
}