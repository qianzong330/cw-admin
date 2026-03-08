package com.example.hello.controller;

import com.example.hello.entity.Category;
import com.example.hello.entity.Employee;
import com.example.hello.service.CategoryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/list")
    public String list(Model model) {
        List<Category> categories = categoryService.findAll();
        model.addAttribute("categories", categories);
        return "category/list";
    }

    @GetMapping("/add")
    public String addPage(Model model) {
        model.addAttribute("category", new Category());
        return "category/form";
    }

    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable Long id, Model model) {
        Category category = categoryService.findById(id);
        model.addAttribute("category", category);
        return "category/form";
    }

    @PostMapping("/save")
    public String save(Category category, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (!currentUser.hasPermission("category")) {
            return "redirect:/index";
        }
        
        // 检查名称是否重复（编辑时排除自己）
        Category existing = categoryService.findByName(category.getName());
        if (existing != null && !existing.getId().equals(category.getId())) {
            // 名称已存在
            return "redirect:/category/list?error=duplicate";
        }
        
        categoryService.save(category);
        return "redirect:/category/list";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (!currentUser.hasPermission("category")) {
            return "redirect:/index";
        }
        
        categoryService.deleteById(id);
        return "redirect:/category/list";
    }
}
