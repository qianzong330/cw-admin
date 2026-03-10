package com.example.hello.controller;

import com.example.hello.entity.Employee;
import com.example.hello.entity.Project;
import com.example.hello.entity.Role;
import com.example.hello.entity.JobCategory;
import com.example.hello.service.EmployeeService;
import com.example.hello.service.ProjectService;
import com.example.hello.service.RoleService;
import com.example.hello.service.JobCategoryService;
import com.example.hello.service.WorkHourConfigService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private JobCategoryService jobCategoryService;
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private WorkHourConfigService workHourConfigService;

    @GetMapping("/list")
    public String list(@RequestParam(required = false) Long projectId, Model model, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (!currentUser.hasPermission("employee")) {
            return "redirect:/index";
        }
        
        List<Employee> employees;
        if (projectId != null) {
            employees = employeeService.findByProjectId(projectId);
        } else {
            employees = employeeService.findAll();
        }
        
        List<Role> roles = roleService.findAll();
        List<JobCategory> jobCategories = jobCategoryService.findAll();
        List<Project> projects = projectService.findAll();
        List<com.example.hello.entity.WorkHourConfig> workHourConfigs = workHourConfigService.getAllConfigs();
        
        model.addAttribute("employees", employees);
        model.addAttribute("roles", roles);
        model.addAttribute("jobCategories", jobCategories);
        model.addAttribute("projects", projects);
        model.addAttribute("workHourConfigs", workHourConfigs);
        model.addAttribute("selectedProjectId", projectId);
        return "employee/list";
    }

    @GetMapping("/add")
    public String addPage(Model model, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (!currentUser.hasPermission("employee")) {
            return "redirect:/index";
        }
        
        List<Role> roles = roleService.findAll();
        List<JobCategory> jobCategories = jobCategoryService.findAll();
        
        model.addAttribute("roles", roles);
        model.addAttribute("jobCategories", jobCategories);
        model.addAttribute("employee", new Employee());
        return "employee/form";
    }

    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable Long id, Model model, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (!currentUser.hasPermission("employee")) {
            return "redirect:/index";
        }
        
        Employee employee = employeeService.findById(id);
        List<Role> roles = roleService.findAll();
        List<JobCategory> jobCategories = jobCategoryService.findAll();
        
        model.addAttribute("roles", roles);
        model.addAttribute("jobCategories", jobCategories);
        model.addAttribute("employee", employee);
        return "employee/form";
    }

    @PostMapping("/save")
    public String save(Employee employee, @RequestParam(required = false) List<Long> projectIds, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (!currentUser.hasPermission("employee")) {
            return "redirect:/index";
        }
        
        // 新增员工时设置默认状态，密码由用户输入（未输则用 123456）
        if (employee.getId() == null) {
            if (employee.getPassword() == null || employee.getPassword().isEmpty()) {
                employee.setPassword("123456");
            }
            employee.setStatus(1);
            // 加密密码
            employeeService.encodePassword(employee);
        } else {
            // 编辑员工时，如果密码为空，保持原密码不变
            Employee existingEmployee = employeeService.findById(employee.getId());
            if (existingEmployee != null && (employee.getPassword() == null || employee.getPassword().isEmpty())) {
                employee.setPassword(existingEmployee.getPassword());
            } else {
                // 加密新密码
                employeeService.encodePassword(employee);
            }
        }
        
        employeeService.save(employee, projectIds);
        return "redirect:/employee/list";
    }

    @GetMapping("/projectIds/{id}")
    @ResponseBody
    public Map<String, Object> getEmployeeProjectIds(@PathVariable Long id, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Map.of("success", false, "message", "未登录");
        }
        List<Long> projectIds = employeeService.findProjectIdsByEmployeeId(id);
        return Map.of("success", true, "projectIds", projectIds);
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (!currentUser.hasPermission("employee")) {
            return "redirect:/index";
        }
        
        employeeService.deleteById(id);
        return "redirect:/employee/list";
    }
}
