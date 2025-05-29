package com.springboot.MyTodoList.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAIService {

    @Value("${openai.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String API_URL = "https://api.openai.com/v1/chat/completions";

    public Map<String, Object> analyzeSprintPerformance(Map<String, Object> sprintData) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o");
            requestBody.put("temperature", 0.5);
            requestBody.put("max_tokens", 1500);
            
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "Eres un experto en análisis de datos y gestión de proyectos ágiles. " +
                    "Tu tarea es analizar los datos de rendimiento del sprint proporcionado y generar un informe detallado " +
                    "con insights valiosos, patrones, áreas de mejora y recomendaciones específicas. " +
                    "Usa un formato de markdown con encabezados y listas para estructurar la información. " +
                    "Incluye métricas clave, comparativas y sugerencias accionables.");
            
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "Analiza el rendimiento de este sprint basado en los siguientes datos: " + 
                    objectMapper.writeValueAsString(sprintData));
            
            requestBody.put("messages", List.of(systemMessage, userMessage));
            
            return makeOpenAIRequest(requestBody);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error en el análisis del sprint: " + e.getMessage());
            return error;
        }
    }

    public Map<String, Object> analyzeTeamPerformance(Map<String, Object> teamData) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o");
            requestBody.put("temperature", 0.5);
            requestBody.put("max_tokens", 1500);
            
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "Eres un experto en análisis de rendimiento de equipos de desarrollo. " +
                    "Tu tarea es analizar los datos proporcionados sobre el equipo y generar un informe detallado " +
                    "que incluya fortalezas del equipo, áreas de mejora, patrones de rendimiento, y recomendaciones específicas " +
                    "para aumentar la productividad y eficiencia. Analiza también la distribución de trabajo y la colaboración. " +
                    "Usa un formato de markdown con encabezados y listas para estructurar la información.");
            
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "Analiza el rendimiento de este equipo basado en los siguientes datos: " + 
                    objectMapper.writeValueAsString(teamData));
            
            requestBody.put("messages", List.of(systemMessage, userMessage));
            
            return makeOpenAIRequest(requestBody);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error en el análisis del equipo: " + e.getMessage());
            return error;
        }
    }

    public Map<String, Object> analyzeDeveloperPerformance(Map<String, Object> developerData) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o");
            requestBody.put("temperature", 0.5);
            requestBody.put("max_tokens", 1500);
            
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "Eres un experto en coaching de desarrolladores de software. " +
                    "Tu tarea es analizar los datos de rendimiento del desarrollador proporcionado y generar un informe detallado " +
                    "con insights personalizados, fortalezas, áreas de mejora y recomendaciones específicas para su crecimiento. " +
                    "Compara su desempeño con las métricas del equipo cuando sea relevante. " +
                    "Usa un formato de markdown con encabezados y listas para estructurar la información.");
            
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "Analiza el rendimiento de este desarrollador basado en los siguientes datos: " + 
                    objectMapper.writeValueAsString(developerData));
            
            requestBody.put("messages", List.of(systemMessage, userMessage));
            
            return makeOpenAIRequest(requestBody);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error en el análisis del desarrollador: " + e.getMessage());
            return error;
        }
    }

    private Map<String, Object> makeOpenAIRequest(Map<String, Object> requestBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, entity, String.class);
            
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
            
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> choices = (Map<String, Object>) ((List<Object>) responseMap.get("choices")).get(0);
            Map<String, Object> message = (Map<String, Object>) choices.get("message");
            
            result.put("analysis", message.get("content"));
            result.put("usage", responseMap.get("usage"));
            
            return result;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error en la comunicación con OpenAI: " + e.getMessage());
            return error;
        }
    }
}