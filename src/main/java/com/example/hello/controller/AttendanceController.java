package com.example.hello.controller;

import com.example.hello.entity.Attendance;
import com.example.hello.entity.AttendanceChangeLog;
import com.example.hello.entity.AttendanceMonthStatus;
import com.example.hello.entity.Employee;
import com.example.hello.entity.Project;
import com.example.hello.entity.WorkHourConfig;
import com.example.hello.mapper.AttendanceMapper;
import com.example.hello.mapper.AttendanceMonthStatusMapper;
import com.example.hello.mapper.AttendanceChangeLogMapper;
import com.example.hello.service.AttendanceService;
import com.example.hello.service.EmployeeService;
import com.example.hello.service.ProjectService;
import com.example.hello.service.SalarySlipService;
import com.example.hello.service.WorkHourConfigService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 考勤管理控制器
 */
@Controller
@RequestMapping("/attendance")
public class AttendanceController {
    
    private static final Logger log = LoggerFactory.getLogger(AttendanceController.class);
    
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
    @Autowired
    private AttendanceMapper attendanceMapper;
    @Autowired
    private AttendanceChangeLogMapper attendanceChangeLogMapper;
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
            // 处理空 projectId，统一为 null
            if (projectId != null && projectId == 0) {
                projectId = null;
            }
            
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
                int statusCode = status.getStatus();
                result.put("status", statusCode);
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
                
                // 查询变更日志情况
                int pendingChangeLogCount = 0;
                int allChangeLogCount = 0;
                try {
                    pendingChangeLogCount = attendanceChangeLogMapper.countPendingByProjectAndMonth(projectId, yearMonth);
                    allChangeLogCount = attendanceChangeLogMapper.countByProjectAndMonth(projectId, yearMonth);
                    result.put("hasChangeLogs", pendingChangeLogCount > 0);
                    result.put("hasAnyChangeLogs", allChangeLogCount > 0);
                } catch (Exception ignore) {
                    result.put("hasChangeLogs", false);
                    result.put("hasAnyChangeLogs", false);
                }
                
                // 根据状态和变更日志历史生成文案
                String statusText;
                if (statusCode == AttendanceMonthStatus.STATUS_APPROVED) {
                    // 已锁定状态，从备注中判断是否有过变更历史
                    String approveRemark = status.getApproveRemark();
                    if (approveRemark != null && (approveRemark.contains("[二次审批驳回") || approveRemark.contains("变更未生效"))) {
                        statusText = "已锁定，变更数据未生效";
                    } else if (approveRemark != null && (approveRemark.contains("[二次审批通过") || approveRemark.contains("变更已生效"))) {
                        statusText = "已锁定，变更数据已生效";
                    } else {
                        statusText = "已锁定";
                    }
                } else if (statusCode == AttendanceMonthStatus.STATUS_REJECTED) {
                    statusText = "已驳回";
                } else {
                    statusText = status.getStatusText();
                }
                result.put("statusText", statusText);
                
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
            
