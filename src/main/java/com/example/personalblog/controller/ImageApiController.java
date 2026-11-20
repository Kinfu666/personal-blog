package com.example.personalblog.controller;

import com.example.personalblog.dto.ImageResponse;
import com.example.personalblog.dto.PreviewForm;
import com.example.personalblog.service.ImageService;
import com.example.personalblog.service.MarkdownService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Map;

@RestController
public class ImageApiController {
    private final ImageService images;
    private final MarkdownService markdown;

    public ImageApiController(ImageService images, MarkdownService markdown) {
        this.images = images;
        this.markdown = markdown;
    }

    @PostMapping("/admin/images")
    public ImageResponse upload(@RequestParam MultipartFile file, Principal principal) {
        return images.upload(file, principal.getName());
    }

    @PostMapping("/admin/markdown/preview")
    public Map<String, String> preview(@Valid @RequestBody PreviewForm form) {
        return Map.of("html", markdown.render(form.content()));
    }
}
