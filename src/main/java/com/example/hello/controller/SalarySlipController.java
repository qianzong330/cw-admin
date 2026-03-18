package com.example.hello.controller;

import com.example.hello.entity.Employee;
import com.example.hello.entity.SalaryItem;
import com.example.hello.entity.SalaryMonthStatus;
import com.example.hello.entity.SalarySlip;
import com.example.hello.entity.SalarySlipChangeLog;
import com.example.hello.entity.AttendanceMonthStatus;
import com.example.hello.mapper.AttendanceMonthStatusMapper;
import com.example.hello.mapper.SalaryMonthStatusMapper;
import com.example.hello.mapper.SalarySlipChangeLogMapper;
import com.example.hello.mapper.SalarySlipMapper;
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
    @Autowired private AttendanceMonthStatusMapper attendanceMonthStatusMapper;
    @Autowired private SalarySlipChangeLogMapper changeLogMapper;
    @Autowired private SalarySlipMapper salarySlipMapper;
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
            
            // 状态5（已锁定）时，判断变更效果
            if (monthStatusValue == SalaryMonthStatus.STATUS_APPROVED) {
                String remark = monthStatus.getApproveRemark();
                if (remark != null) {
                    if (remark.contains("变更已生效")) {
                        result.put("changeEffect", "applied");
                    } else if (remark.contains("变更未生效")) {
                        result.put("changeEffect", "rejected");
                    }
                }
            }
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
            
            // BOSS 角色提交时直接审批通过
            boolean isBoss = currentUser.isBoss() || "root".equalsIgnoreCase(currentUser.getRoleCode());
            
            // 更新工资条月份状态
            SalaryMonthStatus record = new SalaryMonthStatus();
            record.setYearMonth(salaryPeriod);
            record.setProjectId(projectId);
            if (isBoss) {
                // BOSS 直接审批通过
                record.setStatus(SalaryMonthStatus.STATUS_APPROVED);
                record.setApproveBy(currentUser.getId());
                record.setApproveTime(java.time.LocalDateTime.now());
            } else {
                // 其他人提交待审批
                record.setStatus(SalaryMonthStatus.STATUS_PENDING);
            }
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

        // 只有当该项目该月考勤已审批通过（STATUS_APPROVED=5）时，才自动补建工资条
        // 其他状态（未提交/待审批/驳回）直接展示空列表，不触发工资条生成
        if (projectId != null) {
            AttendanceMonthStatus attendanceStatus = attendanceMonthStatusMapper.findByYearMonthAndProject(salaryPeriod, projectId);
            boolean attendanceApproved = attendanceStatus != null
                    && attendanceStatus.getStatus() == AttendanceMonthStatus.STATUS_APPROVED;
            log.info("[Salary] projectId={}, period={}, attendanceStatus={}, approved={}",
                    projectId, salaryPeriod,
                    attendanceStatus != null ? attendanceStatus.getStatus() : "null",
                    attendanceApproved);
            if (attendanceApproved) {
                // 【修改】只要考勤已审批，就为没有工资条的员工生成工资条
                // 不再判断工资条月份状态，只检查员工是否已有工资条
                log.info("[Salary] 考勤已审批通过，自动补建工资条...");
                salarySlipService.batchCreateForProject(projectId, salaryPeriod);
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
    public String updateFeeItem(@PathVariable Long id, @RequestParam BigDecimal amount, HttpSession session) {
        try {
            // 获取当前登录用户姓名
            Employee user = (Employee) session.getAttribute("user");
            String modifier = user != null ? user.getName() : null;
            salarySlipService.updateSalaryItemAmount(id, amount, modifier);
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
    public String addItem(@RequestBody SalaryItem item, HttpSession session) {
        try {
            // 获取当前登录用户姓名
            Employee user = (Employee) session.getAttribute("user");
            if (user != null) {
                item.setModifier(user.getName());
            }
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
            
            // 查询当前状态，区分首次审批和二次审批
            SalaryMonthStatus currentStatus = monthStatusMapper.findByYearMonthAndProject(salaryPeriod, projectId);
            int currentStatusValue = currentStatus != null ? currentStatus.getStatus() : SalaryMonthStatus.STATUS_DRAFT;
            
            // 通过是否存在变更日志判断是否二次审批
            boolean hasChangeLogs = changeLogMapper.countByProjectAndPeriod(projectId, salaryPeriod) > 0;
            boolean isSecondApproval = hasChangeLogs || currentStatusValue == SalaryMonthStatus.STATUS_CHANGE_PENDING;
            
            // 更新工资条月份状态
            SalaryMonthStatus record = new SalaryMonthStatus();
            record.setYearMonth(salaryPeriod);
            record.setProjectId(projectId);
            
            if (approved) {
                // 审批通过：状态变为已锁定（5）
                record.setStatus(SalaryMonthStatus.STATUS_APPROVED);
            } else {
                // 审批驳回：区分首次审批和二次审批
                if (isSecondApproval) {
                    // 二次审批驳回：保持已锁定状态（5），变更未生效
                    record.setStatus(SalaryMonthStatus.STATUS_APPROVED);
                } else {
                    // 首次审批驳回：状态变为已驳回（12）
                    record.setStatus(SalaryMonthStatus.STATUS_REJECTED);
                }
            }
            
            record.setApproveBy(currentUser.getId());
            record.setApproveTime(java.time.LocalDateTime.now());
            
            // 在审批备注中添加变更效果标记
            String effectRemark;
            if (approved) {
                effectRemark = isSecondApproval ? "[二次审批通过，变更已生效] " : "[审批通过] ";
            } else {
                effectRemark = isSecondApproval ? "[二次审批驳回，变更未生效] " : "[审批驳回] ";
            }
            record.setApproveRemark(effectRemark + (remark != null ? remark : ""));
            monthStatusMapper.updateStatus(record);
            
            // 审批通过时，先应用所有费用项变更
            if (approved && hasChangeLogs) {
                applyAllPendingFeeChanges(projectId, salaryPeriod);
            }
            
            // 审批通过或驳回时，都删除待审批的变更记录
            changeLogMapper.deleteByProjectAndPeriod(projectId, salaryPeriod);
            log.info("[Salary] 审批{}，删除项目{}月份{}的待审批费用项变更记录", 
                    approved ? "通过" : "驳回", projectId, salaryPeriod);
            
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
    
    /**
     * 应用所有待审批的费用项变更
     */
    private void applyAllPendingFeeChanges(Long projectId, String salaryPeriod) {
        // 查询所有变更记录
        List<SalarySlipChangeLog> pendingLogs = changeLogMapper.selectByProjectAndPeriod(projectId, salaryPeriod);
        
        for (SalarySlipChangeLog changeLog : pendingLogs) {
            // 应用费用项变更到数据库
            salarySlipService.applyFeeItemsChange(changeLog);
            
            // 更新工资条费用汇总
            SalarySlip slip = salarySlipMapper.selectById(changeLog.getSalarySlipId());
            if (slip != null) {
                slip.setAdditionAmount(changeLog.getNewAdditionAmount());
                slip.setDeductionAmount(changeLog.getNewDeductionAmount());
                slip.setPayableAmount(changeLog.getNewPayableAmount());
                salarySlipMapper.update(slip);
            }
        }
        
        log.info("[Salary] 应用所有待审批费用项变更: projectId={}, period={}, 变更数量={}", 
                projectId, salaryPeriod, pendingLogs.size());
    }

    /**
     * 取消待审批的变更（用户主动取消编辑）
     */
    @PostMapping("/month/cancel")
    @ResponseBody
    public String cancelPendingChanges(@RequestParam Long projectId,
                                        @RequestParam String salaryPeriod,
                                        HttpSession session) {
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) return "请先登录";
            
            // 删除变更记录
            int deleted = changeLogMapper.deleteByProjectAndPeriod(projectId, salaryPeriod);
            log.info("[Salary] 用户{}取消变更，删除项目{}月份{}的{}条待审批变更记录", 
                    currentUser.getName(), projectId, salaryPeriod, deleted);
            
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * 获取年度汇总数据（整体合计）
     */
    @GetMapping("/api/year-total")
    @ResponseBody
    public Map<String, Object> getYearTotal(@RequestParam Long projectId,
                                            @RequestParam String year) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = salarySlipService.getYearTotal(projectId, year);
            result.put("success", true);
            result.put("data", data);
        } catch (Exception e) {
            log.error("[YearTotal] error projectId={}, year={}", projectId, year, e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取年度按员工聚合的工资条汇总列表（仅已审批通过月份）
     */
    @GetMapping("/api/year-by-employee")
    @ResponseBody
    public Map<String, Object> getYearByEmployee(@RequestParam Long projectId,
                                                 @RequestParam String year) {
        Map<String, Object> result = new HashMap<>();
        try {
            var list = salarySlipService.getYearByEmployee(projectId, year);
            result.put("success", true);
            result.put("list", list);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取某员工在某项目某年度已审批通过的工资条列表（含费用项）
     */
    @GetMapping("/api/year-slips-by-employee")
    @ResponseBody
    public Map<String, Object> getYearSlipsByEmployee(@RequestParam Long projectId,
                                                      @RequestParam Long employeeId,
                                                      @RequestParam String year) {
        Map<String, Object> result = new HashMap<>();
        try {
            var slips = salarySlipService.getYearSlipsByEmployee(projectId, employeeId, year);
            result.put("success", true);
            result.put("slips", slips);
        } catch (Exception e) {
            log.error("[YearSlipsByEmployee] error", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    // 【已移除】二次修改与重新计算 API
    // 工资条重新计算功能已去除，考勤数据变化不再触发重新计算流程
    
    /**
     * 查询工资条的待审批修改记录
     */
    @GetMapping("/api/pending-changes")
    @ResponseBody
    public Map<String, Object> getPendingChanges(@RequestParam Long salarySlipId) {
        Map<String, Object> result = new HashMap<>();
        try {
            var log = salarySlipService.getPendingChangeLog(salarySlipId);
            result.put("success", true);
            result.put("hasPending", log != null);
            result.put("changeLog", log);
        } catch (Exception e) {
            log.error("[PendingChanges] error", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 查询项目和月份的所有修改记录
     */
    @GetMapping("/api/change-logs")
    @ResponseBody
    public Map<String, Object> getChangeLogs(@RequestParam Long projectId,
                                              @RequestParam String salaryPeriod) {
        Map<String, Object> result = new HashMap<>();
        try {
            var logs = salarySlipService.getChangeLogsByProjectAndPeriod(projectId, salaryPeriod);
            result.put("success", true);
            result.put("logs", logs);
        } catch (Exception e) {
            log.error("[ChangeLogs] error", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 审批通过工资条修改
     */
    @PostMapping("/api/approve-change")
    @ResponseBody
    public Map<String, Object> approveChange(@RequestParam Long changeLogId,
                                              @RequestParam(required = false) String remark,
                                              HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            
            // 检查权限（BOSS或项目管理员）
            boolean canApprove = currentUser.isBoss() || "root".equalsIgnoreCase(currentUser.getRoleCode());
            if (!canApprove) {
                result.put("success", false);
                result.put("message", "无权审批");
                return result;
            }
            
            salarySlipService.approveFeeChangeLog(changeLogId, currentUser.getId(), 
                    currentUser.getName(), remark);
            
            result.put("success", true);
            result.put("message", "审批通过");
        } catch (Exception e) {
            log.error("[ApproveChange] error", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 驳回工资条修改
     */
    @PostMapping("/api/reject-change")
    @ResponseBody
    public Map<String, Object> rejectChange(@RequestParam Long changeLogId,
                                             @RequestParam(required = false) String remark,
                                             HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            
            // 检查权限（BOSS或项目管理员）
            boolean canApprove = currentUser.isBoss() || "root".equalsIgnoreCase(currentUser.getRoleCode());
            if (!canApprove) {
                result.put("success", false);
                result.put("message", "无权审批");
                return result;
            }
            
            salarySlipService.rejectFeeChangeLog(changeLogId, currentUser.getId(), 
                    currentUser.getName(), remark);
            
            result.put("success", true);
            result.put("message", "已驳回");
        } catch (Exception e) {
            log.error("[RejectChange] error", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 批量审批工资条修改（按项目和月份）
     */
    @PostMapping("/api/batch-approve-changes")
    @ResponseBody
    public Map<String, Object> batchApproveChanges(@RequestParam Long projectId,
                                                    @RequestParam String salaryPeriod,
                                                    @RequestParam(required = false) String remark,
                                                    HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            
            // 检查权限（BOSS或项目管理员）
            boolean canApprove = currentUser.isBoss() || "root".equalsIgnoreCase(currentUser.getRoleCode());
            if (!canApprove) {
                result.put("success", false);
                result.put("message", "无权审批");
                return result;
            }
            
            salarySlipService.batchApproveFeeChanges(projectId, salaryPeriod, 
                    currentUser.getId(), currentUser.getName(), remark);
            
            result.put("success", true);
            result.put("message", "批量审批通过");
        } catch (Exception e) {
            log.error("[BatchApproveChanges] error", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 批量驳回工资条修改（按项目和月份）
     */
    @PostMapping("/api/batch-reject-changes")
    @ResponseBody
    public Map<String, Object> batchRejectChanges(@RequestParam Long projectId,
                                                   @RequestParam String salaryPeriod,
                                                   @RequestParam(required = false) String remark,
                                                   HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            
            // 检查权限（BOSS或项目管理员）
            boolean canApprove = currentUser.isBoss() || "root".equalsIgnoreCase(currentUser.getRoleCode());
            if (!canApprove) {
                result.put("success", false);
                result.put("message", "无权审批");
                return result;
            }
            
            salarySlipService.batchRejectFeeChanges(projectId, salaryPeriod, 
                    currentUser.getId(), currentUser.getName(), remark);
            
            result.put("success", true);
            result.put("message", "批量驳回成功");
        } catch (Exception e) {
            log.error("[BatchRejectChanges] error", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    // ===== 费用项变更相关 API =====
    
    /**
     * 记录费用项变更（实时抵消计算）
     */
    @PostMapping("/api/record-fee-item-change")
    @ResponseBody
    public Map<String, Object> recordFeeItemChange(@RequestParam Long salarySlipId,
                                                    @RequestParam Long itemId,
                                                    @RequestParam String itemName,
                                                    @RequestParam Integer itemType,
                                                    @RequestParam String operation,
                                                    @RequestParam BigDecimal oldAmount,
                                                    @RequestParam BigDecimal newAmount,
                                                    @RequestParam String changeReason,
                                                    HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            
            boolean recorded = salarySlipService.recordFeeItemChange(
                    salarySlipId, itemId, itemName, itemType, operation, 
                    oldAmount, newAmount, changeReason, 
                    currentUser.getId(), currentUser.getName());
            
            result.put("success", true);
            if (recorded) {
                result.put("message", "已记录变更");
            } else {
                result.put("message", "变更已抵消");
            }
        } catch (Exception e) {
            log.error("[RecordFeeItemChange] error", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 提交费用项变更审批
     */
    @PostMapping("/api/submit-fee-changes")
    @ResponseBody
    public Map<String, Object> submitFeeChanges(@RequestParam Long projectId,
                                                 @RequestParam String salaryPeriod,
                                                 @RequestParam String changeReason,
                                                 HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            
            salarySlipService.submitFeeChangesForApproval(
                    projectId, salaryPeriod, currentUser.getId(), changeReason);
            
            result.put("success", true);
            result.put("message", "变更已提交审批");
        } catch (Exception e) {
            log.error("[SubmitFeeChanges] error", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 审批通过费用项变更
     */
    @PostMapping("/api/approve-fee-change")
    @ResponseBody
    public Map<String, Object> approveFeeChange(@RequestParam Long changeLogId,
                                                 @RequestParam(required = false) String remark,
                                                 HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            
            // 检查权限（BOSS或项目管理员）
            boolean canApprove = currentUser.isBoss() || "root".equalsIgnoreCase(currentUser.getRoleCode());
            if (!canApprove) {
                result.put("success", false);
                result.put("message", "无权审批");
                return result;
            }
            
            salarySlipService.approveFeeChangeLog(changeLogId, currentUser.getId(), 
                    currentUser.getName(), remark);
            
            result.put("success", true);
            result.put("message", "费用项变更审批通过");
        } catch (Exception e) {
            log.error("[ApproveFeeChange] error", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 驳回费用项变更
     */
    @PostMapping("/api/reject-fee-change")
    @ResponseBody
    public Map<String, Object> rejectFeeChange(@RequestParam Long changeLogId,
                                                @RequestParam(required = false) String remark,
                                                HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            
            // 检查权限（BOSS或项目管理员）
            boolean canApprove = currentUser.isBoss() || "root".equalsIgnoreCase(currentUser.getRoleCode());
            if (!canApprove) {
                result.put("success", false);
                result.put("message", "无权审批");
                return result;
            }
            
            salarySlipService.rejectFeeChangeLog(changeLogId, currentUser.getId(), 
                    currentUser.getName(), remark);
            
            result.put("success", true);
            result.put("message", "费用项变更已驳回");
        } catch (Exception e) {
            log.error("[RejectFeeChange] error", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 取消费用项变更
     */
    @PostMapping("/api/cancel-fee-change")
    @ResponseBody
    public Map<String, Object> cancelFeeChange(@RequestParam Long projectId,
                                                @RequestParam String salaryPeriod,
                                                HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            
            salarySlipService.cancelFeeChange(projectId, salaryPeriod, currentUser.getId());
            
            result.put("success", true);
            result.put("message", "变更已取消");
        } catch (Exception e) {
            log.error("[CancelFeeChange] error", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 进入编辑模式（将状态改为变更待审 status=2）
     */
    @PostMapping("/api/enter-edit-mode")
    @ResponseBody
    public Map<String, Object> enterEditMode(@RequestParam Long projectId,
                                              @RequestParam String salaryPeriod,
                                              @RequestParam(required = false) String changeReason,
                                              HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            
            salarySlipService.enterEditMode(projectId, salaryPeriod, 
                    currentUser.getId(), changeReason);
            
            result.put("success", true);
            result.put("message", "已进入编辑模式");
        } catch (Exception e) {
            log.error("[EnterEditMode] error", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 批量审批费用项变更（按项目和月份）
     */
    @PostMapping("/api/batch-approve-fee-changes")
    @ResponseBody
    public Map<String, Object> batchApproveFeeChanges(@RequestParam Long projectId,
                                                       @RequestParam String salaryPeriod,
                                                       @RequestParam(required = false) String remark,
                                                       HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            
            // 检查权限（BOSS或项目管理员）
            boolean canApprove = currentUser.isBoss() || "root".equalsIgnoreCase(currentUser.getRoleCode());
            if (!canApprove) {
                result.put("success", false);
                result.put("message", "无权审批");
                return result;
            }
            
            salarySlipService.batchApproveFeeChanges(projectId, salaryPeriod, 
                    currentUser.getId(), currentUser.getName(), remark);
            
            result.put("success", true);
            result.put("message", "费用项变更批量审批通过");
        } catch (Exception e) {
            log.error("[BatchApproveFeeChanges] error", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 查询工资条的费用项变更记录
     */
    @GetMapping("/api/fee-change-logs")
    @ResponseBody
    public Map<String, Object> getFeeChangeLogs(@RequestParam Long salarySlipId) {
        Map<String, Object> result = new HashMap<>();
        try {
            var logs = salarySlipService.getFeeChangeLogsBySalarySlipId(salarySlipId);
            result.put("success", true);
            result.put("logs", logs);
        } catch (Exception e) {
            log.error("[FeeChangeLogs] error", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 查询项目和月份的所有费用项变更记录
     */
    @GetMapping("/api/fee-change-logs-by-period")
    @ResponseBody
    public Map<String, Object> getFeeChangeLogsByPeriod(@RequestParam Long projectId,
                                                         @RequestParam String salaryPeriod) {
        Map<String, Object> result = new HashMap<>();
        try {
            var logs = salarySlipService.getFeeChangeLogsByProjectAndPeriod(projectId, salaryPeriod);
            result.put("success", true);
            result.put("logs", logs);
        } catch (Exception e) {
            log.error("[FeeChangeLogsByPeriod] error", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 检查工资条是否有待审批的费用项变更
     */
    @GetMapping("/api/has-pending-fee-change")
    @ResponseBody
    public Map<String, Object> hasPendingFeeChange(@RequestParam Long salarySlipId) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean hasPending = salarySlipService.hasPendingFeeChange(salarySlipId);
            result.put("success", true);
            result.put("hasPending", hasPending);
        } catch (Exception e) {
            log.error("[HasPendingFeeChange] error", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
