package com.curaai.curaai.controller;

import com.curaai.curaai.dto.NutritionDaySummaryDto;
import com.curaai.curaai.model.User;
import com.curaai.curaai.repository.UserRepository;
import com.curaai.curaai.service.NutritionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

@Controller
public class NutritionController {

    private final NutritionService service;
    private final UserRepository userRepository;

    public NutritionController(NutritionService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @GetMapping("/nutrition")
    public String page() {
        return "nutrition";
    }

    @PostMapping("/nutrition/analyze")
    public String analyze(
            @RequestParam("image") MultipartFile image,
            Authentication authentication,
            Model model
    ) {
        try {
            String result = service.analyzeFood(image);
            model.addAttribute("result", result);

            String base64 = Base64.getEncoder().encodeToString(image.getBytes());
            String imageSrc = "data:" + image.getContentType() + ";base64," + base64;
            model.addAttribute("imageSrc", imageSrc);

            User user = currentUser(authentication);
            service.logMeal(user, result, LocalDate.now());

        } catch (Exception e) {
            model.addAttribute("result", "Error analyzing image: " + e.getMessage());
        }

        return "nutrition";
    }

    @GetMapping("/nutrition-activity")
    public String activity(Authentication authentication, Model model) {
        User user = currentUser(authentication);

        List<NutritionDaySummaryDto> week = service.weeklySummary(user);
        int todayCalories = week.get(week.size() - 1).totalCalories();
        int calorieGoal = service.getDailyCalorieGoal(user);

        model.addAttribute("week", week);
        model.addAttribute("todayCalories", todayCalories);
        model.addAttribute("calorieGoal", calorieGoal);
        model.addAttribute("remainingCalories", Math.max(calorieGoal - todayCalories, 0));
        model.addAttribute("weeklyAvgCalories", service.weeklyAverageCalories(week));

        return "nutrition-activity";
    }

    private User currentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }
}
