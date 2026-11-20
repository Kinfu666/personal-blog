package com.example.personalblog.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.personalblog.service.DashboardService;

@Controller
public class DashboardController {
    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("stats", dashboard.stats());
        return "admin/dashboard";
    }
}
