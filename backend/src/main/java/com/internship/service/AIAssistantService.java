package com.internship.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIAssistantService {
    private static final Logger log = LoggerFactory.getLogger(AIAssistantService.class);

    @Value("${deepseek.api-key:}")
    private String apiKey;
    @Value("${deepseek.api-url:https://api.deepseek.com/v1}")
    private String apiUrl;
    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    private final RestTemplate rt = new RestTemplate();

    public String chat(String msg) {
        if (apiKey == null || apiKey.isBlank()) {
            return "DeepSeek API Key 未配置，请在后端环境变量 DEEPSEEK_API_KEY 中配置后重启服务。";
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(message("system", "你是实习存证平台AI助手。请用中文回答，结合实习备案、企业审核、过程填报、链上存证、证书核验等业务给出简洁可执行的建议。"));
            messages.add(message("user", msg == null ? "" : msg));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("temperature", 0.7);
            body.put("messages", messages);

            ResponseEntity<Map> response = rt.postForEntity(apiUrl + "/chat/completions", new HttpEntity<>(body, headers), Map.class);
            Object choicesObj = response.getBody() == null ? null : response.getBody().get("choices");
            if (choicesObj instanceof List<?> choices && !choices.isEmpty() && choices.get(0) instanceof Map<?, ?> choice) {
                Object messageObj = choice.get("message");
                if (messageObj instanceof Map<?, ?> responseMessage) {
                    Object content = responseMessage.get("content");
                    if (content != null) return content.toString();
                }
            }
            return "DeepSeek 返回为空，请稍后重试。";
        } catch (Exception e) {
            log.warn("AI assistant request failed: {}", e.getMessage());
            return "AI助手暂不可用：" + e.getMessage();
        }
    }

    public String generateCertificateContent(String sn, String sid, String en, String pos, String sd, String ed) {
        return chat(String.format("为实习生%s(%s)在%s的%s岗位(%s~%s)生成实习证明", sn, sid, en, pos, sd, ed));
    }

    public String analyzeDailyRecord(String content) {
        return chat("分析以下实习日记：" + content);
    }

    public String getInternshipGuidance(String question) {
        return chat("实习指导问题：" + question);
    }

    private Map<String, Object> message(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }
}
