package com.example.personalblog.controller;

import com.example.personalblog.dto.PostForm;
import com.example.personalblog.enums.PostStatus;
import com.example.personalblog.exception.RuleViolation;
import com.example.personalblog.service.PostService;
import com.example.personalblog.service.TaxonomyService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/posts")
public class AdminPostController {
    private final PostService posts;
    private final TaxonomyService taxonomy;

    public AdminPostController(PostService posts, TaxonomyService taxonomy) {
        this.posts = posts;
        this.taxonomy = taxonomy;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) PostStatus status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("posts", posts.list(false, q, null, null, status, page));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        return "admin/posts";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("postForm", new PostForm());
        return form(null, model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("postForm", posts.edit(id));
        return form(id, model);
    }

    @PostMapping({"", "/{id}"})
    public String save(
            @PathVariable(required = false) Long id,
            @Valid @ModelAttribute PostForm postForm,
            BindingResult errors,
            Model model,
            Principal principal,
            RedirectAttributes redirect) {
        if (!errors.hasErrors())
            try {
                posts.save(id, postForm, principal.getName());
                redirect.addFlashAttribute("message", "文章已儲存");
                return "redirect:/admin/posts";
            } catch (RuleViolation error) {
                errors.rejectValue(error.getField(), "rule", error.getMessage());
            }
        return form(id, model);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        posts.delete(id);
        redirect.addFlashAttribute("message", "文章已刪除");
        return "redirect:/admin/posts";
    }

    private String form(Long id, Model model) {
        model.addAttribute("postId", id);
        model.addAttribute("categories", taxonomy.list(false));
        model.addAttribute("tags", taxonomy.list(true));
        return "admin/post-form";
    }
}
