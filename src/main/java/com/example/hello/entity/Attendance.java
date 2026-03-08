package com.example.hello.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工考勤记录实体
 */
public class Attendance {
    
    /** 主键ID */
    private Long id;
    
    /** 员工ID */
    private Long employeeId;
    
    /** 员工姓名（非数据库字段，用于展示） */
    private String employeeName;
    
    /** 项目ID */
    private Long projectId;
    
    /** 项目名称（非数据库字段，用于展示） */
    private String projectName;
    
    /** 工作日期 */
    private LocalDate workDate;
    
    /** 出勤类型：1-出勤，2-加班，3-请假，4-缺勤，5-迟到，6-旷工 */
    private Integer attendanceType;
    
    /** 加班类型：1-工作日加班，2-休息日加班，3-法定假期加班 */
    private Integer overtimeType;
    
    /** 工作时长（小时） */
    private BigDecimal workHours;
    
    /** 备注 */
    private String remark;
    
    /** 创建人ID */
    private Long createdBy;
    
    /** 创建人姓名（非数据库字段，用于展示） */
    private String createdByName;
    
    /** 创建时间 */
    private LocalDateTime createdTime;
    
    /** 更新时间 */
    private LocalDateTime updatedTime;
    
    /**
     * 获取出勤类型文本
     */
    public String getAttendanceTypeText() {
        return switch (attendanceType) {
            case 1 -> "出勤";
            case 2 -> "加班" + getOvertimeTypeText();
            case 3 -> "请假";
            case 4 -> "缺勤";
            case 5 -> "迟到";
            case 6 -> "旷工";
            default -> "未知";
        };
    }
    
    /**
     * 获取加班类型文本
     */
    public String getOvertimeTypeText() {
        if (attendanceType != 2 || overtimeType == null) {
            return "";
        }
        return switch (overtimeType) {
            case 1 -> "(工作日)";
            case 2 -> "(休息日)";
            case 3 -> "(法定假期)";
            default -> "";
        };
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    public Integer getAttendanceType() { return attendanceType; }
    public void setAttendanceType(Integer attendanceType) { this.attendanceType = attendanceType; }
    public Integer getOvertimeType() { return overtimeType; }
    public void setOvertimeType(Integer overtimeType) { this.overtimeType = overtimeType; }
    public BigDecimal getWorkHours() { return workHours; }
    public void setWorkHours(BigDecimal workHours) { this.workHours = workHours; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
