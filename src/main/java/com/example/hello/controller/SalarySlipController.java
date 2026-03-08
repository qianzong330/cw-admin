package com.example.hello.controller;

import com.example.hello.entity.Employee;
import com.example.hello.entity.SalaryItem;
import com.example.hello.entity.SalaryMonthStatus;
import com.example.hello.entity.SalarySlip;
import com.example.hello.mapper.SalaryMonthStatusMapper;
import com.example.hello.service.AttendanceService;
import com.example.hello.service.EmployeeService;
import com.example.hello.service.ProjectService;
import com.example.hello.service.SalarySlipService;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工资条管理控制器
 */
@Controller
@RequestMapping("/salary")
public class SalarySlipController {

    private static final Logger log = LoggerFactory.getLogger(SalarySlipController.class);

    @Autowired private SalarySlipService salarySlipService;
    @Autowired private EmployeeService   employeeService;
    @Autowired private ProjectService    projectService;
    @Autowired private AttendanceService attendanceService;
    @Autowired private SalaryMonthStatusMapper monthStatusMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    /**
     * API：获取工资条列表（含月度状态）
     */
    @GetMapping("/api/list")
    @ResponseBody
    public Map<String, Object> apiList(@RequestParam Long projectId,
                                        @RequestParam String salaryPeriod) {
        Map<String, Object> result = new HashMap<>();
        
        // 查询工资条月份状态（工资条本身无状态，由月份状态表控制）
        SalaryMonthStatus monthStatus = monthStatusMapper.findByYearMonthAndProject(salaryPeriod, projectId);
        int monthStatusValue = monthStatus != null ? monthStatus.getStatus() : SalaryMonthStatus.STATUS_DRAFT;
        result.put("monthStatus", monthStatusValue);
        
        // 添加审批人信息和驳回原因
        if (monthStatus != null) {
            result.put("approveBy", monthStatus.getApproveBy());
            result.put("approveTime", monthStatus.getApproveTime());
            result.put("approveRemark", monthStatus.getApproveRemark());
            result.put("submitBy", monthStatus.getSubmitBy());
            result.put("submitTime", monthStatus.getSubmitTime());
        }
        
        // 查询所有BOSS角色用户（用于显示等待谁审批）
        List<Map<String, Object>> bosses = jdbcTemplate.queryForList(
            "SELECT e.id, e.name FROM tb_employee e JOIN tb_role r ON e.role_id = r.id WHERE r.role_code = 'boss' AND e.status = 1"
        );
        result.put("bosses", bosses);
        
        // 查询工资条列表
        List<SalarySlip> slips = salarySlipService.listSalarySlips(null, salaryPeriod, null, projectId);
        
        // 填充费用项，并重新计算瞬态字段（出勤工时、每日工时等，供前端显示计算说明）
        for (SalarySlip slip : slips) {
            slip.setItems(salarySlipService.getSalarySlipById(slip.getId()).getItems());
            // 日薪员工：重新调用工时配置计算，将中间数据填充到 slip 瞬态字段
            if (slip.getSalaryType() != null && slip.getSalaryType() == 1
                    && slip.getEmployeeId() != null && slip.getSalaryPeriod() != null) {
                try {
                    YearMonth ym = YearMonth.parse(slip.getSalaryPeriod());
                    attendanceService.calcAttendanceDaysWithConfig(slip.getEmployeeId(), ym, slip);
                } catch (Exception e) {
                    log.warn("[apiList] fill transient fields failed for slip {}: {}", slip.getId(), e.getMessage());
                }
            }
        }
        result.put("slips", slips);
        
        return result;
    }

    /**
     * 提交整月工资条审批
     */
    @PostMapping("/month/submit")
    @ResponseBody
    public String submitMonth(@RequestParam Long projectId,
                              @RequestParam String salaryPeriod,
                              HttpSession session) {
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) return "请先登录";
            
            // 更新工资条月份状态为待审批
            SalaryMonthStatus record = new SalaryMonthStatus();
            record.setYearMonth(salaryPeriod);
            record.setProjectId(projectId);
            record.setStatus(SalaryMonthStatus.STATUS_PENDING);
            record.setSubmitBy(currentUser.getId());
            record.setSubmitTime(java.time.LocalDateTime.now());
            monthStatusMapper.insertOrUpdate(record);
            
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * 工资条列表页面（左右双栏）
     * 如果考勤已审批通过，自动补建工资条
     */
    @GetMapping("/list")
    public String list(@RequestParam(required = false) Long projectId,
                       @RequestParam(required = false) String salaryPeriod,
                       Model model) {
        if (salaryPeriod == null || salaryPeriod.isEmpty()) {
            salaryPeriod = java.time.YearMonth.now().toString();
        }

        var projects = projectService.findAll();
        if (projectId == null && !projects.isEmpty()) {
            projectId = projects.get(0).getId();
        }

        // 检查考勤是否已审批通过，自动补建工资条（同时初始化工资条月份状态为草稿）
        if (projectId != null) {
            SalaryMonthStatus monthStatus = monthStatusMapper.findByYearMonthAndProject(salaryPeriod, projectId);
            log.info("[Salary] projectId={}, period={}, monthStatus={}", projectId, salaryPeriod, monthStatus != null ? monthStatus.getStatus() : "null");
            if (monthStatus == null || monthStatus.getStatus() == SalaryMonthStatus.STATUS_DRAFT) {
                log.info("[Salary] Calling batchCreateForProject...");
                salarySlipService.batchCreateForProject(projectId, salaryPeriod);
                // 初始化月份状态为草稿
                if (monthStatus == null) {
                    SalaryMonthStatus newStatus = new SalaryMonthStatus();
                    newStatus.setYearMonth(salaryPeriod);
                    newStatus.setProjectId(projectId);
                    newStatus.setStatus(SalaryMonthStatus.STATUS_DRAFT);
                    monthStatusMapper.insertOrUpdate(newStatus);
                }
            }
        }

        List<Employee> employees = projectId != null
                ? employeeService.findByProjectId(projectId)
                : java.util.Collections.emptyList();

        model.addAttribute("projects", projects);
        model.addAttribute("projectId", projectId);
        model.addAttribute("salaryPeriod", salaryPeriod);
        model.addAttribute("employees", employees);
        return "salary/list";
    }

