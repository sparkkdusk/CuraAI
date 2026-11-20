package com.curaai.curaai.controller;

import com.curaai.curaai.model.User;
import com.curaai.curaai.repository.UserRepository;
import com.curaai.curaai.service.ExerciseService;
import com.curaai.curaai.service.NutritionService;
import com.curaai.curaai.service.PrescriptionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * One-off backfill endpoint: embeds and stores every existing nutrition,
 * exercise, and prescription record for the logged-in user, so chat's RAG
 * search can find history that existed before this feature was added.
 *
 * New records are indexed automatically going forward (see each service's
 * logging/save methods) - you only need to call this once per user after
 * deploying this feature, or again if you ever wipe the document_chunks
 * table. Call it with:
 *
 *   Invoke-RestMethod -Uri "http://localhost:8080/api/rag/reindex" -Method Post -WebSession $session
 *
 * (using whatever session/cookie your browser already has from being
 * logged in - or just visit it via a quick fetch() in devtools console:
 * fetch('/api/rag/reindex', { method: 'POST' }) while logged in on the site).
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final UserRepository userRepository;
    private final NutritionService nutritionService;
    private final ExerciseService exerciseService;
    private final PrescriptionService prescriptionService;

    public RagController(UserRepository userRepository,
                          NutritionService nutritionService,
                          ExerciseService exerciseService,
                          PrescriptionService prescriptionService) {
        this.userRepository = userRepository;
        this.nutritionService = nutritionService;
        this.exerciseService = exerciseService;
        this.prescriptionService = prescriptionService;
    }

    @PostMapping("/reindex")
    @ResponseBody
    public Map<String, String> reindex(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        nutritionService.reindexAll(user);
        exerciseService.reindexAll(user);
        prescriptionService.reindexAll(user);

        return Map.of("status", "reindexed");
    }
}
