package com.example.hello.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工资条修改记录实体
 * 用于记录工资条二次修改的变更内容，长期保存作为变更历史
 */
public class SalarySlipChangeLog {
    
    // 状态常量
    public static final int STATUS_PENDING = 0;   // 待审批
    public static final int STATUS_APPROVED = 1;  // 已通过
    public static final int STATUS_REJECTED = 2;  // 已驳回
    
    private Long id;
    private Long salarySlipId;
    private Long projectId;
    private Long employeeId;
    private String salaryPeriod;
    
    // 变更前数据
    private BigDecimal oldAttendanceDays;
    private BigDecimal oldBaseSalary;
    private BigDecimal oldBaseAmount;
    private BigDecimal oldAdditionAmount;
    private BigDecimal oldDeductionAmount;
    private BigDecimal oldPayableAmount;
    
    // 变更后数据
    private BigDecimal newAttendanceDays;
    private BigDecimal newBaseSalary;
    private BigDecimal newBaseAmount;
    private BigDecimal newAdditionAmount;
    private BigDecimal newDeductionAmount;
    private BigDecimal newPayableAmount;
    
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
    
    // ===== 费用项变更相关字段 =====
    // 变更类型：ADD-新增费用项, EDIT-修改费用项金额, DELETE-删除费用项, BATCH-批量费用项变更
    private String changeType;
    
    // 费用项变更详情（JSON格式存储，包含所有费用项的变更明细）
    // 格式: [{"itemId": 1, "itemName": "餐补", "itemType": 1, "oldAmount": 0, "newAmount": 100, "changeType": "ADD"}, ...]
    private String feeItemsChangeDetail;
    
    // 变更前费用项JSON（完整备份）
    private String oldFeeItemsJson;
    
    // 变更后费用项JSON（完整备份）
    private String newFeeItemsJson;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getSalarySlipId() { return salarySlipId; }
    public void setSalarySlipId(Long salarySlipId) { this.salarySlipId = salarySlipId; }
    
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    
    public String getSalaryPeriod() { return salaryPeriod; }
    public void setSalaryPeriod(String salaryPeriod) { this.salaryPeriod = salaryPeriod; }
    
    public BigDecimal getOldAttendanceDays() { return oldAttendanceDays; }
    public void setOldAttendanceDays(BigDecimal oldAttendanceDays) { this.oldAttendanceDays = oldAttendanceDays; }
    
    public BigDecimal getOldBaseSalary() { return oldBaseSalary; }
    public void setOldBaseSalary(BigDecimal oldBaseSalary) { this.oldBaseSalary = oldBaseSalary; }
    
    public BigDecimal getOldBaseAmount() { return oldBaseAmount; }
    public void setOldBaseAmount(BigDecimal oldBaseAmount) { this.oldBaseAmount = oldBaseAmount; }
    
    public BigDecimal getOldAdditionAmount() { return oldAdditionAmount; }
    public void setOldAdditionAmount(BigDecimal oldAdditionAmount) { this.oldAdditionAmount = oldAdditionAmount; }
    
    public BigDecimal getOldDeductionAmount() { return oldDeductionAmount; }
    public void setOldDeductionAmount(BigDecimal oldDeductionAmount) { this.oldDeductionAmount = oldDeductionAmount; }
    
    public BigDecimal getOldPayableAmount() { return oldPayableAmount; }
    public void setOldPayableAmount(BigDecimal oldPayableAmount) { this.oldPayableAmount = oldPayableAmount; }
    
    public BigDecimal getNewAttendanceDays() { return newAttendanceDays; }
    public void setNewAttendanceDays(BigDecimal newAttendanceDays) { this.newAttendanceDays = newAttendanceDays; }
    
    public BigDecimal getNewBaseSalary() { return newBaseSalary; }
    public void setNewBaseSalary(BigDecimal newBaseSalary) { this.newBaseSalary = newBaseSalary; }
    
    public BigDecimal getNewBaseAmount() { return newBaseAmount; }
    public void setNewBaseAmount(BigDecimal newBaseAmount) { this.newBaseAmount = newBaseAmount; }
    
    public BigDecimal getNewAdditionAmount() { return newAdditionAmount; }
    public void setNewAdditionAmount(BigDecimal newAdditionAmount) { this.newAdditionAmount = newAdditionAmount; }
    
    public BigDecimal getNewDeductionAmount() { return newDeductionAmount; }
    public void setNewDeductionAmount(BigDecimal newDeductionAmount) { this.newDeductionAmount = newDeductionAmount; }
    
    public BigDecimal getNewPayableAmount() { return newPayableAmount; }
    public void setNewPayableAmount(BigDecimal newPayableAmount) { this.newPayableAmount = newPayableAmount; }
    
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
    
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
    
    // ===== 费用项变更 Getter/Setter =====
    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    
    public String getFeeItemsChangeDetail() { return feeItemsChangeDetail; }
    public void setFeeItemsChangeDetail(String feeItemsChangeDetail) { this.feeItemsChangeDetail = feeItemsChangeDetail; }
    
    public String getOldFeeItemsJson() { return oldFeeItemsJson; }
    public void setOldFeeItemsJson(String oldFeeItemsJson) { this.oldFeeItemsJson = oldFeeItemsJson; }
    
    public String getNewFeeItemsJson() { return newFeeItemsJson; }
    public void setNewFeeItemsJson(String newFeeItemsJson) { this.newFeeItemsJson = newFeeItemsJson; }
}
