package com.curaai.curaai.service;

import com.curaai.curaai.dto.NutritionDaySummaryDto;
import com.curaai.curaai.dto.NutritionLogDto;
import com.curaai.curaai.model.NutritionLog;
import com.curaai.curaai.model.User;
import com.curaai.curaai.repository.NutritionLogRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NutritionService {

    @Value("${groq.api.key}")
    private String apiKey;

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    // TODO: once a per-user calorie target exists (e.g. a field on User), read it from there instead.
    private static final int DEFAULT_DAILY_CALORIE_GOAL = 2000;

    private final NutritionLogRepository repository;

    public NutritionService(NutritionLogRepository repository) {
        this.repository = repository;
    }

    public String analyzeFood(MultipartFile image) {

        try {
            // Convert image to base64
            String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
            String mimeType = image.getContentType(); // e.g. "image/jpeg"

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // IMAGE content part
            Map<String, Object> imageUrl = new HashMap<>();
            imageUrl.put("url", "data:" + mimeType + ";base64," + base64Image);

            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("type", "image_url");
            imagePart.put("image_url", imageUrl);

            // TEXT content part
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text", """
    You are a professional nutritionist and food analyst.
    
    Carefully analyze this food image and respond ONLY in this exact format, no extra text:
    
    FOOD_NAME: [specific food name]
    CALORIES: [number only, kcal]
    PROTEIN: [number only, grams]
    CARBS: [number only, grams]
    FAT: [number only, grams]
    FIBER: [number only, grams]
    SUGAR: [number only, grams]
    HEALTH_SCORE: [1-10 score]
    MEAL_TYPE: [Breakfast/Lunch/Dinner/Snack]
    INSIGHT: [2-3 sentence expert nutritionist insight about this food, health benefits, warnings, and tips]
    """);

            // Combine parts into message content
            List<Object> contentParts = new ArrayList<>();
            contentParts.add(imagePart);
            contentParts.add(textPart);

            // User message
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", contentParts);

            // Request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "meta-llama/llama-4-scout-17b-16e-instruct"); // free vision model
            requestBody.put("max_tokens", 1024);
            requestBody.put("messages", List.of(userMessage));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    API_URL,
                    request,
                    String.class
            );

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());

            // Parse response: choices[0].message.content
            String result = root
                    .get("choices")
                    .get(0)
                    .get("message")
                    .get("content")
                    .asText();

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Parses the raw "FOOD_NAME: ... CALORIES: ..." text the model returns and
     * persists it as a NutritionLog for the day. Skips saving if the analysis failed.
     */
    @Transactional
    public NutritionLog logMeal(User user, String rawResult, LocalDate date) {
        if (rawResult == null || rawResult.trim().toUpperCase(Locale.ROOT).startsWith("ERROR")) {
            return null;
        }

        NutritionLog log = new NutritionLog();
        log.setUser(user);
        log.setDate(date != null ? date : LocalDate.now());
        log.setFoodName(extract(rawResult, "FOOD_NAME"));
        log.setMealType(extract(rawResult, "MEAL_TYPE"));
        log.setCalories(parseIntSafe(extract(rawResult, "CALORIES")));
        log.setProtein(parseIntOrNull(extract(rawResult, "PROTEIN")));
        log.setCarbs(parseIntOrNull(extract(rawResult, "CARBS")));
        log.setFat(parseIntOrNull(extract(rawResult, "FAT")));
        log.setFiber(parseIntOrNull(extract(rawResult, "FIBER")));
        log.setSugar(parseIntOrNull(extract(rawResult, "SUGAR")));
        log.setHealthScore(parseIntOrNull(extract(rawResult, "HEALTH_SCORE")));
        log.setInsight(extract(rawResult, "INSIGHT"));

        return repository.save(log);
    }

    /** Today and the 6 days before it, oldest first, each with its logs and calorie total. */
    @Transactional(readOnly = true)
    public List<NutritionDaySummaryDto> weeklySummary(User user) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);

        List<NutritionLog> logs = repository.findByUserAndDateBetweenOrderByDateAsc(user, weekStart, today);

        Map<LocalDate, List<NutritionLog>> byDate = logs.stream()
                .collect(Collectors.groupingBy(NutritionLog::getDate));

        List<NutritionDaySummaryDto> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            List<NutritionLog> dayLogs = byDate.getOrDefault(date, List.of());

            int totalCalories = dayLogs.stream().mapToInt(NutritionLog::getCalories).sum();

            List<NutritionLogDto> logDtos = dayLogs.stream()
                    .map(l -> new NutritionLogDto(l.getId(), l.getFoodName(), l.getMealType(), l.getCalories(),
                            l.getProtein(), l.getCarbs(), l.getFat(), l.getFiber(), l.getSugar(),
                            l.getHealthScore(), l.getInsight()))
                    .toList();

            String dayLabel = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            result.add(new NutritionDaySummaryDto(date, dayLabel, totalCalories, logDtos));
        }

        return result;
    }

    public int weeklyAverageCalories(List<NutritionDaySummaryDto> week) {
        if (week.isEmpty()) return 0;
        int total = week.stream().mapToInt(NutritionDaySummaryDto::totalCalories).sum();
        return total / week.size();
    }

    public int getDailyCalorieGoal(User user) {
        return DEFAULT_DAILY_CALORIE_GOAL;
    }

    private String extract(String text, String key) {
        Matcher m = Pattern.compile(key + ":\\s*(.+)", Pattern.CASE_INSENSITIVE).matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private int parseIntSafe(String s) {
        Integer v = parseIntOrNull(s);
        return v != null ? v : 0;
    }

    private Integer parseIntOrNull(String s) {
        if (s == null) return null;
        Matcher m = Pattern.compile("-?\\d+").matcher(s);
        return m.find() ? Integer.parseInt(m.group()) : null;
    }
}
