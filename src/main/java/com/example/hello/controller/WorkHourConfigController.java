package com.example.hello.controller;

import com.example.hello.entity.Employee;
import com.example.hello.entity.WorkHourConfig;
import com.example.hello.service.WorkHourConfigService;
import com.example.hello.service.ProjectService;
import com.example.hello.entity.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/workhour")
public class WorkHourConfigController {
    
    @Autowired
    private WorkHourConfigService workHourConfigService;
    
    @Autowired
    private ProjectService projectService;
    
    @GetMapping("/config")
    public String configList(Model model, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        List<WorkHourConfig> configs = workHourConfigService.getAllConfigs();
        List<Project> projects = projectService.findAll();
        model.addAttribute("configs", configs);
        model.addAttribute("projects", projects);
        return "workhour/config";
    }
    
    /**
     * 工时配置审批页面
     */
    @GetMapping("/pending")
    public String pendingList(Model model, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        // 只有具备审批权限的角色才能访问审批页面
        if (!currentUser.hasPermission("workhour:approve")) {
            return "redirect:/workhour/config";
        }
        
        // 获取所有审批中的配置
        List<WorkHourConfig> pendingConfigs = workHourConfigService.getPendingConfigs();
        model.addAttribute("pendingConfigs", pendingConfigs);
        return "workhour/pending";
    }
    
    @PostMapping("/save")
    @ResponseBody
    public String save(@RequestBody WorkHourConfig config, HttpSession session) {
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                return "请先登录";
            }
            
            // 校验：必须选择项目
            if (config.getProjectId() == null) {
                return "请选择项目";
            }
            
            // 校验：同一项目下不能有两个相同类型的配置
            if (config.getId() == null) {
                // 新增时检查
                WorkHourConfig existing = workHourConfigService.findByProjectIdAndCalcType(config.getProjectId(), config.getCalcType());
                if (existing != null) {
                    String typeName = config.getCalcType() == 1 ? "日薪" : "月薪";
                    return "该项目已存在" + typeName + "计算方式的配置，不能重复创建";
                }
                workHourConfigService.createConfig(config, currentUser);
            } else {
                // 编辑时检查（排除自己）
                WorkHourConfig existing = workHourConfigService.findByProjectIdAndCalcType(config.getProjectId(), config.getCalcType());
                if (existing != null && !existing.getId().equals(config.getId())) {
                    String typeName = config.getCalcType() == 1 ? "日薪" : "月薪";
                    return "该项目已存在" + typeName + "计算方式的配置，不能重复创建";
                }
                workHourConfigService.updateConfig(config, currentUser);
            }
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
    
    /**
     * 禁用生效中的配置
     */
    @PostMapping("/deactivate/{id}")
    @ResponseBody
    public String deactivate(@PathVariable Long id, HttpSession session) {
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                return "请先登录";
            }
            
            workHourConfigService.deactivateConfig(id);
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
    
    /**
     * 审批通过
     */
    @PostMapping("/approve/{id}")
    @ResponseBody
    public String approve(@PathVariable Long id, @RequestParam(required = false) String remark, HttpSession session) {
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                return "请先登录";
            }
            
            // 只有 BOSS 才能审批
            if (!currentUser.isBoss()) {
                return "只有BOSS才能审批工时配置";
            }
            
            workHourConfigService.approveConfig(id, currentUser, remark);
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
    
    /**
     * 审批拒绝
     */
    @PostMapping("/reject/{id}")
    @ResponseBody
    public String reject(@PathVariable Long id, @RequestParam(required = false) String remark, HttpSession session) {
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                return "请先登录";
            }
            
            // 只有 BOSS 才能拒绝
            if (!currentUser.isBoss()) {
                return "只有BOSS才能审批工时配置";
            }
            
            workHourConfigService.rejectConfig(id, currentUser, remark);
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
    
    /**
     * 撤销审批
     */
    @PostMapping("/revoke/{id}")
    @ResponseBody
    public String revoke(@PathVariable Long id, HttpSession session) {
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                return "请先登录";
            }
            
            workHourConfigService.revokeConfig(id, currentUser.getId());
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
    
    /**
     * 删除未生效配置
     * BOSS：直接删除
     * 管理员：未生效直接删除，生效中需先发起作废审批
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public String delete(@PathVariable Long id, HttpSession session) {
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                return "请先登录";
            }
            
            workHourConfigService.deleteConfig(id, currentUser);
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
    
    /**
     * 管理员发起删除/作废审批（对生效中配置）
     */
    @PostMapping("/request-delete/{id}")
    @ResponseBody
    public String requestDeleteApproval(@PathVariable Long id, HttpSession session) {
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                return "请先登录";
            }
            
            // 只有管理员才能发起
            if (!"admin".equalsIgnoreCase(currentUser.getRoleCode())) {
                return "只有管理员才能发起删除审批";
            }
            
            workHourConfigService.requestDeleteApproval(id, currentUser);
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
    
    /**
     * 作废生效中的配置（仅 BOSS 可用）
     */
    @PostMapping("/invalidate/{id}")
    @ResponseBody
    public String invalidate(@PathVariable Long id, HttpSession session) {
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                return "请先登录";
            }
            
            workHourConfigService.invalidateConfig(id, currentUser);
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
    
    /**
     * 删除所有配置（仅BOSS可用）
     */
    @PostMapping("/deleteAll")
    @ResponseBody
    public String deleteAll(HttpSession session) {
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                return "请先登录";
            }
            
            // 只有具备审批权限的角色才能执行
            if (!currentUser.hasPermission("workhour:approve")) {
                return "没有权限执行此操作";
            }
            
            workHourConfigService.deleteAllConfigs();
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
    
    @GetMapping("/get/{id}")
    @ResponseBody
    public WorkHourConfig getById(@PathVariable Long id) {
        return workHourConfigService.getConfigById(id);
    }
    
    @GetMapping("/default")
    @ResponseBody
    public WorkHourConfig getDefault() {
        return workHourConfigService.getActiveConfig();
    }
    
    /**
     * 计算当月工作日天数（排除周末）
     */
    @GetMapping("/workdays")
    @ResponseBody
    public Map<String, Object> getWorkDays(@RequestParam(required = false) String yearMonth) {
        Map<String, Object> result = new HashMap<>();
        try {
            YearMonth ym;
            if (yearMonth != null && !yearMonth.isEmpty()) {
                ym = YearMonth.parse(yearMonth);
            } else {
                ym = YearMonth.now();
            }
            
            int workDays = 0;
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                DayOfWeek dayOfWeek = date.getDayOfWeek();
                if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                    workDays++;
                }
            }
            result.put("success", true);
            result.put("workDays", workDays);
            result.put("yearMonth", ym.toString());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取可用的工时配置列表（供新增考勤弹窗下拉选择）
     */
    @GetMapping("/configs-for-select")
    @ResponseBody
    public List<Map<String, Object>> getConfigsForSelect() {
        List<WorkHourConfig> configs = workHourConfigService.getAllConfigs();
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        configs.stream()
            .filter(c -> c.getStatus() != null && c.getStatus() == 5)
            .forEach(c -> {
                Map<String, Object> item = new java.util.LinkedHashMap<>();
                item.put("id", c.getId());
                item.put("calcType", c.getCalcType());
                item.put("calcTypeName", c.getCalcType() == 1 ? "日薪计算" : "月薪计算");
                item.put("dailyWorkHours", c.getDailyWorkHours());
                result.add(item);
            });
        return result;
    }
}
