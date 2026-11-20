package com.example.personalblog.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.personalblog.service.PostService;
import com.example.personalblog.service.TaxonomyService;

@Controller
public class PublicController {
    private final PostService posts;
    private final TaxonomyService taxonomy;

    public PublicController(PostService posts, TaxonomyService taxonomy) {
        this.posts = posts;
        this.taxonomy = taxonomy;
    }

    @GetMapping("/")
    public String home(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) Long tag,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("posts", posts.list(true, q, category, tag, null, page));
        model.addAttribute("categories", taxonomy.list(false));
        model.addAttribute("tags", taxonomy.list(true));
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("tag", tag);
        return "public/home";
    }

    @GetMapping("/posts/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("detail", posts.published(id));
        return "public/post";
    }

    @GetMapping("/about")
    public String about() {
        return "public/about";
    }
}
