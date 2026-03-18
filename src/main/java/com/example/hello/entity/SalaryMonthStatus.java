package com.example.hello.entity;

import java.time.LocalDateTime;

/**
 * 工资条月份状态实体
 */
public class SalaryMonthStatus {

    // ===== 状态常量 =====
    public static final int STATUS_DRAFT     = 0;   // 草稿（可编辑）
    public static final int STATUS_PENDING   = 1;   // 待审批（首次提交）
    public static final int STATUS_CHANGE_PENDING = 2;  // 变更待审（二次编辑）
    public static final int STATUS_APPROVED  = 5;   // 已审批锁定
    public static final int STATUS_REJECTED  = 12;  // 已驳回（可重新编辑）

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
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

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

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }

    public boolean isLocked() {
        return status != null && status == STATUS_APPROVED;
    }
}
