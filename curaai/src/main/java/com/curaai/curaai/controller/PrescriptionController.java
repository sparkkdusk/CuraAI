package com.curaai.curaai.controller;

import com.curaai.curaai.dto.PrescriptionDetailDto;
import com.curaai.curaai.dto.PrescriptionSummaryDto;
import com.curaai.curaai.dto.SavePrescriptionRequest;
import com.curaai.curaai.dto.TodayItemDto;
import com.curaai.curaai.model.AnalysisResult;
import com.curaai.curaai.model.User;
import com.curaai.curaai.repository.UserRepository;
import com.curaai.curaai.service.PrescriptionAnalysisService;
import com.curaai.curaai.service.PrescriptionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Controller
@RequestMapping("/prescription")
public class PrescriptionController {

    private final PrescriptionAnalysisService analysisService;
    private final PrescriptionService prescriptionService;
    private final UserRepository userRepository;

    public PrescriptionController(PrescriptionAnalysisService analysisService,
                                   PrescriptionService prescriptionService,
                                   UserRepository userRepository) {
        this.analysisService = analysisService;
        this.prescriptionService = prescriptionService;
        this.userRepository = userRepository;
    }

    /** Renders the prescription analyzer + tracker page. */
    @GetMapping
    public String page() {
        return "prescription";
    }

    /**
     * Accepts an uploaded prescription image, sends it to Gemini's vision API
     * to OCR + extract structured medication data, and returns JSON for the
     * front-end to render. Nothing is persisted here - the user reviews the
     * result first and saves it explicitly via /prescription/save.
     */
    @PostMapping("/analyze")
    @ResponseBody
    public AnalysisResult analyze(@RequestParam("image") MultipartFile image) {
        try {
            byte[] bytes = image.getBytes();
            String contentType = image.getContentType() != null
                    ? image.getContentType() : "image/jpeg";

            PrescriptionAnalysisService.Result result = analysisService.analyze(bytes, contentType);

            return new AnalysisResult(true, result.rawText(), result.medications(), null);
        } catch (Exception e) {
            return new AnalysisResult(false, null, List.of(), e.getMessage());
        }
    }

    /** Persists a reviewed analysis result against the logged-in user. */
    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody SavePrescriptionRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        Long id = prescriptionService.save(user, request.label(), request.rawText(), request.medications());
        return Map.of("success", true, "id", id);
    }

    /** List of this user's past prescriptions, most recent first. */
    @GetMapping("/history")
    @ResponseBody
    public List<PrescriptionSummaryDto> history(Authentication authentication) {
        return prescriptionService.history(currentUser(authentication));
    }

    /** Full detail (raw text + medications) for one saved prescription. */
    @GetMapping("/history/{id}")
    @ResponseBody
    public PrescriptionDetailDto historyDetail(@PathVariable Long id, Authentication authentication) {
        return prescriptionService.detail(id, currentUser(authentication));
    }

    /** Today's medication checklist across all active prescriptions for this user. */
    @GetMapping("/today")
    @ResponseBody
    public List<TodayItemDto> today(Authentication authentication) {
        return prescriptionService.today(currentUser(authentication));
    }

    /** Marks (or unmarks) today's dose for a medication as taken. */
    @PostMapping("/dose/{medicationId}")
    @ResponseBody
    public TodayItemDto markDose(@PathVariable Long medicationId,
                                  @RequestBody Map<String, Boolean> body,
                                  Authentication authentication) {
        boolean taken = body.getOrDefault("taken", true);
        return prescriptionService.markDose(medicationId, currentUser(authentication), taken);
    }

    /** Marks a medication's course as finished so it drops off the daily tracker. */
    @PostMapping("/medication/{medicationId}/deactivate")
    @ResponseBody
    public Map<String, Object> deactivate(@PathVariable Long medicationId, Authentication authentication) {
        prescriptionService.deactivateMedication(medicationId, currentUser(authentication));
        return Map.of("success", true);
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseBody
    public Map<String, Object> handleNotFound(NoSuchElementException e) {
        return Map.of("success", false, "error", e.getMessage());
    }

    /**
     * Looks up the logged-in User by the Authentication principal name.
     * Assumes Spring Security's principal name is the user's email (the
     * standard setup when UserDetailsService loads users by email) and that
     * UserRepository exposes findByEmail(String). Adjust if your login flow
     * differs.
     */
    private User currentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }
}
