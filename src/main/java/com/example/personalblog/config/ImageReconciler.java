package com.example.personalblog.config;

import com.example.personalblog.service.ImageService;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class ImageReconciler implements ApplicationRunner {
    private final ImageService images;

    public ImageReconciler(ImageService images) {
        this.images = images;
    }

    public void run(ApplicationArguments args) throws Exception {
        images.reconcile();
    }
}
