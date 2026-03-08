package com.example.hello.controller;

import com.example.hello.entity.Employee;
import com.example.hello.entity.EmployeeProject;
import com.example.hello.entity.Project;
import com.example.hello.service.EmployeeService;
import com.example.hello.service.ProjectService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/project")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/list")
    public String list(Model model, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        List<Project> projects = projectService.findAll();
        List<Employee> employees = employeeService.findAll();
        model.addAttribute("projects", projects);
        model.addAttribute("employees", employees);
        model.addAttribute("currentUser", currentUser);
        return "project/list";
    }

    @GetMapping("/add")
    public String addPage(Model model) {
        model.addAttribute("project", new Project());
        return "project/form";
    }

    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable Long id, Model model) {
        Project project = projectService.findById(id);
        model.addAttribute("project", project);
        return "project/form";
    }

    @PostMapping("/save")
    public String save(Project project, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (!currentUser.hasPermission("project")) {
            return "redirect:/index";
        }
        
        projectService.save(project);
        return "redirect:/project/list";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (!currentUser.hasPermission("project")) {
            return "redirect:/index";
        }
        
        projectService.deleteById(id);
        return "redirect:/project/list";
    }

    /**
     * 获取项目的关联员工
     */
    @GetMapping("/employees/{projectId}")
    @ResponseBody
    public List<EmployeeProject> getProjectEmployees(@PathVariable Long projectId, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (currentUser == null) {
            return null;
        }
        return projectService.getProjectEmployees(projectId);
    }

    /**
     * 关联员工到项目（支持批量）
     */
    @PostMapping("/assign")
    @ResponseBody
    public String assignEmployee(@RequestParam(required = false) Long employeeId, 
                                  @RequestParam(required = false) List<Long> employeeIds,
                                  @RequestParam Long projectId, 
                                  HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "未登录";
        }
        
        // 批量关联
        if (employeeIds != null && !employeeIds.isEmpty()) {
            int successCount = 0;
            for (Long empId : employeeIds) {
                if (projectService.assignEmployeeToProject(empId, projectId)) {
                    successCount++;
                }
            }
            return successCount == employeeIds.size() ? "success" : "partial";
        }
        
        // 单个关联
        if (employeeId != null) {
            boolean success = projectService.assignEmployeeToProject(employeeId, projectId);
            return success ? "success" : "failed";
        }
        
        return "failed";
    }

    /**
     * 取消员工与项目的关联
     */
    @PostMapping("/remove")
    @ResponseBody
    public String removeEmployee(@RequestParam Long employeeId, @RequestParam Long projectId, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "未登录";
        }
        boolean success = projectService.removeEmployeeFromProject(employeeId, projectId);
        return success ? "success" : "failed";
    }
    
    /**
     * 批量取消员工与项目的关联
     */
    @PostMapping("/removeBatch")
    @ResponseBody
    public String removeEmployeesBatch(@RequestParam List<Long> employeeIds, @RequestParam Long projectId, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "未登录";
        }
        
        int successCount = 0;
        for (Long empId : employeeIds) {
            if (projectService.removeEmployeeFromProject(empId, projectId)) {
                successCount++;
            }
        }
        return successCount > 0 ? "success" : "failed";
    }
}
