package com.curaai.curaai.controller;

import com.curaai.curaai.dto.DaySummaryDto;
import com.curaai.curaai.dto.NutritionDaySummaryDto;
import com.curaai.curaai.model.User;
import com.curaai.curaai.repository.PrescriptionRepository;
import com.curaai.curaai.repository.UserRepository;
import com.curaai.curaai.service.ExerciseService;
import com.curaai.curaai.service.NutritionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final PrescriptionRepository prescriptionRepository;
    private final UserRepository userRepository;
    private final ExerciseService exerciseService;
    private final NutritionService nutritionService;

    public HomeController(PrescriptionRepository prescriptionRepository,
                          UserRepository userRepository,
                          ExerciseService exerciseService,
                          NutritionService nutritionService) {
        this.prescriptionRepository = prescriptionRepository;
        this.userRepository = userRepository;
        this.exerciseService = exerciseService;
        this.nutritionService = nutritionService;
    }

    @GetMapping("/")
    public String home() {
        return "index"; // renders templates/index.html
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        model.addAttribute("user", user);
        model.addAttribute("prescriptions", prescriptionRepository.findByUserOrderByUploadedAtDesc(user));

        // Exercise summary
        List<DaySummaryDto> week = exerciseService.weeklySummary(user);
        model.addAttribute("weeklyMinutes", exerciseService.weeklyTotalMinutes(week));
        model.addAttribute("weeklyCalories", exerciseService.weeklyTotalCalories(week));
        model.addAttribute("activeDays", exerciseService.activeDaysCount(week));

        // Nutrition summary
        List<NutritionDaySummaryDto> nutritionWeek = nutritionService.weeklySummary(user);
        int todayCalories = nutritionWeek.get(nutritionWeek.size() - 1).totalCalories();
        model.addAttribute("todayCalories", todayCalories);
        model.addAttribute("calorieGoal", nutritionService.getDailyCalorieGoal(user));

        return "dashboard"; // renders templates/dashboard.html
    }
}