package com.example.personalblog.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.personalblog.dto.NameForm;
import com.example.personalblog.exception.RuleViolation;
import com.example.personalblog.service.TaxonomyService;

@Controller
@RequestMapping("/admin/{kind:categories|tags}")
public class TaxonomyController {
    private final TaxonomyService taxonomy;

    public TaxonomyController(TaxonomyService taxonomy) {
        this.taxonomy = taxonomy;
    }

    @GetMapping
    public String list(@PathVariable String kind, Model model) {
        model.addAttribute("nameForm", new NameForm());
        return view(kind, null, model);
    }

    @PostMapping({"", "/{id}"})
    public String save(
            @PathVariable String kind,
            @PathVariable(required = false) Long id,
            @Valid @ModelAttribute NameForm nameForm,
            BindingResult errors,
            Model model,
            RedirectAttributes redirect) {
        if (!errors.hasErrors())
            try {
                taxonomy.save(kind.equals("tags"), id, nameForm);
                redirect.addFlashAttribute("message", "名稱已儲存");
                return "redirect:/admin/" + kind;
            } catch (RuleViolation error) {
                errors.rejectValue("name", "rule", error.getMessage());
            }
        return view(kind, id, model);
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable String kind, @PathVariable Long id, RedirectAttributes redirect) {
        try {
            taxonomy.delete(kind.equals("tags"), id);
            redirect.addFlashAttribute("message", "已刪除");
        } catch (RuleViolation error) {
            redirect.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/admin/" + kind;
    }

    private String view(String kind, Long id, Model model) {
        model.addAttribute("kind", kind);
        model.addAttribute("label", kind.equals("tags") ? "標籤" : "分類");
        model.addAttribute("editId", id);
        model.addAttribute("items", taxonomy.list(kind.equals("tags")));
        return "admin/taxonomy";
    }
}
