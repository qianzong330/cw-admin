package com.example.hello.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤修改记录实体
 * 用于记录考勤审批通过后二次修改的变更内容
 */
public class AttendanceChangeLog {
    
    // 状态常量
    public static final int STATUS_PENDING = 0;   // 待审批
    public static final int STATUS_APPROVED = 1;  // 已通过
    public static final int STATUS_REJECTED = 2;  // 已驳回
    
    // 变更类型常量
    public static final String TYPE_ADD = "ADD";      // 新增
    public static final String TYPE_EDIT = "EDIT";    // 编辑
    public static final String TYPE_DELETE = "DELETE"; // 删除
    
    private Long id;
    private Long attendanceId;
    private Long projectId;
    private Long employeeId;
    private LocalDate workDate;
    private String yearMonth;
    private String changeType; // ADD, EDIT, DELETE
    
    // 变更前数据
    private Integer oldAttendanceType;
    private Integer oldOvertimeType;
    private BigDecimal oldWorkHours;
    private String oldRemark;
    
    // 变更后数据
    private Integer newAttendanceType;
    private Integer newOvertimeType;
    private BigDecimal newWorkHours;
    private String newRemark;
    
    // 变更原因
    private String changeReason;
    
    // 审批状态
    private Integer status;
    
    // 操作人信息
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdTime;
    
    // 审批信息
    private Long approvedBy;
    private String approvedByName;
    private LocalDateTime approvedTime;
    private String approveRemark;
    
    // 关联字段（非数据库字段）
    private String employeeName;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getAttendanceId() { return attendanceId; }
    public void setAttendanceId(Long attendanceId) { this.attendanceId = attendanceId; }
    
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    
    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }
    
    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    
    public Integer getOldAttendanceType() { return oldAttendanceType; }
    public void setOldAttendanceType(Integer oldAttendanceType) { this.oldAttendanceType = oldAttendanceType; }
    
    public Integer getOldOvertimeType() { return oldOvertimeType; }
    public void setOldOvertimeType(Integer oldOvertimeType) { this.oldOvertimeType = oldOvertimeType; }
    
    public BigDecimal getOldWorkHours() { return oldWorkHours; }
    public void setOldWorkHours(BigDecimal oldWorkHours) { this.oldWorkHours = oldWorkHours; }
    
    public String getOldRemark() { return oldRemark; }
    public void setOldRemark(String oldRemark) { this.oldRemark = oldRemark; }
    
    public Integer getNewAttendanceType() { return newAttendanceType; }
    public void setNewAttendanceType(Integer newAttendanceType) { this.newAttendanceType = newAttendanceType; }
    
    public Integer getNewOvertimeType() { return newOvertimeType; }
    public void setNewOvertimeType(Integer newOvertimeType) { this.newOvertimeType = newOvertimeType; }
    
    public BigDecimal getNewWorkHours() { return newWorkHours; }
    public void setNewWorkHours(BigDecimal newWorkHours) { this.newWorkHours = newWorkHours; }
    
    public String getNewRemark() { return newRemark; }
    public void setNewRemark(String newRemark) { this.newRemark = newRemark; }
    
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
    
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    
    public String getApprovedByName() { return approvedByName; }
    public void setApprovedByName(String approvedByName) { this.approvedByName = approvedByName; }
    
    public LocalDateTime getApprovedTime() { return approvedTime; }
    public void setApprovedTime(LocalDateTime approvedTime) { this.approvedTime = approvedTime; }
    
    public String getApproveRemark() { return approveRemark; }
    public void setApproveRemark(String approveRemark) { this.approveRemark = approveRemark; }
    
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    
    // 业务方法：获取状态文本
    public String getStatusText() {
        if (status == null) return "-";
        switch (status) {
            case STATUS_PENDING: return "待审批";
            case STATUS_APPROVED: return "已通过";
            case STATUS_REJECTED: return "已驳回";
            default: return "-";
        }
    }
    
    // 业务方法：获取变更类型文本
    public String getChangeTypeText() {
        if (changeType == null) return "编辑";
        return switch (changeType) {
            case TYPE_ADD -> "新增";
            case TYPE_EDIT -> "编辑";
            case TYPE_DELETE -> "删除";
            default -> "编辑";
        };
    }
    
    // 业务方法：获取出勤类型文本
    public String getAttendanceTypeText(Integer type) {
        if (type == null) return "-";
        return switch (type) {
            case 1 -> "出勤";
            case 2 -> "加班";
            case 3 -> "请假";
            case 4 -> "缺勤";
            case 5 -> "迟到";
            case 6 -> "旷工";
            default -> "-";
        };
    }
    
    // 业务方法：获取加班类型文本
    public String getOvertimeTypeText(Integer type) {
        if (type == null) return "";
        return switch (type) {
            case 1 -> "(工作日)";
            case 2 -> "(休息日)";
            case 3 -> "(法定假期)";
            default -> "";
        };
    }
}