    /**
     * 工资条审批页面（BOSS 查看所有待审批）- 已改为整月审批模式
     */
    @GetMapping("/pending")
    public String pendingList(Model model) {
        // 工资条本身无状态，由月份状态表控制，此页面功能已合并到 list 页面
        return "redirect:/salary/list";
    }

    // ===== 员工信息自动带入 =====

    /**
     * 返回员工基本薪资信息（用于新增工资条时前端自动填充）
     */
    @GetMapping("/employee-info/{employeeId}")
    @ResponseBody
    public Map<String, Object> getEmployeeInfo(@PathVariable Long employeeId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Employee emp = employeeService.findById(employeeId);
            if (emp != null) {
                result.put("name",            emp.getName());
                result.put("idCard",          emp.getPhone()); // phone 暂代 id_card
                result.put("jobCategoryName", emp.getJobCategoryName());
                result.put("salaryType",      emp.getSalaryType());
                result.put("baseSalary",      emp.getSalaryAmount());
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * 查询某员工某月份的工资条（用于左侧点击员工后加载右侧）
     */
    @GetMapping("/get-by-employee")
    @ResponseBody
    public SalarySlip getByEmployee(@RequestParam Long employeeId,
                                    @RequestParam String salaryPeriod) {
        return salarySlipService.getSalarySlipByEmployeeAndPeriod(employeeId, salaryPeriod);
    }

    /**
     * 根据ID查询工资条（含费用项）
     */
    @GetMapping("/get/{id}")
    @ResponseBody
    public SalarySlip getById(@PathVariable Long id) {
        return salarySlipService.getSalarySlipById(id);
    }

    // ===== 工资条 CRUD =====

    /**
     * 保存工资条（新增 / 更新备注）
     */
    @PostMapping("/save")
    @ResponseBody
    public String save(@RequestBody SalarySlip slip, HttpSession session) {
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser != null) slip.setCreatedBy(currentUser.getId());

            if (slip.getId() == null) {
                salarySlipService.createSalarySlip(slip);
            } else {
                salarySlipService.updateSalarySlip(slip);
            }
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * 删除工资条
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public String delete(@PathVariable Long id) {
        try {
            salarySlipService.deleteSalarySlip(id);
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * 更新费用项金额
     */
    @PostMapping("/item/update/{id}")
    @ResponseBody
    public String updateFeeItem(@PathVariable Long id, @RequestParam BigDecimal amount) {
        try {
            salarySlipService.updateSalaryItemAmount(id, amount);
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * 添加费用项
     */
    @PostMapping("/item/add")
    @ResponseBody
    public String addItem(@RequestBody SalaryItem item) {
        try {
            salarySlipService.addSalaryItem(item);
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * 删除费用项
     */
    @PostMapping("/item/delete/{id}")
    @ResponseBody
    public String deleteItem(@PathVariable Long id) {
        try {
            salarySlipService.deleteSalaryItem(id);
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    // ===== 审批流程 =====

    /**
     * BOSS 审批整月工资条（通过/驳回）
     */
    @PostMapping("/month/approve")
    @ResponseBody
    public String approveMonth(@RequestParam Long projectId,
                               @RequestParam String salaryPeriod,
                               @RequestParam boolean approved,
                               @RequestParam(required = false) String remark,
                               HttpSession session) {
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) return "请先登录";
            if (!currentUser.isBoss() && !"root".equalsIgnoreCase(currentUser.getRoleCode())) {
                return "只有 BOSS 或超级管理员可以审批工资条";
            }
            
            // 更新工资条月份状态
            SalaryMonthStatus record = new SalaryMonthStatus();
            record.setYearMonth(salaryPeriod);
            record.setProjectId(projectId);
            record.setStatus(approved ? SalaryMonthStatus.STATUS_APPROVED : SalaryMonthStatus.STATUS_REJECTED);
            record.setApproveBy(currentUser.getId());
            record.setApproveTime(java.time.LocalDateTime.now());
            record.setApproveRemark(remark);
            monthStatusMapper.updateStatus(record);
            
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
