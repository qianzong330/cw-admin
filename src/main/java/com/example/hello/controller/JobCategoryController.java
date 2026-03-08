package com.example.hello.controller;

import com.example.hello.entity.Employee;
import com.example.hello.entity.JobCategory;
import com.example.hello.service.JobCategoryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/jobcategory")
public class JobCategoryController {

    @Autowired
    private JobCategoryService jobCategoryService;

    @GetMapping("/list")
    public String list(Model model, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (currentUser == null || !currentUser.hasPermission("jobcategory")) {
            return "redirect:/index";
        }
        
        List<JobCategory> jobCategories = jobCategoryService.findAll();
        model.addAttribute("jobCategories", jobCategories);
        return "jobcategory/list";
    }

    @GetMapping("/add")
    public String addPage(Model model, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (currentUser == null || !currentUser.hasPermission("jobcategory:add")) {
            return "redirect:/index";
        }
        
        model.addAttribute("jobCategory", new JobCategory());
        return "jobcategory/form";
    }

    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable Long id, Model model, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (currentUser == null || !currentUser.hasPermission("jobcategory:edit")) {
            return "redirect:/index";
        }
        
        JobCategory jobCategory = jobCategoryService.findById(id);
        model.addAttribute("jobCategory", jobCategory);
        return "jobcategory/form";
    }

    @PostMapping("/save")
    public String save(JobCategory jobCategory, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (currentUser == null || !currentUser.hasPermission("jobcategory:add")) {
            return "redirect:/index";
        }
        
        // 检查名称是否重复（编辑时排除自己）
        JobCategory existing = jobCategoryService.findByName(jobCategory.getName());
        if (existing != null && !existing.getId().equals(jobCategory.getId())) {
            return "redirect:/jobcategory/list?error=duplicate";
        }
        
        jobCategoryService.save(jobCategory);
        return "redirect:/jobcategory/list";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (currentUser == null || !currentUser.hasPermission("jobcategory:delete")) {
            return "redirect:/index";
        }
        
        jobCategoryService.deleteById(id);
        return "redirect:/jobcategory/list";
    }
}
