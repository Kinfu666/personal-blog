package com.example.personalblog.config;

import com.example.personalblog.service.SeedService;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class SeedInitializer implements ApplicationRunner {
    private final SeedService seed;

    public SeedInitializer(SeedService seed) {
        this.seed = seed;
    }

    public void run(ApplicationArguments args) throws Exception {
        seed.initialize();
    }
}
