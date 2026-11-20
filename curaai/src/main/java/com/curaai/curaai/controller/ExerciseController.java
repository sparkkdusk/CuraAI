package com.curaai.curaai.controller;

import com.curaai.curaai.dto.DaySummaryDto;
import com.curaai.curaai.model.User;
import com.curaai.curaai.repository.UserRepository;
import com.curaai.curaai.service.ExerciseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
    public Object logActivity(
            @RequestParam String activityType,
            @RequestParam int durationMinutes,
            @RequestParam(required = false) Integer caloriesBurned,
            @RequestParam(required = false) String notes,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
            Authentication authentication
    ) {
        User user = currentUser(authentication);
        exerciseService.logActivity(user, activityType, durationMinutes, caloriesBurned, notes, LocalDate.now());

        // fetch() from exercise.html gets a plain 200; a normal form post still redirects
        if ("XMLHttpRequest".equals(requestedWith)) {
            return ResponseEntity.ok("saved");
        }
        return "redirect:/exercise";
    }

    /** Backfill: index all existing exercise logs for RAG. Visit via a POST (e.g. curl/Postman with CSRF) once. */
    @PostMapping("/exercise/reindex")
    @ResponseBody
    public String reindex(Authentication authentication) {
        exerciseService.reindexAll(currentUser(authentication));
        return "reindexed";
    }

    private User currentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }
}