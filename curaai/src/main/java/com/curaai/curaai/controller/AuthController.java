package com.curaai.curaai.controller;



import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {


    @GetMapping("/auth/login")
    public String login(){
        return "login";
    }


    @PostMapping("/auth/login")
    public String loginUser(){

        // later you can add database authentication

        return "redirect:/dashboard";
    }


    @GetMapping("/dashboard")
    public String dashboard(){

        return "dashboard";

    }


    @GetMapping("/auth/register")
    public String register(){
        return "register";
    }



}