package com.example.personalblog.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.personalblog.dto.ImageContent;
import com.example.personalblog.exception.RuleViolation;
import com.example.personalblog.service.ImageService;

@Controller
public class ImageController {
    private final ImageService images;

    public ImageController(ImageService images) {
        this.images = images;
    }

    @GetMapping("/admin/images")
    public String list(Model model) {
        model.addAttribute("images", images.list());
        return "admin/images";
    }

    @PostMapping("/admin/images/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            images.delete(id);
            redirect.addFlashAttribute("message", "圖片已刪除");
        } catch (RuleViolation error) {
            redirect.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/admin/images";
    }

    @GetMapping("/uploads/{storedName}")
    @ResponseBody
    public ResponseEntity<byte[]> read(@PathVariable String storedName, Authentication auth) {
        boolean admin =
                auth != null
                        && auth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        ImageContent content = images.read(storedName, admin);
        // 同一路徑的公開權限會隨發布/撤回改變，不讓共享快取繼續提供已撤回圖片。
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.mediaType()))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(content.bytes());
    }
}
