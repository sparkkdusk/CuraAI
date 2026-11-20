package com.curaai.curaai.controller;

import com.curaai.curaai.model.User;
import com.curaai.curaai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String password,
            Model model) {

        if (userRepository.existsByEmail(email)) {
            model.addAttribute("error", "An account with that email already exists");
            return "register";
        }

        User user = new User(fullName, email, passwordEncoder.encode(password));
        userRepository.save(user);

        return "redirect:/auth/login?registered";
    }
}