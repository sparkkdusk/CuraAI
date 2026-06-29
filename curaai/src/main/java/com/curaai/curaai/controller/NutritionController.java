package com.curaai.curaai.controller;

import com.curaai.curaai.service.NutritionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

@Controller
public class NutritionController {

    private final NutritionService service;

    public NutritionController(NutritionService service) {
        this.service = service;
    }

    @GetMapping("/nutrition")
    public String page() {
        return "nutrition";
    }

    @PostMapping("/nutrition/analyze")
    public String analyze(
            @RequestParam("image") MultipartFile image,
            Model model
    ) {
        try {
            String result = service.analyzeFood(image);
            model.addAttribute("result", result);

            String base64 = Base64.getEncoder().encodeToString(image.getBytes());
            String imageSrc = "data:" + image.getContentType() + ";base64," + base64;
            model.addAttribute("imageSrc", imageSrc);

        } catch (Exception e) {
            model.addAttribute("result", "Error analyzing image: " + e.getMessage());
        }

        return "nutrition";
    }
}