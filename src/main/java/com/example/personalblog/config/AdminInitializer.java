package com.example.personalblog.config;

import com.example.personalblog.service.AdminService;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class AdminInitializer implements ApplicationRunner {
    private final AdminService admins;

    public AdminInitializer(AdminService admins) {
        this.admins = admins;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        admins.initialize();
    }
}
