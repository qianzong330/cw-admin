package com.example.hello.entity;

import java.time.LocalDateTime;

/**
 * 月度考勤表状态
 */
public class AttendanceMonthStatus {
    
    private Long id;
    private String yearMonth;
    private Long projectId;
    private Integer status;
    private Long submitBy;
    private LocalDateTime submitTime;
    private String submitRemark;
    private Long approveBy;
    private LocalDateTime approveTime;
    private String approveRemark;
    private Long changeApplyBy;
    private LocalDateTime changeApplyTime;
    private String changeApplyRemark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    
    // 关联查询展示字段（非数据库存储）
    private String projectName;
    private String submitByName;
    
    // 状态常量
    public static final int STATUS_DRAFT = 0;        // 草稿
    public static final int STATUS_PENDING = 1;      // 待审批
    public static final int STATUS_APPROVED = 5;     // 已审批锁定
    public static final int STATUS_REJECTED = 12;    // 已驳回
    
    public String getStatusText() {
        return switch (status) {
            case STATUS_DRAFT -> "草稿";
            case STATUS_PENDING -> "待审批";
            case STATUS_APPROVED -> "已审批锁定";
            case STATUS_REJECTED -> "已驳回";
            default -> "未知";
        };
    }
    
    public String getStatusBadgeClass() {
        return switch (status) {
            case STATUS_DRAFT -> "bg-secondary";
            case STATUS_PENDING -> "bg-warning text-dark";
            case STATUS_APPROVED -> "bg-success";
            case STATUS_REJECTED -> "bg-danger";
            default -> "bg-secondary";
        };
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }
    
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    
    public Long getSubmitBy() { return submitBy; }
    public void setSubmitBy(Long submitBy) { this.submitBy = submitBy; }
    
    public LocalDateTime getSubmitTime() { return submitTime; }
    public void setSubmitTime(LocalDateTime submitTime) { this.submitTime = submitTime; }
    
    public String getSubmitRemark() { return submitRemark; }
    public void setSubmitRemark(String submitRemark) { this.submitRemark = submitRemark; }
    
    public Long getApproveBy() { return approveBy; }
    public void setApproveBy(Long approveBy) { this.approveBy = approveBy; }
    
    public LocalDateTime getApproveTime() { return approveTime; }
    public void setApproveTime(LocalDateTime approveTime) { this.approveTime = approveTime; }
    
    public String getApproveRemark() { return approveRemark; }
    public void setApproveRemark(String approveRemark) { this.approveRemark = approveRemark; }
    
    public Long getChangeApplyBy() { return changeApplyBy; }
    public void setChangeApplyBy(Long changeApplyBy) { this.changeApplyBy = changeApplyBy; }
    
    public LocalDateTime getChangeApplyTime() { return changeApplyTime; }
    public void setChangeApplyTime(LocalDateTime changeApplyTime) { this.changeApplyTime = changeApplyTime; }
    
    public String getChangeApplyRemark() { return changeApplyRemark; }
    public void setChangeApplyRemark(String changeApplyRemark) { this.changeApplyRemark = changeApplyRemark; }
    
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
    
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    
    public String getSubmitByName() { return submitByName; }
    public void setSubmitByName(String submitByName) { this.submitByName = submitByName; }
}