package com.example.hello.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 员工工资条实体（工资条本身无状态，由月份状态表控制）
 */
public class SalarySlip {

    /** 主键ID */
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 项目ID（标识工资条属于哪个项目） */
    private Long projectId;

    /** 员工姓名（关联查询） */
    private String employeeName;

    /** 身份证号 */
    private String idCard;

    /** 工种名称 */
    private String jobCategoryName;

    /** 工资月份（格式：yyyy-MM） */
    private String salaryPeriod;

    /** 薪资类型：1-日薪，2-月薪 */
    private Integer salaryType;

    /** 基础薪资（日薪或月薪金额） */
    private BigDecimal baseSalary;

    /** 出勤天数 */
    private BigDecimal attendanceDays;

    /** 汇总工资（日薪*天数 或 月薪） */
    private BigDecimal baseAmount;

    /** 费用+合计 */
    private BigDecimal additionAmount;

    /** 费用-合计 */
    private BigDecimal deductionAmount;

    /** 应付工资 = baseAmount + additionAmount - deductionAmount */
    private BigDecimal payableAmount;

    /** 备注 */
    private String remark;

    /** 审批备注（驳回原因） */
    private String approveRemark;

    /** 创建人ID */
    private Long createdBy;

    /** 审批人ID */
    private Long approvedBy;

    /** 审批时间 */
    private LocalDateTime approvedTime;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;

    /** 费用项列表（非数据库字段） */
    private List<SalaryItem> items;

    /** 出勤工时（非数据库字段，用于前端展示计算过程） */
    private BigDecimal attendanceHours;
    /** 工作日加班工时（非数据库字段） */
    private BigDecimal weekdayOTHours;
    /** 休息日加班工时（非数据库字段） */
    private BigDecimal restdayOTHours;
    /** 节假日加班工时（非数据库字段） */
    private BigDecimal holidayOTHours;
    /** 每日工时配置（非数据库字段） */
    private BigDecimal dailyWorkHours;
    /** 工作日加班费率（非数据库字段） */
    private BigDecimal weekdayOTRate;
    /** 休息日加班费率（非数据库字段） */
    private BigDecimal restdayOTRate;
    /** 节假日加班费率（非数据库字段） */
    private BigDecimal holidayOTRate;

    // ===== 业务方法 =====

    public String getSalaryTypeText() {
        if (salaryType == null) return "-";
        return salaryType == 1 ? "日薪" : "月薪";
    }

    /** 计算应付工资 */
    public void calculatePayable() {
        BigDecimal base = baseAmount != null ? baseAmount : BigDecimal.ZERO;
        BigDecimal add  = additionAmount  != null ? additionAmount  : BigDecimal.ZERO;
        BigDecimal ded  = deductionAmount != null ? deductionAmount : BigDecimal.ZERO;
        this.payableAmount = base.add(add).subtract(ded);
    }

    // ===== Getters and Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }

    public String getJobCategoryName() { return jobCategoryName; }
    public void setJobCategoryName(String jobCategoryName) { this.jobCategoryName = jobCategoryName; }

    public String getSalaryPeriod() { return salaryPeriod; }
    public void setSalaryPeriod(String salaryPeriod) { this.salaryPeriod = salaryPeriod; }

    public Integer getSalaryType() { return salaryType; }
    public void setSalaryType(Integer salaryType) { this.salaryType = salaryType; }

    public BigDecimal getBaseSalary() { return baseSalary; }
    public void setBaseSalary(BigDecimal baseSalary) { this.baseSalary = baseSalary; }

    public BigDecimal getAttendanceDays() { return attendanceDays; }
    public void setAttendanceDays(BigDecimal attendanceDays) { this.attendanceDays = attendanceDays; }

    public BigDecimal getBaseAmount() { return baseAmount; }
    public void setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; }

    public BigDecimal getAdditionAmount() { return additionAmount; }
    public void setAdditionAmount(BigDecimal additionAmount) { this.additionAmount = additionAmount; }

    public BigDecimal getDeductionAmount() { return deductionAmount; }
    public void setDeductionAmount(BigDecimal deductionAmount) { this.deductionAmount = deductionAmount; }

    public BigDecimal getPayableAmount() { return payableAmount; }
    public void setPayableAmount(BigDecimal payableAmount) { this.payableAmount = payableAmount; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getApproveRemark() { return approveRemark; }
    public void setApproveRemark(String approveRemark) { this.approveRemark = approveRemark; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedTime() { return approvedTime; }
    public void setApprovedTime(LocalDateTime approvedTime) { this.approvedTime = approvedTime; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }

    public List<SalaryItem> getItems() { return items; }
    public void setItems(List<SalaryItem> items) { this.items = items; }

    public BigDecimal getAttendanceHours() { return attendanceHours; }
    public void setAttendanceHours(BigDecimal attendanceHours) { this.attendanceHours = attendanceHours; }

    public BigDecimal getWeekdayOTHours() { return weekdayOTHours; }
    public void setWeekdayOTHours(BigDecimal weekdayOTHours) { this.weekdayOTHours = weekdayOTHours; }

    public BigDecimal getRestdayOTHours() { return restdayOTHours; }
    public void setRestdayOTHours(BigDecimal restdayOTHours) { this.restdayOTHours = restdayOTHours; }

    public BigDecimal getHolidayOTHours() { return holidayOTHours; }
    public void setHolidayOTHours(BigDecimal holidayOTHours) { this.holidayOTHours = holidayOTHours; }

    public BigDecimal getDailyWorkHours() { return dailyWorkHours; }
    public void setDailyWorkHours(BigDecimal dailyWorkHours) { this.dailyWorkHours = dailyWorkHours; }

    public BigDecimal getWeekdayOTRate() { return weekdayOTRate; }
    public void setWeekdayOTRate(BigDecimal weekdayOTRate) { this.weekdayOTRate = weekdayOTRate; }

    public BigDecimal getRestdayOTRate() { return restdayOTRate; }
    public void setRestdayOTRate(BigDecimal restdayOTRate) { this.restdayOTRate = restdayOTRate; }

    public BigDecimal getHolidayOTRate() { return holidayOTRate; }
    public void setHolidayOTRate(BigDecimal holidayOTRate) { this.holidayOTRate = holidayOTRate; }
}
