package com.example.hello.controller;

import com.example.hello.entity.Employee;
import com.example.hello.entity.EmployeeProject;
import com.example.hello.entity.EmployeeProjectFlow;
import com.example.hello.entity.Project;
import com.example.hello.entity.ProjectAdmin;
import com.example.hello.entity.ProjectEmployeeSalary;
import com.example.hello.service.EmployeeService;
import com.example.hello.service.ProjectEmployeeSalaryService;
import com.example.hello.service.ProjectService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/project")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private EmployeeService employeeService;
    
    @Autowired
    private ProjectEmployeeSalaryService projectEmployeeSalaryService;

    @GetMapping("/list")
    public String list(Model model, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        List<Project> projects = projectService.findAllWithAdmins();
        List<Employee> employees = employeeService.findExcludeRoot();
        // 获取admin角色的员工列表（用于新增/编辑时的管理员选择）
        List<Employee> adminEmployees = employeeService.findByRoleCode("admin");
        model.addAttribute("projects", projects);
        model.addAttribute("employees", employees);
        model.addAttribute("adminEmployees", adminEmployees);
        model.addAttribute("currentUser", currentUser);
        return "project/list";
    }

    @GetMapping("/add")
    public String addPage(Model model) {
        model.addAttribute("project", new Project());
        // 获取admin角色的员工列表
        List<Employee> adminEmployees = employeeService.findByRoleCode("admin");
        model.addAttribute("adminEmployees", adminEmployees);
        return "project/form";
    }

    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable Long id, Model model) {
        Project project = projectService.findByIdWithAdmins(id);
        model.addAttribute("project", project);
        // 获取admin角色的员工列表
        List<Employee> adminEmployees = employeeService.findByRoleCode("admin");
        model.addAttribute("adminEmployees", adminEmployees);
        return "project/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String name,
                       @RequestParam(required = false) List<Long> adminIds,
                       HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        // BOSS/管理员/有project权限的用户可以保存项目
        if (!currentUser.isBoss() && !currentUser.hasPermission("project") && !currentUser.hasPermission("project:list")) {
            System.out.println("权限不足，无法保存项目");
            return "redirect:/index";
        }
        
        System.out.println("=== ProjectController.save 接收参数 ===");
        System.out.println("id: " + id);
        System.out.println("name: " + name);
        System.out.println("adminIds: " + adminIds);
        
        Project project = new Project();
        project.setId(id);
        project.setName(name);
        
        boolean result = projectService.saveWithAdmins(project, adminIds);
        System.out.println("保存结果: " + result);
        System.out.println("项目ID: " + project.getId());
        
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
     * 获取项目的关联员工（含薪资信息）
     */
    @GetMapping("/employees/{projectId}")
    @ResponseBody
    public List<Map<String, Object>> getProjectEmployees(@PathVariable Long projectId, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (currentUser == null) {
            return null;
        }
        
        List<EmployeeProject> employees = projectService.getProjectEmployees(projectId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 获取今天日期
        LocalDate today = LocalDate.now();
        
        for (EmployeeProject emp : employees) {
            Map<String, Object> empMap = new HashMap<>();
            empMap.put("id", emp.getId());
            empMap.put("employeeId", emp.getEmployeeId());
            empMap.put("employeeName", emp.getEmployeeName());
            empMap.put("jobCategoryName", emp.getJobCategoryName());
            empMap.put("createTime", emp.getCreateTime());
            
            // 获取当前生效的薪资（生效日期 <= 今天）
            ProjectEmployeeSalary currentSalary = projectEmployeeSalaryService.getEffectiveSalaryForDate(
                projectId, emp.getEmployeeId(), today);
            
            if (currentSalary != null) {
                empMap.put("currentSalary", currentSalary.getSalaryAmount());
                empMap.put("currentSalaryType", currentSalary.getSalaryType());
                empMap.put("currentSalaryTypeName", currentSalary.getSalaryType() == 2 ? "月薪" : "日薪");
                empMap.put("currentEffectiveDate", currentSalary.getEffectiveDate());
            } else {
                empMap.put("currentSalary", null);
                empMap.put("currentSalaryType", null);
            }
            
            // 获取下一条即将生效的薪资（生效日期 > 今天）
            List<ProjectEmployeeSalary> history = projectEmployeeSalaryService.getSalaryHistory(projectId, emp.getEmployeeId());
            ProjectEmployeeSalary nextSalary = null;
            for (ProjectEmployeeSalary s : history) {
                if (s.getEffectiveDate().isAfter(today)) {
                    nextSalary = s;
                    break;
                }
            }
            
            if (nextSalary != null) {
                empMap.put("nextSalary", nextSalary.getSalaryAmount());
                empMap.put("nextSalaryType", nextSalary.getSalaryType());
                empMap.put("nextSalaryTypeName", nextSalary.getSalaryType() == 2 ? "月薪" : "日薪");
                empMap.put("nextEffectiveDate", nextSalary.getEffectiveDate());
            } else {
                empMap.put("nextSalary", null);
            }
            
            result.add(empMap);
        }
        
        return result;
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
                if (projectService.assignEmployeeToProject(empId, projectId, currentUser.getId(), currentUser.getName())) {
                    successCount++;
                }
            }
            return successCount == employeeIds.size() ? "success" : "partial";
        }
        
        // 单个关联
        if (employeeId != null) {
            boolean success = projectService.assignEmployeeToProject(employeeId, projectId, currentUser.getId(), currentUser.getName());
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
        boolean success = projectService.removeEmployeeFromProject(employeeId, projectId, currentUser.getId(), currentUser.getName());
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
            if (projectService.removeEmployeeFromProject(empId, projectId, currentUser.getId(), currentUser.getName())) {
                successCount++;
            }
        }
        return successCount > 0 ? "success" : "failed";
    }
    
    /**
     * 获取项目的员工流动记录
     */
    @GetMapping("/flow/{projectId}")
    @ResponseBody
    public List<EmployeeProjectFlow> getProjectEmployeeFlow(@PathVariable Long projectId, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (currentUser == null) {
            return null;
        }
        return projectService.getProjectEmployeeFlow(projectId);
    }
    
    // ==================== 项目员工薪资管理 API ====================
    
    /**
     * 获取项目员工的薪资历史
     */
    @GetMapping("/salary/history")
    @ResponseBody
    public Map<String, Object> getEmployeeSalaryHistory(@RequestParam Long projectId, 
                                                         @RequestParam Long employeeId,
                                                         HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (currentUser == null) {
            result.put("success", false);
            result.put("message", "未登录");
            return result;
        }
        
        try {
            List<ProjectEmployeeSalary> history = projectEmployeeSalaryService.getSalaryHistory(projectId, employeeId);
            result.put("success", true);
            result.put("data", history);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 添加薪资记录（调薪）
     */
    @PostMapping("/salary/add")
    @ResponseBody
    public Map<String, Object> addSalaryRecord(@RequestParam Long projectId,
                                                @RequestParam Long employeeId,
                                                @RequestParam BigDecimal salaryAmount,
                                                @RequestParam Integer salaryType,
                                                @RequestParam String effectiveDate,
                                                @RequestParam(required = false) String remark,
                                                HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (currentUser == null) {
            result.put("success", false);
            result.put("message", "未登录");
            return result;
        }
        
        try {
            ProjectEmployeeSalary salary = new ProjectEmployeeSalary();
            salary.setProjectId(projectId);
            salary.setEmployeeId(employeeId);
            salary.setSalaryAmount(salaryAmount);
            salary.setSalaryType(salaryType);
            salary.setEffectiveDate(LocalDate.parse(effectiveDate));
            salary.setCreatedBy(currentUser.getId());
            salary.setRemark(remark);
            
            projectEmployeeSalaryService.addSalaryRecord(salary);
            result.put("success", true);
            result.put("message", "调薪成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 删除薪资记录
     */
    @PostMapping("/salary/delete")
    @ResponseBody
    public Map<String, Object> deleteSalaryRecord(@RequestParam Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (currentUser == null) {
            result.put("success", false);
            result.put("message", "未登录");
            return result;
        }
        
        try {
            projectEmployeeSalaryService.deleteSalaryRecord(id);
            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
