package com.example.hello.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Account {
    private Long id;
    private Long projectId;
    private Long creatorId;
    private Long jobCategoryId;
    private Long roleId;
    private Integer type;
    private Long categoryId;
    private String invoiceNo;
    private Integer status;
    private String companyName;
    private String approvalProgressId;
    private BigDecimal amount;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 关联字段
    private String projectName;
    private String creatorName;
    private String jobCategoryName;
    private String roleName;
    private String categoryName;
    private Long financeContactId;
    private String financeContactName;
    
    // 多级审批字段
    private Integer approvalStage; // 审批阶段：1-待财务审批，2-待BOSS审批
    private String approvedByFinance; // 已审批的财务人员ID列表（逗号分隔）
    private Long finalApproverId; // 最终审批人ID（BOSS）

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public Long getJobCategoryId() {
        return jobCategoryId;
    }

    public void setJobCategoryId(Long jobCategoryId) {
        this.jobCategoryId = jobCategoryId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getApprovalProgressId() {
        return approvalProgressId;
    }

    public void setApprovalProgressId(String approvalProgressId) {
        this.approvalProgressId = approvalProgressId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getJobCategoryName() {
        return jobCategoryName;
    }

    public void setJobCategoryName(String jobCategoryName) {
        this.jobCategoryName = jobCategoryName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getFinanceContactId() {
        return financeContactId;
    }

    public void setFinanceContactId(Long financeContactId) {
        this.financeContactId = financeContactId;
    }

    public String getFinanceContactName() {
        return financeContactName;
    }

    public void setFinanceContactName(String financeContactName) {
        this.financeContactName = financeContactName;
    }

    public Integer getApprovalStage() {
        return approvalStage;
    }

    public void setApprovalStage(Integer approvalStage) {
        this.approvalStage = approvalStage;
    }

    public String getApprovedByFinance() {
        return approvedByFinance;
    }

    public void setApprovedByFinance(String approvedByFinance) {
        this.approvedByFinance = approvedByFinance;
    }

    public Long getFinalApproverId() {
        return finalApproverId;
    }

    public void setFinalApproverId(Long finalApproverId) {
        this.finalApproverId = finalApproverId;
    }

    public String getStatusText() {
        switch (status) {
            case 1: return "审批中";
            case 5: return "生效";
            case 12: return "审核未通过";
            default: return "未知";
        }
    }

    public String getTypeText() {
        return type == 1 ? "收入" : "支出";
    }
}
