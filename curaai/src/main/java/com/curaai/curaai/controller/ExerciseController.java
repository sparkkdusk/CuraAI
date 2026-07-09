package com.curaai.curaai.controller;

import com.curaai.curaai.dto.DaySummaryDto;
import com.curaai.curaai.model.User;
import com.curaai.curaai.repository.UserRepository;
import com.curaai.curaai.service.ExerciseService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class ExerciseController {

    private final ExerciseService exerciseService;
    private final UserRepository userRepository;

    public ExerciseController(ExerciseService exerciseService, UserRepository userRepository) {
        this.exerciseService = exerciseService;
        this.userRepository = userRepository;
    }

    @GetMapping("/exercise")
    public String exercise(Authentication authentication, Model model) {
        User user = currentUser(authentication);

        List<DaySummaryDto> week = exerciseService.weeklySummary(user);

        model.addAttribute("week", week);
        model.addAttribute("weeklyMinutes", exerciseService.weeklyTotalMinutes(week));
        model.addAttribute("weeklyCalories", exerciseService.weeklyTotalCalories(week));
        model.addAttribute("activeDays", exerciseService.activeDaysCount(week));

        return "exercise";
    }

    @GetMapping("/activity")
    public String activity(Authentication authentication, Model model) {
        User user = currentUser(authentication);

        List<DaySummaryDto> week = exerciseService.weeklySummary(user);

        model.addAttribute("week", week);
        model.addAttribute("weeklyMinutes", exerciseService.weeklyTotalMinutes(week));
        model.addAttribute("weeklyCalories", exerciseService.weeklyTotalCalories(week));
        model.addAttribute("activeDays", exerciseService.activeDaysCount(week));

        return "activity";
    }

    @PostMapping("/exercise/log")
    public String logActivity(
            @RequestParam String activityType,
            @RequestParam int durationMinutes,
            @RequestParam(required = false) Integer caloriesBurned,
            @RequestParam(required = false) String notes,
            Authentication authentication
    ) {
        User user = currentUser(authentication);
        exerciseService.logActivity(user, activityType, durationMinutes, caloriesBurned, notes, LocalDate.now());
        return "redirect:/exercise";
    }

    private User currentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }
}
