package com.example.personalblog.controller;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {
    private final Environment environment;

    public LoginController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute(
                "demoMode",
                environment.acceptsProfiles(Profiles.of("demo"))
                        && !environment.acceptsProfiles(Profiles.of("prod")));
        return "public/login";
    }
}
