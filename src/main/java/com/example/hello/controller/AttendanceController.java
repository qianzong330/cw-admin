package com.example.hello.controller;

import com.example.hello.entity.Attendance;
import com.example.hello.entity.AttendanceMonthStatus;
import com.example.hello.entity.Employee;
import com.example.hello.entity.Project;
import com.example.hello.mapper.AttendanceMonthStatusMapper;
import com.example.hello.service.AttendanceService;
import com.example.hello.service.EmployeeService;
import com.example.hello.service.ProjectService;
import com.example.hello.service.SalarySlipService;
import com.example.hello.service.WorkHourConfigService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 考勤管理控制器
 */
@Controller
@RequestMapping("/attendance")
public class AttendanceController {
    
    @Autowired
    private AttendanceService attendanceService;
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private WorkHourConfigService workHourConfigService;
    @Autowired
    private AttendanceMonthStatusMapper monthStatusMapper;
    @Autowired private SalarySlipService salarySlipService;
        @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    
    /**
     * 考勤列表页面
     */
    @GetMapping("/list")
    public String list(@RequestParam(required = false) String yearMonth,
                       @RequestParam(required = false) Long projectId,
                       Model model,
                       HttpSession session) {
        // 默认查询本月
        if (yearMonth == null || yearMonth.isEmpty()) {
            yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        
        List<Project> projects = projectService.findAll();
        
        // 如果没有指定项目，默认选中第一个项目
        if (projectId == null && !projects.isEmpty()) {
            projectId = projects.get(0).getId();
        }
        
        // 只查询当前项目下的员工
        List<Employee> employees = projectId != null ? employeeService.findByProjectId(projectId) : new ArrayList<>();
        
        model.addAttribute("employees", employees);
        model.addAttribute("projects", projects);
        model.addAttribute("projectId", projectId);
        model.addAttribute("yearMonth", yearMonth);
        
        // 将当前用户角色传到模板
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        String roleCode = currentUser != null ? currentUser.getRoleCode() : "";
        model.addAttribute("currentRoleCode", roleCode != null ? roleCode : "");
        
        return "attendance/list";
    }
    
    /**
     * 考勤审批页面 - 显示待审批的考勤记录
     */
    @GetMapping("/pending")
    public String pendingList(Model model) {
        // 查询所有待审批的月度考勤表（状态为1-待审批或2-变更待审）
        List<AttendanceMonthStatus> pendingList = monthStatusMapper.findPendingList();
        model.addAttribute("pendingList", pendingList);
        return "attendance/pending";
    }
    
    /**
     * 保存考勤记录（新增/编辑）
     */
    @PostMapping("/save")
    @ResponseBody
    public String save(@RequestBody Attendance attendance, HttpSession session) {
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                return "请先登录";
            }
            
            attendance.setCreatedBy(currentUser.getId());
            
            if (attendance.getId() == null) {
                attendanceService.createAttendance(attendance);
            } else {
                attendanceService.updateAttendance(attendance);
            }
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
    
    /**
     * 删除考勤记录
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public String delete(@PathVariable Long id) {
        try {
            attendanceService.deleteAttendance(id);
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
    
    /**
     * 根据ID查询考勤记录
     */
    @GetMapping("/get/{id}")
    @ResponseBody
    public Attendance getById(@PathVariable Long id) {
        return attendanceService.getAttendanceById(id);
    }
    
    /**
     * 查询某员工在某月份的考勤记录（用于日历展示）
     */
    @GetMapping("/calendar/{employeeId}")
    @ResponseBody
    public List<Attendance> getCalendarData(@PathVariable Long employeeId,
                                            @RequestParam String yearMonth,
                                            @RequestParam(required = false) Long projectId) {
        return attendanceService.getAttendancesByEmployeeProjectAndMonth(employeeId, projectId, yearMonth);
    }
    

    /**
     * 员工当月考勤汇总（用于面板统计卡片）
     * 返回：正常出勤天数、实际出勤天数（加班折算）、出勤工时、加班工时、请假工时、缺勤天数
     */
    @GetMapping("/summary/{employeeId}")
    @ResponseBody
    public Map<String, Object> summary(@PathVariable Long employeeId,
                                       @RequestParam String yearMonth,
                                       @RequestParam(required = false) Long projectId) {
        List<Attendance> list = attendanceService.getAttendancesByEmployeeProjectAndMonth(employeeId, projectId, yearMonth);
        Map<String, Object> result = new LinkedHashMap<>();

        // 获取工时配置（用于加班折算）- 使用默认配置
        com.example.hello.entity.WorkHourConfig config = workHourConfigService.getActiveConfig();
        double standardHours = (config != null && config.getDailyWorkHours() != null)
            ? config.getDailyWorkHours().doubleValue() : 8.0;
        double weekdayRate   = (config != null && config.getWeekdayOvertimeRate()   != null)
            ? config.getWeekdayOvertimeRate().doubleValue()   : 1.5;
        double restdayRate   = (config != null && config.getRestdayOvertimeRate()   != null)
            ? config.getRestdayOvertimeRate().doubleValue()   : 2.0;
        double holidayRate   = (config != null && config.getHolidayOvertimeRate()   != null)
            ? config.getHolidayOvertimeRate().doubleValue()   : 3.0;

        double normalHours = 0, overtimeEquivHours = 0, leaveHours = 0, lateHours = 0, absentHours = 0;
        double weekdayOvertimeHours = 0, restdayOvertimeHours = 0, holidayOvertimeHours = 0;
        int absentDays = 0;
        
        for (Attendance a : list) {
            double h = a.getWorkHours() != null ? a.getWorkHours().doubleValue() : 0;
            if (a.getAttendanceType() == 1) {
                normalHours += h;
            } else if (a.getAttendanceType() == 2) {
                // 按加班类型分别统计
                int ot = a.getOvertimeType() != null ? a.getOvertimeType() : 1;
                if (ot == 2) {
                    restdayOvertimeHours += h;
                } else if (ot == 3) {
                    holidayOvertimeHours += h;
                } else {
                    weekdayOvertimeHours += h;
                }
                // 加班工时按费率折算为等价工时
                double rate = (ot == 2) ? restdayRate : (ot == 3) ? holidayRate : weekdayRate;
                overtimeEquivHours += h * rate;
            } else if (a.getAttendanceType() == 3) {
                leaveHours += h;
            } else if (a.getAttendanceType() == 4) {
                absentDays++;
            } else if (a.getAttendanceType() == 5) {
                lateHours += h;
            } else if (a.getAttendanceType() == 6) {
                absentHours += h;
            }
        }
        
        // 出勤天数 = 出勤工时÷每日工时 + 工作日加班工时×费率÷每日工时 + 休息日加班工时×费率÷每日工时 + 节假日加班工时×费率÷每日工时
        double normalDays = standardHours > 0
            ? (normalHours
               + weekdayOvertimeHours * weekdayRate
               + restdayOvertimeHours * restdayRate
               + holidayOvertimeHours * holidayRate)
              / standardHours
            : 0;
        // 加班折算天数 = 加班折算工时 / 每日标准工时
        double overtimeEquivDays = standardHours > 0 ? overtimeEquivHours / standardHours : 0;
        // 实际累计天数 = 出勤天数 + 加班折算天数
        double totalDays = normalDays + overtimeEquivDays;
        
        // 当月出勤天数（含加班折算）
        result.put("normalDays",        Math.round(normalDays * 10.0) / 10.0);
        result.put("normalHours",       Math.round(normalHours * 10.0) / 10.0);
        // 分类型加班工时
        result.put("weekdayOvertimeHours",  Math.round(weekdayOvertimeHours * 10.0) / 10.0);
        result.put("restdayOvertimeHours",  Math.round(restdayOvertimeHours * 10.0) / 10.0);
        result.put("holidayOvertimeHours",  Math.round(holidayOvertimeHours * 10.0) / 10.0);
        result.put("overtimeEquivDays", Math.round(overtimeEquivDays * 10.0) / 10.0);
        result.put("totalDays",         Math.round(totalDays * 10.0) / 10.0);
        result.put("leaveHours",        Math.round(leaveHours * 10.0) / 10.0);
        result.put("lateHours",         Math.round(lateHours * 10.0) / 10.0);
        result.put("absentHours",       Math.round(absentHours * 10.0) / 10.0);
        result.put("absentDays",        absentDays);
        result.put("standardHours",     standardHours);
        result.put("weekdayRate",        weekdayRate);
        result.put("totalRecords",       list.size());
        return result;
    }

    // ==================== 月度考勤表状态管理接口 ====================
    
    /**
     * 查询月度考勤表状态
     */
    @GetMapping("/month/status")
    @ResponseBody
    public Map<String, Object> getMonthStatus(@RequestParam String yearMonth,
                                               @RequestParam(required = false) Long projectId) {
        Map<String, Object> result = new HashMap<>();
        try {
            AttendanceMonthStatus status = monthStatusMapper.findByYearMonthAndProject(yearMonth, projectId);
            
            if (status == null) {
                // 默认返回草稿状态
                result.put("status", AttendanceMonthStatus.STATUS_DRAFT);
                result.put("statusText", "草稿");
                result.put("statusBadgeClass", "bg-secondary");
                result.put("exists", false);
                // 返回 boss 角色员工名字（审批人）
                try {
                    String approverName = jdbcTemplate.queryForObject(
                        "SELECT e.name FROM tb_employee e JOIN tb_role r ON e.role_id = r.id WHERE LOWER(r.role_code) = 'boss' AND e.status = 1 LIMIT 1",
                        String.class);
                    result.put("approverName", approverName);
                } catch (Exception ignore) { result.put("approverName", "BOSS"); }
            } else {
                result.put("status", status.getStatus());
                result.put("statusText", status.getStatusText());
                result.put("statusBadgeClass", status.getStatusBadgeClass());
                result.put("exists", true);
                result.put("submitBy", status.getSubmitBy());
                result.put("submitTime", status.getSubmitTime());
                result.put("submitRemark", status.getSubmitRemark());
                result.put("approveBy", status.getApproveBy());
                result.put("approveTime", status.getApproveTime());
                result.put("approveRemark", status.getApproveRemark());
                result.put("changeApplyBy", status.getChangeApplyBy());
                result.put("changeApplyTime", status.getChangeApplyTime());
                result.put("changeApplyRemark", status.getChangeApplyRemark());
                // 返回 boss 角色员工名字（审批人）
                try {
                    String approverName = jdbcTemplate.queryForObject(
                        "SELECT e.name FROM tb_employee e JOIN tb_role r ON e.role_id = r.id WHERE LOWER(r.role_code) = 'boss' AND e.status = 1 LIMIT 1",
                        String.class);
                    result.put("approverName", approverName);
                } catch (Exception ignore) { result.put("approverName", "BOSS"); }
            }
        } catch (Exception e) {
            // 数据库表不存在或其他错误时，默认返回草稿状态
            System.out.println("查询月度状态失败: " + e.getMessage());
            result.put("status", AttendanceMonthStatus.STATUS_DRAFT);
            result.put("statusText", "草稿");
            result.put("statusBadgeClass", "bg-secondary");
            result.put("exists", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
    
    /**
     * 提交月度考勤审批
     */
    @PostMapping("/month/submit")
    @ResponseBody
    public String submitMonthAudit(@RequestParam String yearMonth,
                                   @RequestParam(required = false) Long projectId,
                                   @RequestParam(required = false) String remark,
                                   HttpSession session) {
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                return "请先登录";
            }
            
            // 检查当前状态
            AttendanceMonthStatus existing = monthStatusMapper.findByYearMonthAndProject(yearMonth, projectId);
            if (existing != null) {
                int currentStatus = existing.getStatus();
                // 只有草稿或已驳回状态可以提交
                if (currentStatus != AttendanceMonthStatus.STATUS_DRAFT 
                    && currentStatus != AttendanceMonthStatus.STATUS_REJECTED) {
                    return "当前状态不允许提交审批";
                }
            }
            
            AttendanceMonthStatus status = new AttendanceMonthStatus();
            status.setYearMonth(yearMonth);
            status.setProjectId(projectId);
            status.setStatus(AttendanceMonthStatus.STATUS_PENDING);
            status.setSubmitBy(currentUser.getId());
            status.setSubmitTime(LocalDateTime.now());
            status.setSubmitRemark(remark);
            
            monthStatusMapper.saveOrUpdate(status);
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
    
    /**
     * 审批月度考勤（通过/驳回）
     */
    @PostMapping("/month/approve")
    @ResponseBody
    public String approveMonth(@RequestParam String yearMonth,
                               @RequestParam(required = false) Long projectId,
                               @RequestParam boolean approved,
                               @RequestParam(required = false) String remark,
                               HttpSession session) {
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                return "请先登录";
            }
            
            // BOSS 或 root 可以审批（使用 isBoss() 方法忽略大小写）
            if (!currentUser.isBoss() && !"root".equalsIgnoreCase(currentUser.getRoleCode())) {
                return "只有BOSS或超级管理员可以审批考勤";
            }
            
            // 检查当前状态
            AttendanceMonthStatus existing = monthStatusMapper.findByYearMonthAndProject(yearMonth, projectId);
            if (existing == null) {
                return "未找到该月份的考勤记录";
            }
            
            int currentStatus = existing.getStatus();
            // 只有待审批状态可以审批
            if (currentStatus != AttendanceMonthStatus.STATUS_PENDING) {
                return "当前状态不允许审批";
            }
            
            // 同意→已审批锁定；驳回→已驳回
            int newStatus = approved
                ? AttendanceMonthStatus.STATUS_APPROVED
                : AttendanceMonthStatus.STATUS_REJECTED;
            
            AttendanceMonthStatus status = new AttendanceMonthStatus();
            status.setYearMonth(yearMonth);
            status.setProjectId(projectId);
            status.setStatus(newStatus);
            status.setApproveBy(currentUser.getId());
            status.setApproveTime(LocalDateTime.now());
            status.setApproveRemark(remark);
            
            monthStatusMapper.approve(status);

            if (projectId != null) {
                if (approved) {
                    // 审批通过后，自动批量创建工资条（已存在的跳过）
                    salarySlipService.batchCreateForProject(projectId, yearMonth);
                } else {
                    // 驳回时，删除对应的工资条
                    salarySlipService.deleteByProjectAndPeriod(projectId, yearMonth);
                }
            }

            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
    
    /**
     * 检查考勤表是否已锁定（用于前端判断是否可以编辑）
     */
    @GetMapping("/month/is-locked")
    @ResponseBody
    public Map<String, Object> isMonthLocked(@RequestParam String yearMonth,
                                              @RequestParam(required = false) Long projectId) {
        Map<String, Object> result = new HashMap<>();
        AttendanceMonthStatus status = monthStatusMapper.findByYearMonthAndProject(yearMonth, projectId);
        
        boolean isLocked = status != null && (status.getStatus() == AttendanceMonthStatus.STATUS_APPROVED
                || status.getStatus() == AttendanceMonthStatus.STATUS_PENDING);
        result.put("locked", isLocked);
        result.put("status", status != null ? status.getStatus() : AttendanceMonthStatus.STATUS_DRAFT);
        result.put("statusText", status != null ? status.getStatusText() : "草稿");
        
        return result;
    }
}