            // 处理空 projectId，统一为 null
            if (projectId != null && projectId == 0) {
                projectId = null;
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
            
            // BOSS 角色提交时直接审批通过
            boolean isBoss = currentUser.isBoss() || "root".equalsIgnoreCase(currentUser.getRoleCode());
            
            AttendanceMonthStatus status = new AttendanceMonthStatus();
            status.setYearMonth(yearMonth);
            status.setProjectId(projectId);
            if (isBoss) {
                // BOSS 直接审批通过
                status.setStatus(AttendanceMonthStatus.STATUS_APPROVED);
                status.setApproveBy(currentUser.getId());
                status.setApproveTime(LocalDateTime.now());
            } else {
                // 其他人提交待审批
                status.setStatus(AttendanceMonthStatus.STATUS_PENDING);
            }
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
            
            // 处理空 projectId，统一为 null
            if (projectId != null && projectId == 0) {
                projectId = null;
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
            // 只有待审批状态(1)或变更待审状态(2)可以审批
            if (currentStatus != AttendanceMonthStatus.STATUS_PENDING && currentStatus != AttendanceMonthStatus.STATUS_CHANGE_PENDING) {
                return "当前状态不允许审批";
            }
            
            int newStatus;
            boolean isSecondReject = false; // 标记是否是二次审批驳回
            boolean isSecondApprove = false; // 标记是否是二次审批通过
            if (approved) {
                // 同意→已审批锁定
                newStatus = AttendanceMonthStatus.STATUS_APPROVED;
                // 检查是否有变更日志记录，有则认为是二次编辑通过
                int changeLogCount = attendanceChangeLogMapper.countPendingByProjectAndMonth(projectId, yearMonth);
                if (changeLogCount > 0) {
                    isSecondApprove = true;
                    // 同时应用变更并清理日志
                    attendanceService.batchApproveChanges(projectId, yearMonth, currentUser.getId(), currentUser.getName(), remark);
                }
            } else {
                // 驳回：需要判断是首次提交还是二次编辑
                // 查询是否有变更日志记录，有则认为是二次编辑，恢复到已审批状态
                int changeLogCount = attendanceChangeLogMapper.countPendingByProjectAndMonth(projectId, yearMonth);
                if (changeLogCount > 0) {
                    // 二次编辑驳回：恢复到已审批锁定状态
                    newStatus = AttendanceMonthStatus.STATUS_APPROVED;
                    isSecondReject = true;
                    // 同时清理变更日志
                    attendanceService.batchRejectChanges(projectId, yearMonth, currentUser.getId(), currentUser.getName(), remark);
                } else {
                    // 首次提交驳回：变为已驳回状态
                    newStatus = AttendanceMonthStatus.STATUS_REJECTED;
                }
            }
            
            AttendanceMonthStatus status = new AttendanceMonthStatus();
            status.setYearMonth(yearMonth);
            status.setProjectId(projectId);
            status.setStatus(newStatus);
            status.setApproveBy(currentUser.getId());
            status.setApproveTime(LocalDateTime.now());
            // 二次审批时，在备注中标记
            if (isSecondReject) {
                status.setApproveRemark("[二次审批驳回，变更未生效] " + (remark != null ? remark : ""));
            } else if (isSecondApprove) {
                status.setApproveRemark("[二次审批通过，变更已生效] " + (remark != null ? remark : ""));
            } else {
                status.setApproveRemark(remark);
            }
            
            monthStatusMapper.approve(status);

            // 考勤审批通过（首次或二次编辑）时，自动创建或更新工资条
            if (approved && projectId != null) {
                log.info("[Attendance] 考勤审批通过，自动同步工资条: projectId={}, yearMonth={}", projectId, yearMonth);
                salarySlipService.batchCreateForProject(projectId, yearMonth);
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
    
    /**
     * 下载考勤模板Excel
     * 文件名格式：项目名称-2025-03-考勤表.xlsx
     * 格式：班次单独一列，上午、下午、加班上下排列
     */
    @GetMapping("/template/download")
    public void downloadTemplate(@RequestParam Long projectId,
                                  @RequestParam String yearMonth,
                                  HttpServletResponse response,
                                  HttpSession session) {
        try {
            // 获取项目信息
            Project project = projectService.findById(projectId);
            if (project == null) {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write("项目不存在");
                return;
            }
            
            // 获取项目下的员工列表
            List<Employee> employees = employeeService.findByProjectId(projectId);
            
            // 解析年月
            String[] ym = yearMonth.split("-");
            int year = Integer.parseInt(ym[0]);
            int month = Integer.parseInt(ym[1]);
            YearMonth yearMonthObj = YearMonth.of(year, month);
            int daysInMonth = yearMonthObj.lengthOfMonth();
            
            // 创建Excel工作簿
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("考勤表");
                
                // 创建样式（无背景色）
                CellStyle headerStyle = workbook.createCellStyle();
                headerStyle.setAlignment(HorizontalAlignment.CENTER);
                headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                headerStyle.setBorderBottom(BorderStyle.THIN);
                headerStyle.setBorderTop(BorderStyle.THIN);
                headerStyle.setBorderLeft(BorderStyle.THIN);
                headerStyle.setBorderRight(BorderStyle.THIN);
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                
                CellStyle centerStyle = workbook.createCellStyle();
                centerStyle.setAlignment(HorizontalAlignment.CENTER);
                centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                centerStyle.setBorderBottom(BorderStyle.THIN);
                centerStyle.setBorderTop(BorderStyle.THIN);
                centerStyle.setBorderLeft(BorderStyle.THIN);
                centerStyle.setBorderRight(BorderStyle.THIN);
                
                // 计算总列数：序号 + 姓名 + 工种 + 班次 + 每日1列
                int totalCols = 4 + daysInMonth;
                
                // 创建标题行
                Row titleRow = sheet.createRow(0);
                Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue(project.getName() + " " + yearMonth + " 考勤表");
                CellStyle titleStyle = workbook.createCellStyle();
                titleStyle.setAlignment(HorizontalAlignment.CENTER);
                Font titleFont = workbook.createFont();
                titleFont.setBold(true);
                titleFont.setFontHeightInPoints((short) 14);
                titleStyle.setFont(titleFont);
                titleCell.setCellStyle(titleStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, totalCols - 1));
                
                // 创建表头行（第2行）
                Row headerRow = sheet.createRow(1);
                String[] headers = {"序号", "员工姓名", "工种", "班次"};
                int colIndex = 0;
                for (String header : headers) {
                    Cell cell = headerRow.createCell(colIndex);
                    cell.setCellValue(header);
                    cell.setCellStyle(headerStyle);
                    colIndex++;
                }
                
                // 添加日期列表
                for (int day = 1; day <= daysInMonth; day++) {
                    Cell cell = headerRow.createCell(colIndex);
                    cell.setCellValue(day + "日");
                    cell.setCellStyle(headerStyle);
                    colIndex++;
                }
                
                // 填充员工数据（每个员工占3行：上午、下午、加班）
                String[] shifts = {"上午", "下午", "加班"};
                int rowIndex = 2;
                int empIndex = 1;
                
                for (Employee emp : employees) {
                    int startRow = rowIndex;
                    
                    for (int s = 0; s < 3; s++) {
                        Row row = sheet.createRow(rowIndex);
                        
                        // 序号（只在第一行显示，合并3行）
                        if (s == 0) {
                            Cell noCell = row.createCell(0);
                            noCell.setCellValue(empIndex);
                            noCell.setCellStyle(centerStyle);
                        } else {
                            row.createCell(0).setCellStyle(centerStyle);
                        }
                        
                        // 员工姓名（只在第一行显示，合并3行）
                        if (s == 0) {
                            Cell nameCell = row.createCell(1);
                            nameCell.setCellValue(emp.getName());
                            nameCell.setCellStyle(centerStyle);
                        } else {
                            row.createCell(1).setCellStyle(centerStyle);
                        }
                        
                        // 工种（只在第一行显示，合并3行）
                        if (s == 0) {
                            Cell jobCell = row.createCell(2);
                            jobCell.setCellValue(emp.getJobCategoryName() != null ? emp.getJobCategoryName() : "");
                            jobCell.setCellStyle(centerStyle);
                        } else {
                            row.createCell(2).setCellStyle(centerStyle);
                        }
                        
                        // 班次
                        Cell shiftCell = row.createCell(3);
                        shiftCell.setCellValue(shifts[s]);
                        shiftCell.setCellStyle(centerStyle);
                        
                        // 日期列（空白，供用户填写）
                        for (int day = 1; day <= daysInMonth; day++) {
                            Cell cell = row.createCell(3 + day);
                            cell.setCellStyle(centerStyle);
                        }
                        
                        rowIndex++;
                    }
                    
                    // 合并序号、姓名、工种单元格（3行）
                    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(startRow, startRow + 2, 0, 0));
                    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(startRow, startRow + 2, 1, 1));
                    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(startRow, startRow + 2, 2, 2));
                    
                    empIndex++;
                }
                
                // 设置列宽
                sheet.setColumnWidth(0, 1500);  // 序号
                sheet.setColumnWidth(1, 3000);  // 员工姓名
                sheet.setColumnWidth(2, 2500);  // 工种
                sheet.setColumnWidth(3, 1500);  // 班次
                for (int day = 1; day <= daysInMonth; day++) {
                    sheet.setColumnWidth(3 + day, 1500);  // 日期列
                }
                
                // 设置行高
                titleRow.setHeightInPoints(25);
                headerRow.setHeightInPoints(20);
                
                // 设置响应头
                String fileName = project.getName() + "-" + yearMonth + "-考勤表.xlsx";
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
                
                // 写入响应
                OutputStream out = response.getOutputStream();
                workbook.write(out);
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write("导出失败：" + e.getMessage());
            } catch (IOException ignore) {}
        }
    }
    
    /**
     * 获取星期几简称
     */
    private String getDayOfWeekShort(LocalDate date) {
        String[] days = {"日", "一", "二", "三", "四", "五", "六"};
        return "周" + days[date.getDayOfWeek().getValue() % 7];
    }
    
    /**
     * 导入考勤数据Excel
     * 模板格式：班次单独一列，每个员工占3行（上午、下午、加班）
     * 只覆盖上传了数据的员工，未上传数据的员工保持原有数据不变
     */
    @PostMapping("/import")
    @ResponseBody
    public Map<String, Object> importAttendance(@RequestParam("file") MultipartFile file,
                                                 @RequestParam Long projectId,
                                                 @RequestParam String yearMonth,
                                                 HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            
            // 检查考勤表状态
            AttendanceMonthStatus status = monthStatusMapper.findByYearMonthAndProject(yearMonth, projectId);
            if (status != null && (status.getStatus() == AttendanceMonthStatus.STATUS_APPROVED
                    || status.getStatus() == AttendanceMonthStatus.STATUS_PENDING)) {
                result.put("success", false);
                result.put("message", "考勤表已锁定，无法导入");
                return result;
            }
            
            // 判断是否是变更导入模式
            // 首次导入：状态为null（未创建）、0（草稿）、12（已驳回）
            // 变更导入：状态为2（变更待审）
            boolean isChangeImportMode = (status != null && status.getStatus() == AttendanceMonthStatus.STATUS_CHANGE_PENDING);
            
            // 获取项目下的员工列表
            List<Employee> employees = employeeService.findByProjectId(projectId);
            Map<String, Employee> empMap = new HashMap<>();
            for (Employee emp : employees) {
                empMap.put(emp.getName(), emp);
            }
            
            // 解析年月
            String[] ym = yearMonth.split("-");
            int year = Integer.parseInt(ym[0]);
            int month = Integer.parseInt(ym[1]);
            YearMonth yearMonthObj = YearMonth.of(year, month);
            int daysInMonth = yearMonthObj.lengthOfMonth();
            
            // 解析Excel
            int successCount = 0;
            int failCount = 0;
            List<String> errors = new ArrayList<>();
            
            try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
                Sheet sheet = workbook.getSheetAt(0);
                
                // 从第3行开始读取数据（第1行标题，第2行表头）
                int rowIndex = 2;
                while (rowIndex <= sheet.getLastRowNum()) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        rowIndex++;
                        continue;
                    }
                    
                    // 读取员工姓名
                    Cell nameCell = row.getCell(1);
                    if (nameCell == null) {
                        rowIndex++;
                        continue;
                    }
                    String empName = getCellValue(nameCell).trim();
                    if (empName.isEmpty()) {
                        rowIndex++;
                        continue;
                    }
                    
                    Employee emp = empMap.get(empName);
                    if (emp == null) {
                        errors.add("第" + (rowIndex + 1) + "行：员工 '" + empName + "' 不存在");
                        // 跳过该员工的3行数据
                        rowIndex += 3;
                        failCount++;
                        continue;
                    }
                    
                    // 先收集该员工的所有考勤数据，判断是否有有效数据
                    List<Attendance> attendanceList = new ArrayList<>();
                    // 使用Map按日期合并上午和下午的出勤记录
                    Map<String, Attendance> workAttendanceMap = new HashMap<>();
                    boolean hasValidData = false;
                    
                    // 读取该员工的3行数据（上午、下午、加班）
                    for (int shiftIdx = 0; shiftIdx < 3; shiftIdx++) {
                        Row shiftRow = sheet.getRow(rowIndex + shiftIdx);
                        if (shiftRow == null) continue;
                        
                        // 读取班次
                        Cell shiftCell = shiftRow.getCell(3);
                        String shiftName = shiftCell != null ? getCellValue(shiftCell).trim() : "";
                        
                        // 遍历每一天的考勤数据
                        for (int day = 1; day <= daysInMonth; day++) {
                            LocalDate date = LocalDate.of(year, month, day);
                            
                            // 日期数据从第4列开始（索引4）
                            int dataCol = 3 + day;
                            
                            Cell dataCell = shiftRow.getCell(dataCol);
                            if (dataCell == null) continue;
                            
                            String val = getCellValue(dataCell).trim();
                            if (val.isEmpty()) continue;
                            
                            double hours = 0;
                            try {
                                hours = Double.parseDouble(val);
                            } catch (NumberFormatException e) {
                                continue;
                            }
                            
                            if (hours <= 0) continue;
                            
                            hasValidData = true;
                            
                            // 根据班次类型处理
                            if ("上午".equals(shiftName) || "下午".equals(shiftName)) {
                                // 出勤记录：按日期合并上午和下午的工时
                                String key = date.toString();
                                Attendance att = workAttendanceMap.get(key);
                                if (att == null) {
                                    att = new Attendance();
                                    att.setEmployeeId(emp.getId());
                                    att.setProjectId(projectId);
                                    att.setWorkDate(date);
                                    att.setAttendanceType(1); // 正常出勤
                                    att.setWorkHours(BigDecimal.valueOf(hours));
                                    att.setCreatedBy(currentUser.getId());
                                    workAttendanceMap.put(key, att);
                                } else {
                                    // 累加工时（上午+下午）
                                    att.setWorkHours(att.getWorkHours().add(BigDecimal.valueOf(hours)));
                                }
                            } else if ("加班".equals(shiftName)) {
                                // 加班记录
                                Attendance att = new Attendance();
                                att.setEmployeeId(emp.getId());
                                att.setProjectId(projectId);
                                att.setWorkDate(date);
                                att.setAttendanceType(2); // 加班
                                att.setWorkHours(BigDecimal.valueOf(hours));
                                // 判断加班类型：工作日=1, 休息日=2
                                int dayOfWeek = date.getDayOfWeek().getValue();
                                att.setOvertimeType(dayOfWeek <= 5 ? 1 : 2);
                                att.setCreatedBy(currentUser.getId());
                                attendanceList.add(att);
                            }
                        }
                    }
                    
                    // 只有当有有效数据时，才处理数据
                    if (hasValidData) {
                        if (isChangeImportMode) {
                            // 变更导入模式：记录到变更日志，不直接修改出勤表
                            importToChangeLog(workAttendanceMap, attendanceList, emp.getId(), projectId,
                                yearMonth, currentUser.getId(), currentUser.getName());
                        } else {
                            // 首次导入模式：先删除该员工当月所有考勤数据，然后重新插入
                            // 1. 删除该员工当月所有考勤记录
                            LocalDate monthStart = yearMonthObj.atDay(1);
                            LocalDate monthEnd = yearMonthObj.atEndOfMonth();
                            attendanceMapper.deleteByEmployeeProjectAndMonth(emp.getId(), projectId, monthStart, monthEnd);
                            
                            // 2. 插入新的出勤记录（上午+下午合并的）
                            for (Attendance newAtt : workAttendanceMap.values()) {
                                attendanceService.createAttendance(newAtt);
                            }
                            
                            // 3. 插入新的加班记录
                            for (Attendance newAtt : attendanceList) {
                                attendanceService.createAttendance(newAtt);
                            }
                        }
                        successCount++;
                    } else if (isChangeImportMode) {
                        // 【变更导入模式】即使Excel中没有该员工的数据，也要检查是否需要生成删除日志
                        // 先清空该员工的所有变更日志
                        attendanceChangeLogMapper.deleteByEmployeeProjectAndMonth(emp.getId(), projectId, yearMonth);
                        
                        // 查询该员工当月已有的考勤记录
                        List<Attendance> existingAttendances = attendanceService.getAttendancesByEmployeeProjectAndMonth(
                            emp.getId(), projectId, yearMonth);
                        
                        // 为所有现有记录生成删除日志
                        for (Attendance existing : existingAttendances) {
                            createDeleteLog(existing, emp.getId(), projectId, yearMonth, 
                                existing.getWorkDate(), currentUser.getId(), currentUser.getName(), 
                                "批量导入删除（导入数据为空）");
                        }
                        
                        if (!existingAttendances.isEmpty()) {
                            successCount++; // 有删除操作也算处理成功
                        }
                    }
                    
                    // 跳过已处理的3行
                    rowIndex += 3;
                }
            }
            
            result.put("success", true);
            result.put("message", "导入完成：成功 " + successCount + " 条，失败 " + failCount + " 条");
            if (!errors.isEmpty()) {
                result.put("errors", errors);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "导入失败：" + e.getMessage());
        }
        return result;
    }
    
    /**
     * 变更导入模式：将导入的数据记录到变更日志
     * 支持同一天多条记录（出勤+加班）和删除操作
     * 步骤：1. 清空该员工当月所有变更日志 2. 根据差异重新生成日志
     */
    private void importToChangeLog(Map<String, Attendance> workAttendanceMap, List<Attendance> attendanceList,
                                    Long employeeId, Long projectId, String yearMonth,
                                    Long operatorId, String operatorName) {
        // 第一步：删除该员工当月所有变更日志
        attendanceChangeLogMapper.deleteByEmployeeProjectAndMonth(employeeId, projectId, yearMonth);
        
        // 第二步：查询该员工当月已有的考勤记录
        List<Attendance> existingAttendances = attendanceService.getAttendancesByEmployeeProjectAndMonth(
            employeeId, projectId, yearMonth);
        
        // 按日期分组存储现有记录（支持同一天多条记录）
        Map<String, List<Attendance>> existingMap = new HashMap<>();
        for (Attendance att : existingAttendances) {
            String dateKey = att.getWorkDate().toString();
            existingMap.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(att);
        }
        
        // 收集导入的所有记录（出勤+加班）
        Map<String, List<Attendance>> importMap = new HashMap<>();
        for (Attendance att : workAttendanceMap.values()) {
            String dateKey = att.getWorkDate().toString();
            importMap.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(att);
        }
        for (Attendance att : attendanceList) {
            String dateKey = att.getWorkDate().toString();
            importMap.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(att);
        }
        
        // 处理每一天的记录（包括导入的日期和现有的日期）
        Set<String> allDates = new HashSet<>();
        allDates.addAll(importMap.keySet());
        allDates.addAll(existingMap.keySet());
        
        for (String dateKey : allDates) {
            List<Attendance> importedRecords = importMap.getOrDefault(dateKey, new ArrayList<>());
            List<Attendance> existingRecords = existingMap.getOrDefault(dateKey, new ArrayList<>());
            
            // 从现有记录中找日期（如果有导入记录用导入的日期，否则用现有的）
            LocalDate workDate = null;
            if (!importedRecords.isEmpty()) {
                workDate = importedRecords.get(0).getWorkDate();
            } else if (!existingRecords.isEmpty()) {
                workDate = existingRecords.get(0).getWorkDate();
            }
            if (workDate == null) continue;
            
            // 处理导入的每条记录
            for (Attendance newAtt : importedRecords) {
                Attendance matchingExisting = findMatchingRecord(existingRecords, newAtt);
                
                if (matchingExisting != null) {
                    // 已有同类型记录：检查是否有变化
                    boolean hasChange = hasRecordChanged(matchingExisting, newAtt);
                    if (hasChange) {
                        createEditLog(matchingExisting, newAtt, employeeId, projectId, yearMonth, 
                            workDate, operatorId, operatorName, "批量导入变更");
                    }
                    // 从现有记录列表中移除已处理的记录
                    existingRecords.remove(matchingExisting);
                } else {
                    // 新记录：创建ADD变更日志
                    createAddLog(newAtt, employeeId, projectId, yearMonth, workDate, 
                        operatorId, operatorName, "批量导入新增");
                }
            }
            
            // 剩余未被处理的现有记录表示需要删除
            for (Attendance toDelete : existingRecords) {
                createDeleteLog(toDelete, employeeId, projectId, yearMonth, workDate, 
                    operatorId, operatorName, "批量导入删除");
            }
        }
    }
    
    /**
     * 在现有记录列表中查找匹配的记录（按出勤类型匹配）
     */
    private Attendance findMatchingRecord(List<Attendance> existingRecords, Attendance newAtt) {
        for (Attendance existing : existingRecords) {
            if (Objects.equals(existing.getAttendanceType(), newAtt.getAttendanceType())) {
                return existing;
            }
        }
        return null;
    }
    
    /**
     * 检查记录是否有变化
     */
    private boolean hasRecordChanged(Attendance existing, Attendance newAtt) {
        boolean typeChanged = !Objects.equals(existing.getAttendanceType(), newAtt.getAttendanceType());
        
        // 只有加班类型记录才比较 overtimeType
        boolean overtimeTypeChanged = false;
        if (Objects.equals(existing.getAttendanceType(), 2) || Objects.equals(newAtt.getAttendanceType(), 2)) {
            overtimeTypeChanged = !Objects.equals(existing.getOvertimeType(), newAtt.getOvertimeType());
        }
        
        boolean hoursChanged = (existing.getWorkHours() == null && newAtt.getWorkHours() != null)
            || (existing.getWorkHours() != null && newAtt.getWorkHours() == null)
            || (existing.getWorkHours() != null && newAtt.getWorkHours() != null 
                && existing.getWorkHours().compareTo(newAtt.getWorkHours()) != 0);
        
        // 备注比较：将 null 和空字符串视为相同
        String existingRemark = existing.getRemark();
        String newRemark = newAtt.getRemark();
        boolean remarkChanged = !Objects.equals(
            (existingRemark == null || existingRemark.isEmpty()) ? null : existingRemark,
            (newRemark == null || newRemark.isEmpty()) ? null : newRemark
        );
        
        System.out.println("DEBUG hasRecordChanged: 日期=" + existing.getWorkDate() 
            + ", 类型=" + existing.getAttendanceType() + "->" + newAtt.getAttendanceType() + "(" + typeChanged + ")"
            + ", 工时=" + existing.getWorkHours() + "[" + (existing.getWorkHours() != null ? existing.getWorkHours().scale() : "null") + "]"
            + "->" + newAtt.getWorkHours() + "[" + (newAtt.getWorkHours() != null ? newAtt.getWorkHours().scale() : "null") + "]" + "(" + hoursChanged + ")"
            + ", 加班类型=" + existing.getOvertimeType() + "->" + newAtt.getOvertimeType() + "(" + overtimeTypeChanged + ")"
            + ", 备注=[" + existingRemark + "]->[" + newRemark + "]" + "(" + remarkChanged + ")");
        
        return typeChanged || overtimeTypeChanged || hoursChanged || remarkChanged;
    }
    
    /**
     * 创建编辑变更日志
     */
    private void createEditLog(Attendance existing, Attendance newAtt, Long employeeId, Long projectId, 
                               String yearMonth, LocalDate workDate, Long operatorId, String operatorName, 
                               String changeReason) {
        AttendanceChangeLog log = new AttendanceChangeLog();
        log.setAttendanceId(existing.getId());
        log.setProjectId(projectId);
        log.setEmployeeId(employeeId);
        log.setWorkDate(workDate);
        log.setYearMonth(yearMonth);
        log.setChangeType("EDIT");
        log.setOldAttendanceType(existing.getAttendanceType());
        log.setOldOvertimeType(existing.getOvertimeType());
        log.setOldWorkHours(existing.getWorkHours());
        log.setOldRemark(existing.getRemark());
        log.setNewAttendanceType(newAtt.getAttendanceType());
        log.setNewOvertimeType(newAtt.getOvertimeType());
        log.setNewWorkHours(newAtt.getWorkHours());
        log.setNewRemark(newAtt.getRemark());
        log.setChangeReason(changeReason);
        log.setStatus(AttendanceChangeLog.STATUS_PENDING);
        log.setCreatedBy(operatorId);
        log.setCreatedByName(operatorName);
        log.setCreatedTime(LocalDateTime.now());
        attendanceChangeLogMapper.insert(log);
    }
    
    /**
     * 创建新增变更日志
     */
    private void createAddLog(Attendance newAtt, Long employeeId, Long projectId, 
                              String yearMonth, LocalDate workDate, Long operatorId, String operatorName, 
                              String changeReason) {
        AttendanceChangeLog log = new AttendanceChangeLog();
        log.setAttendanceId(null);
        log.setProjectId(projectId);
        log.setEmployeeId(employeeId);
        log.setWorkDate(workDate);
        log.setYearMonth(yearMonth);
        log.setChangeType("ADD");
        log.setOldAttendanceType(null);
        log.setOldOvertimeType(null);
        log.setOldWorkHours(null);
        log.setOldRemark(null);
        log.setNewAttendanceType(newAtt.getAttendanceType());
        log.setNewOvertimeType(newAtt.getOvertimeType());
        log.setNewWorkHours(newAtt.getWorkHours());
        log.setNewRemark(newAtt.getRemark());
        log.setChangeReason(changeReason);
        log.setStatus(AttendanceChangeLog.STATUS_PENDING);
        log.setCreatedBy(operatorId);
        log.setCreatedByName(operatorName);
        log.setCreatedTime(LocalDateTime.now());
        attendanceChangeLogMapper.insert(log);
    }
    
    /**
     * 创建删除变更日志
     */
    private void createDeleteLog(Attendance toDelete, Long employeeId, Long projectId, 
                                 String yearMonth, LocalDate workDate, Long operatorId, String operatorName, 
                                 String changeReason) {
        AttendanceChangeLog log = new AttendanceChangeLog();
        log.setAttendanceId(toDelete.getId());
        log.setProjectId(projectId);
        log.setEmployeeId(employeeId);
        log.setWorkDate(workDate);
        log.setYearMonth(yearMonth);
        log.setChangeType("DELETE");
        log.setOldAttendanceType(toDelete.getAttendanceType());
        log.setOldOvertimeType(toDelete.getOvertimeType());
        log.setOldWorkHours(toDelete.getWorkHours());
        log.setOldRemark(toDelete.getRemark());
        log.setNewAttendanceType(null);
        log.setNewOvertimeType(null);
        log.setNewWorkHours(null);
        log.setNewRemark(null);
        log.setChangeReason(changeReason);
        log.setStatus(AttendanceChangeLog.STATUS_PENDING);
        log.setCreatedBy(operatorId);
        log.setCreatedByName(operatorName);
        log.setCreatedTime(LocalDateTime.now());
        attendanceChangeLogMapper.insert(log);
    }
    
    /**
     * 获取单元格值
     */
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    // ===== 二次编辑与修改记录 API =====
    
    /**
     * 创建考勤修改记录（审批通过后的二次编辑）
     */
    @PostMapping("/api/create-change-log")
    @ResponseBody
    public Map<String, Object> createChangeLog(@RequestParam Long attendanceId,
                                                @RequestParam Integer attendanceType,
                                                @RequestParam(required = false) Integer overtimeType,
                                                @RequestParam BigDecimal workHours,
                                                @RequestParam(required = false) String remark,
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
            
            // 构建新的考勤数据
            Attendance newData = new Attendance();
            newData.setAttendanceType(attendanceType);
            newData.setOvertimeType(overtimeType);
            newData.setWorkHours(workHours);
            newData.setRemark(remark);
            
            var log = attendanceService.createChangeLog(
                    attendanceId, newData, changeReason, currentUser.getId(), currentUser.getName());
            
            result.put("success", true);
            result.put("message", "已提交修改申请，等待审批");
            result.put("changeLogId", log.getId());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 查询考勤记录的待审批修改记录
     */
    @GetMapping("/api/pending-change")
    @ResponseBody
    public Map<String, Object> getPendingChange(@RequestParam Long attendanceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            var log = attendanceService.getPendingChangeLog(attendanceId);
            result.put("success", true);
            result.put("hasPending", log != null);
            result.put("changeLog", log);
        } catch (Exception e) {
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
                                              @RequestParam String yearMonth) {
        Map<String, Object> result = new HashMap<>();
        try {
            var logs = attendanceService.getChangeLogsByProjectAndMonth(projectId, yearMonth);
            result.put("success", true);
            result.put("logs", logs);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 审批通过考勤修改
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
            
            // 检查权限（BOSS或root）
            boolean canApprove = currentUser.isBoss() || "root".equalsIgnoreCase(currentUser.getRoleCode());
            if (!canApprove) {
                result.put("success", false);
                result.put("message", "无权审批");
                return result;
            }
            
            attendanceService.approveChangeLog(changeLogId, currentUser.getId(), 
                    currentUser.getName(), remark);
            
            result.put("success", true);
            result.put("message", "审批通过");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 驳回考勤修改
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
            
            // 检查权限（BOSS或root）
            boolean canApprove = currentUser.isBoss() || "root".equalsIgnoreCase(currentUser.getRoleCode());
            if (!canApprove) {
                result.put("success", false);
                result.put("message", "无权审批");
                return result;
            }
            
            attendanceService.rejectChangeLog(changeLogId, currentUser.getId(), 
                    currentUser.getName(), remark);
            
            result.put("success", true);
            result.put("message", "已驳回。本次修改申请已被拒绝，考勤数据已恢复原状，如需变更请重新申请修改。");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 批量审批考勤修改（按项目和月份）
     */
    @PostMapping("/api/batch-approve-changes")
    @ResponseBody
    public Map<String, Object> batchApproveChanges(@RequestParam Long projectId,
                                                    @RequestParam String yearMonth,
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
            
            // 检查权限（BOSS或root）
            boolean canApprove = currentUser.isBoss() || "root".equalsIgnoreCase(currentUser.getRoleCode());
            if (!canApprove) {
                result.put("success", false);
                result.put("message", "无权审批");
                return result;
            }
            
            attendanceService.batchApproveChanges(projectId, yearMonth, 
                    currentUser.getId(), currentUser.getName(), remark);
            
            result.put("success", true);
            result.put("message", "批量审批通过");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    // ===== 全局变更编辑模式 API =====
    
    /**
     * 申请全局变更（进入批量编辑模式）
     * 将月份状态从已审批(5)改为变更待审(2)
     */
    @PostMapping("/api/apply-global-change")
    @ResponseBody
    public Map<String, Object> applyGlobalChange(@RequestParam(required = false) Long projectId,
                                                  @RequestParam String yearMonth,
                                                  @RequestParam String reason,
                                                  HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            
            // 处理空 projectId，统一为 null
            if (projectId != null && projectId == 0) {
                projectId = null;
            }
            
            // 检查当前状态是否为已审批
            AttendanceMonthStatus monthStatus = monthStatusMapper.findByYearMonthAndProject(yearMonth, projectId);
            if (monthStatus == null || monthStatus.getStatus() != AttendanceMonthStatus.STATUS_APPROVED) {
                result.put("success", false);
                result.put("message", "只有已审批的月份才能申请修改");
                return result;
            }
            
            // 更新为变更待审状态
            monthStatus.setStatus(AttendanceMonthStatus.STATUS_CHANGE_PENDING);
            monthStatus.setChangeApplyBy(currentUser.getId());
            monthStatus.setChangeApplyTime(LocalDateTime.now());
            monthStatus.setChangeApplyRemark(reason);
            monthStatusMapper.saveOrUpdate(monthStatus);
            
            result.put("success", true);
            result.put("message", "已进入变更编辑模式");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 提交全局变更
     * 将数据库中待审批的修改记录状态更新，并将月份状态改为待审批(1)
     */
    @PostMapping("/api/submit-global-changes")
    @ResponseBody
    public Map<String, Object> submitGlobalChanges(@RequestParam(required = false) Long projectId,
                                                    @RequestParam String yearMonth,
                                                    HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            
            // 处理空 projectId，统一为 null
            if (projectId != null && projectId == 0) {
                projectId = null;
            }
            
            // 查询待审批的修改记录数量
            int pendingCount = attendanceChangeLogMapper.countPendingByProjectAndMonth(projectId, yearMonth);
            if (pendingCount == 0) {
                result.put("success", false);
                result.put("message", "没有检测到任何修改");
                return result;
            }
            
            // 智能抵消：清理无实际变化的记录
            List<AttendanceChangeLog> pendingLogs = attendanceChangeLogMapper.selectPendingByProjectAndMonth(projectId, yearMonth);
            int cancelledCount = cancelOffsettingChanges(pendingLogs);
            
            // 重新查询有效的修改记录数量
            pendingCount = attendanceChangeLogMapper.countPendingByProjectAndMonth(projectId, yearMonth);
            if (pendingCount == 0) {
                result.put("success", true);
                result.put("message", "所有修改已相互抵消，无需提交审批");
                return result;
            }
            
            // 更新月份状态为待审批
            AttendanceMonthStatus monthStatus = monthStatusMapper.findByYearMonthAndProject(yearMonth, projectId);
            if (monthStatus == null) {
                // 如果不存在则创建新记录
                monthStatus = new AttendanceMonthStatus();
                monthStatus.setYearMonth(yearMonth);
                monthStatus.setProjectId(projectId);
            }
            monthStatus.setStatus(AttendanceMonthStatus.STATUS_PENDING);
            monthStatus.setSubmitBy(currentUser.getId());
            monthStatus.setSubmitTime(LocalDateTime.now());
            monthStatus.setSubmitRemark("变更后重新提交审批");
            monthStatusMapper.saveOrUpdate(monthStatus);
            
            result.put("success", true);
            result.put("message", "已提交 " + pendingCount + " 条变更，等待审批");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 查询待审批的修改记录数量
     */
    @GetMapping("/count-pending-changes")
    @ResponseBody
    public int countPendingChanges(@RequestParam(required = false) Long projectId,
                                    @RequestParam String yearMonth) {
        return attendanceChangeLogMapper.countPendingByProjectAndMonth(projectId, yearMonth);
    }
    
    /**
     * 保存修改记录（编辑模式）
     */
    @PostMapping("/save-change-log")
    @ResponseBody
    public Map<String, Object> saveChangeLog(@RequestBody AttendanceChangeLog log, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            
            System.out.println("saveChangeLog called: attendanceId=" + log.getAttendanceId() + 
                ", projectId=" + log.getProjectId() + ", employeeId=" + log.getEmployeeId() +
                ", workDate=" + log.getWorkDate() + ", yearMonth=" + log.getYearMonth());
            
            // 对于新增操作(ADD)，检查同一天同一员工是否已存在相同类型的出勤记录
            if ("ADD".equals(log.getChangeType()) && log.getAttendanceId() == null) {
                // 参数校验
                if (log.getEmployeeId() == null || log.getWorkDate() == null) {
                    result.put("success", false);
                    result.put("message", "参数错误：员工ID或工作日期不能为空");
                    return result;
                }
                // 查询该员工该日期是否已有相同类型的出勤记录
                try {
                    List<Attendance> existingAttendances = attendanceMapper.selectByEmployeeAndDate(
                        log.getEmployeeId(), log.getWorkDate());
                    if (existingAttendances != null && !existingAttendances.isEmpty()) {
                        // 检查是否已有相同类型的记录
                        Integer newAttendanceType = log.getNewAttendanceType();
                        for (Attendance existing : existingAttendances) {
                            if (Objects.equals(existing.getAttendanceType(), newAttendanceType)) {
                                String typeName = newAttendanceType == 2 ? "加班" : "出勤";
                                result.put("success", false);
                                result.put("message", "该员工在" + log.getWorkDate() + "已存在" + typeName + "记录，请勿重复添加。如需变更，请在日历面板操作修改或删除。");
                                return result;
                            }
                        }
                    }
                } catch (Exception queryEx) {
                    System.out.println("查询出勤记录失败: " + queryEx.getMessage());
                    queryEx.printStackTrace();
                    throw queryEx;
                }
            }
            
            // 检查是否已存在待审批的修改记录，如果存在则更新
            AttendanceChangeLog existingLog = attendanceChangeLogMapper.selectPendingByAttendanceId(log.getAttendanceId());
            
            // 对于ADD操作(attendanceId为null)，检查同一天同一类型是否已有待审批的ADD记录
            if (existingLog == null && "ADD".equals(log.getChangeType()) && log.getAttendanceId() == null) {
                existingLog = attendanceChangeLogMapper.selectPendingAddByEmployeeDateAndType(
                    log.getEmployeeId(), log.getWorkDate(), log.getNewAttendanceType());
                if (existingLog != null) {
                    // 已有ADD记录，改为EDIT（更新新值）
                    existingLog.setNewAttendanceType(log.getNewAttendanceType());
                    existingLog.setNewOvertimeType(log.getNewOvertimeType());
                    existingLog.setNewWorkHours(log.getNewWorkHours());
                    existingLog.setNewRemark(log.getNewRemark());
                    existingLog.setChangeReason(log.getChangeReason());
                    existingLog.setCreatedTime(LocalDateTime.now());
                    attendanceChangeLogMapper.update(existingLog);
                    result.put("success", true);
                    result.put("message", "已更新修改申请");
                    result.put("changeLogId", existingLog.getId());
                    return result;
                }
            }
            
            if (existingLog != null) {
                // 更新现有记录
                existingLog.setOldAttendanceType(log.getOldAttendanceType());
                existingLog.setOldOvertimeType(log.getOldOvertimeType());
                existingLog.setOldWorkHours(log.getOldWorkHours());
                existingLog.setOldRemark(log.getOldRemark());
                existingLog.setNewAttendanceType(log.getNewAttendanceType());
                existingLog.setNewOvertimeType(log.getNewOvertimeType());
                existingLog.setNewWorkHours(log.getNewWorkHours());
                existingLog.setNewRemark(log.getNewRemark());
                existingLog.setChangeType(log.getChangeType());
                existingLog.setChangeReason(log.getChangeReason());
                existingLog.setCreatedTime(LocalDateTime.now());
                attendanceChangeLogMapper.update(existingLog);
            } else {
                // 创建新记录
                log.setStatus(AttendanceChangeLog.STATUS_PENDING);
                log.setCreatedBy(currentUser.getId());
                log.setCreatedByName(currentUser.getName());
                log.setCreatedTime(LocalDateTime.now());
                attendanceChangeLogMapper.insert(log);
            }
            
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                errorMsg = e.getClass().getName();
            }
            result.put("message", "保存失败：" + errorMsg);
            e.printStackTrace();
        }
        return result;
    }
    
    /**
     * 智能抵消：清理无实际变化的记录
     * 场景：
     * 1. ADD + DELETE = 抵消（新增后又删除）
     * 2. DELETE + ADD(相同值) = 抵消（删除后又新增相同值）
     * 3. EDIT + EDIT(恢复原值) = 抵消（修改后又改回原值）
     * 4. ADD + EDIT = 合并为ADD（新增后修改 = 最终新增值）
     * 5. EDIT + DELETE = 保留DELETE（修改后删除）
     * 6. DELETE + ADD(不同值) = 保留ADD（删除后新增不同值）
     */
    private int cancelOffsettingChanges(List<AttendanceChangeLog> logs) {
        int cancelledCount = 0;
        
        // 按员工+日期+考勤类型分组（同一天的不同类型记录分开处理）
        Map<String, List<AttendanceChangeLog>> grouped = logs.stream()
            .collect(Collectors.groupingBy(log -> {
                // 对于DELETE类型，用oldAttendanceType；对于ADD/EDIT类型，用newAttendanceType
                Integer attendanceType = AttendanceChangeLog.TYPE_DELETE.equals(log.getChangeType()) 
                    ? log.getOldAttendanceType() 
                    : log.getNewAttendanceType();
                return log.getEmployeeId() + "_" + log.getWorkDate() + "_" + attendanceType;
            }));
        
        for (List<AttendanceChangeLog> group : grouped.values()) {
            if (group.size() < 2) continue;
            
            // 按时间排序
            group.sort(Comparator.comparing(AttendanceChangeLog::getCreatedTime));
            
            // 场景1: ADD + DELETE = 抵消
            if (group.size() == 2) {
                AttendanceChangeLog first = group.get(0);
                AttendanceChangeLog second = group.get(1);
                
                if (AttendanceChangeLog.TYPE_ADD.equals(first.getChangeType()) && 
                    AttendanceChangeLog.TYPE_DELETE.equals(second.getChangeType())) {
                    // 新增后又删除，互相抵消
                    attendanceChangeLogMapper.deleteById(first.getId());
                    attendanceChangeLogMapper.deleteById(second.getId());
                    cancelledCount += 2;
                    continue;
                }
                
                // 场景2: DELETE + ADD = 检查是否恢复原值
                if (AttendanceChangeLog.TYPE_DELETE.equals(first.getChangeType()) && 
                    AttendanceChangeLog.TYPE_ADD.equals(second.getChangeType())) {
                    // 比较DELETE的旧值和ADD的新值是否相同
                    if (isDeleteAndAddSameValue(first, second)) {
                        // 新增的值与被删除的原值相同，抵消
                        attendanceChangeLogMapper.deleteById(first.getId());
                        attendanceChangeLogMapper.deleteById(second.getId());
                        cancelledCount += 2;
                    }
                    continue;
                }
                
                // 场景3: EDIT + EDIT = 检查是否恢复原值
                if (AttendanceChangeLog.TYPE_EDIT.equals(first.getChangeType()) && 
                    AttendanceChangeLog.TYPE_EDIT.equals(second.getChangeType())) {
                    if (isRestoredToOriginal(first, second)) {
                        // 第二次修改恢复了第一次修改前的值，抵消
                        attendanceChangeLogMapper.deleteById(first.getId());
                        attendanceChangeLogMapper.deleteById(second.getId());
                        cancelledCount += 2;
                    }
                    continue;
                }
            }
            
            // 场景4: ADD + EDIT = 合并为最终的ADD
            if (group.size() == 2) {
                AttendanceChangeLog first = group.get(0);
                AttendanceChangeLog second = group.get(1);
                
                if (AttendanceChangeLog.TYPE_ADD.equals(first.getChangeType()) && 
                    AttendanceChangeLog.TYPE_EDIT.equals(second.getChangeType())) {
                    // 将ADD的值更新为EDIT的最终值，删除EDIT记录
                    first.setNewAttendanceType(second.getNewAttendanceType());
                    first.setNewOvertimeType(second.getNewOvertimeType());
                    first.setNewWorkHours(second.getNewWorkHours());
                    first.setNewRemark(second.getNewRemark());
                    attendanceChangeLogMapper.update(first);
                    attendanceChangeLogMapper.deleteById(second.getId());
                    cancelledCount++;
                    continue;
                }
            }
        }
        
        return cancelledCount;
    }
    
    /**
     * 检查DELETE的旧值和ADD的新值是否相同
     */
    private boolean isDeleteAndAddSameValue(AttendanceChangeLog deleteLog, AttendanceChangeLog addLog) {
        // 备注比较：将 null 和空字符串视为相同
        String deleteRemark = deleteLog.getOldRemark();
        String addRemark = addLog.getNewRemark();
        boolean remarkSame = Objects.equals(
            (deleteRemark == null || deleteRemark.isEmpty()) ? null : deleteRemark,
            (addRemark == null || addRemark.isEmpty()) ? null : addRemark
        );
        
        return Objects.equals(deleteLog.getOldAttendanceType(), addLog.getNewAttendanceType()) &&
               Objects.equals(deleteLog.getOldOvertimeType(), addLog.getNewOvertimeType()) &&
               Objects.equals(deleteLog.getOldWorkHours(), addLog.getNewWorkHours()) &&
               remarkSame;
    }
    
    /**
     * 检查新增的值是否与原考勤记录相同
     */
    private boolean isSameValue(Attendance attendance, AttendanceChangeLog addLog) {
        // 备注比较：将 null 和空字符串视为相同
        String attRemark = attendance.getRemark();
        String logRemark = addLog.getNewRemark();
        boolean remarkSame = Objects.equals(
            (attRemark == null || attRemark.isEmpty()) ? null : attRemark,
            (logRemark == null || logRemark.isEmpty()) ? null : logRemark
        );
        
        return Objects.equals(attendance.getAttendanceType(), addLog.getNewAttendanceType()) &&
               Objects.equals(attendance.getOvertimeType(), addLog.getNewOvertimeType()) &&
               Objects.equals(attendance.getWorkHours(), addLog.getNewWorkHours()) &&
               remarkSame;
    }
    
    /**
     * 检查第二次编辑是否恢复了第一次编辑前的值
     */
    private boolean isRestoredToOriginal(AttendanceChangeLog first, AttendanceChangeLog second) {
        // 备注比较：将 null 和空字符串视为相同
        String firstRemark = first.getOldRemark();
        String secondRemark = second.getNewRemark();
        boolean remarkSame = Objects.equals(
            (firstRemark == null || firstRemark.isEmpty()) ? null : firstRemark,
            (secondRemark == null || secondRemark.isEmpty()) ? null : secondRemark
        );
        
        return Objects.equals(first.getOldAttendanceType(), second.getNewAttendanceType()) &&
               Objects.equals(first.getOldOvertimeType(), second.getNewOvertimeType()) &&
               Objects.equals(first.getOldWorkHours(), second.getNewWorkHours()) &&
               remarkSame;
    }
    
    /**
     * 取消全局变更
     * 恢复为已审批状态(5)，清除所有待审批的修改记录
     */
    @PostMapping("/api/cancel-global-changes")
    @ResponseBody
    public Map<String, Object> cancelGlobalChanges(@RequestParam(required = false) Long projectId,
                                                    @RequestParam String yearMonth,
                                                    HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            
            // 处理空 projectId，统一为 null
            if (projectId != null && projectId == 0) {
                projectId = null;
            }
            
            // 检查当前状态是否为变更待审
            AttendanceMonthStatus monthStatus = monthStatusMapper.findByYearMonthAndProject(yearMonth, projectId);
            if (monthStatus == null || monthStatus.getStatus() != AttendanceMonthStatus.STATUS_CHANGE_PENDING) {
                result.put("success", false);
                result.put("message", "当前不是变更编辑模式");
                return result;
            }
            
            // 删除该月份所有待审批的修改记录，并回退数据
            List<AttendanceChangeLog> pendingLogs = attendanceService.getChangeLogsByProjectAndMonth(projectId, yearMonth);
            for (AttendanceChangeLog log : pendingLogs) {
                if (log.getStatus() == AttendanceChangeLog.STATUS_PENDING) {
                    // 根据变更类型回退数据
                    String changeType = log.getChangeType();
                    Long attendanceId = log.getAttendanceId();
                    
                    if ("ADD".equals(changeType)) {
                        // 新增被取消：删除新增的考勤记录
                        if (attendanceId != null) {
                            attendanceMapper.deleteById(attendanceId);
                        }
                    } else if ("EDIT".equals(changeType)) {
                        // 编辑被取消：恢复到修改前的数据
                        if (attendanceId != null) {
                            Attendance attendance = attendanceMapper.selectById(attendanceId);
                            if (attendance != null) {
                                attendance.setAttendanceType(log.getOldAttendanceType());
                                attendance.setOvertimeType(log.getOldOvertimeType());
                                attendance.setWorkHours(log.getOldWorkHours());
                                attendance.setRemark(log.getOldRemark());
                                attendanceMapper.update(attendance);
                            }
                        }
                    } else if ("DELETE".equals(changeType)) {
                        // 删除被取消：恢复被删除的考勤记录
                        // 只有当 attendanceId 为 null 时才需要恢复（这是新增后又删除的情况）
                        // 如果 attendanceId 不为 null，说明是已有记录被标记删除，原记录还在，不需要恢复
                        if (attendanceId == null) {
                            Attendance attendance = new Attendance();
                            attendance.setEmployeeId(log.getEmployeeId());
                            attendance.setProjectId(log.getProjectId());
                            attendance.setWorkDate(log.getWorkDate());
                            attendance.setAttendanceType(log.getOldAttendanceType());
                            attendance.setOvertimeType(log.getOldOvertimeType());
                            attendance.setWorkHours(log.getOldWorkHours());
                            attendance.setRemark(log.getOldRemark());
                            attendanceMapper.insert(attendance);
                        }
                    }
                    
                    // 删除变更记录
                    attendanceChangeLogMapper.deleteById(log.getId());
                }
            }
            
            // 恢复为已审批状态
            monthStatus.setStatus(AttendanceMonthStatus.STATUS_APPROVED);
            monthStatusMapper.saveOrUpdate(monthStatus);
            
            result.put("success", true);
            result.put("message", "已取消变更，恢复为已审批状态");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 测试接口：对比导入数据和现有数据
     */
    @GetMapping("/import-test")
    @ResponseBody
    public Map<String, Object> testImportComparison(@RequestParam Long employeeId,
                                                     @RequestParam Long projectId,
                                                     @RequestParam String yearMonth) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 查询现有记录
            List<Attendance> existingAttendances = attendanceService.getAttendancesByEmployeeProjectAndMonth(
                employeeId, projectId, yearMonth);
            
            List<Map<String, Object>> comparisonList = new ArrayList<>();
            
            for (Attendance att : existingAttendances) {
                Map<String, Object> record = new HashMap<>();
                record.put("date", att.getWorkDate().toString());
                record.put("id", att.getId());
                record.put("attendanceType", att.getAttendanceType());
                record.put("attendanceTypeClass", att.getAttendanceType() != null ? att.getAttendanceType().getClass().getName() : "null");
                record.put("overtimeType", att.getOvertimeType());
                record.put("overtimeTypeClass", att.getOvertimeType() != null ? att.getOvertimeType().getClass().getName() : "null");
                record.put("workHours", att.getWorkHours() != null ? att.getWorkHours().toString() : "null");
                record.put("workHoursScale", att.getWorkHours() != null ? att.getWorkHours().scale() : "null");
                record.put("workHoursClass", att.getWorkHours() != null ? att.getWorkHours().getClass().getName() : "null");
                record.put("remark", att.getRemark());
                comparisonList.add(record);
            }
            
            result.put("success", true);
            result.put("employeeId", employeeId);
            result.put("projectId", projectId);
            result.put("yearMonth", yearMonth);
            result.put("existingRecords", comparisonList);
            result.put("totalCount", comparisonList.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
    
    // ===== 考勤签字表图片接口 =====
    
    /**
     * 上传考勤签字表图片（月度维度）
     * 每次上传都会覆盖之前的所有图片
     */
    @PostMapping("/sign-image/upload")
    @ResponseBody
    public Map<String, Object> uploadSignImages(@RequestParam String yearMonth,
                                                 @RequestParam(required = false) Long projectId,
                                                 @RequestParam("files") List<MultipartFile> files,
                                                 HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            
            if (files == null || files.isEmpty()) {
                result.put("success", false);
                result.put("message", "请选择要上传的图片");
                return result;
            }
            
            // 处理空 projectId
            if (projectId != null && projectId == 0) {
                projectId = null;
            }
            
            // 保存新上传的图片
            List<String> imageUrls = new ArrayList<>();
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                if (file.isEmpty()) continue;
                
                // 保存文件到本地
                String originalName = file.getOriginalFilename();
                String ext = "";
                if (originalName != null && originalName.lastIndexOf(".") > 0) {
                    ext = originalName.substring(originalName.lastIndexOf("."));
                }
                String fileName = System.currentTimeMillis() + "_" + i + ext;
                String uploadDir = System.getProperty("user.dir") + "/uploads/sign-images/";
                java.io.File dir = new java.io.File(uploadDir);
                if (!dir.exists()) dir.mkdirs();
                
                String filePath = uploadDir + fileName;
                file.transferTo(new java.io.File(filePath));
                
                imageUrls.add("/uploads/sign-images/" + fileName);
            }
            
            // 更新月度考勤表的签字表URL（覆盖之前的）
            String signImageUrls = String.join(",", imageUrls);
            monthStatusMapper.updateSignImageUrls(yearMonth, projectId, signImageUrls);
            
            result.put("success", true);
            result.put("message", "上传成功" + imageUrls.size() + "张图片");
            result.put("count", imageUrls.size());
            result.put("images", imageUrls);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "上传失败：" + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
    
    /**
     * 获取考勤签字表图片列表（月度维度）
     */
    @GetMapping("/sign-image/list")
    @ResponseBody
    public Map<String, Object> listSignImages(@RequestParam String yearMonth,
                                               @RequestParam(required = false) Long projectId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 处理空 projectId
            if (projectId != null && projectId == 0) {
                projectId = null;
            }
            
            AttendanceMonthStatus status = monthStatusMapper.findByYearMonthAndProject(yearMonth, projectId);
            List<String> imageUrls = new ArrayList<>();
            
            if (status != null && status.getSignImageUrls() != null && !status.getSignImageUrls().isEmpty()) {
                String[] urls = status.getSignImageUrls().split(",");
                for (String url : urls) {
                    if (!url.trim().isEmpty()) {
                        imageUrls.add(url.trim());
                    }
                }
            }
            
            result.put("success", true);
            result.put("images", imageUrls);
            result.put("count", imageUrls.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询失败：" + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
}
