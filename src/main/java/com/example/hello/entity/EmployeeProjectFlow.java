package com.example.hello.entity;

import java.time.LocalDateTime;

/**
 * 员工项目流动记录实体（记录员工加入和离开项目的历史）
 */
public class EmployeeProjectFlow {
    
    public static final int OPERATION_JOIN = 1;   // 加入项目
    public static final int OPERATION_LEAVE = 2;  // 离开项目
    
    /** 主键ID */
    private Long id;
    
    /** 员工ID */
    private Long employeeId;
    
    /** 项目ID */
    private Long projectId;
    
    /** 员工姓名（冗余存储） */
    private String employeeName;
    
    /** 工种名称（冗余存储） */
    private String jobCategoryName;
    
    /** 操作类型：1-加入，2-移除 */
    private Integer operationType;
    
    /** 操作时间 */
    private LocalDateTime operationTime;
    
    /** 操作人ID */
    private Long operatorId;
    
    /** 操作人姓名 */
    private String operatorName;
    
    /** 备注 */
    private String remark;
    
    /** 记录创建时间 */
    private LocalDateTime createTime;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }
    
    public Long getProjectId() {
        return projectId;
    }
    
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
    
    public String getEmployeeName() {
        return employeeName;
    }
    
    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }
    
    public String getJobCategoryName() {
        return jobCategoryName;
    }
    
    public void setJobCategoryName(String jobCategoryName) {
        this.jobCategoryName = jobCategoryName;
    }
    
    public Integer getOperationType() {
        return operationType;
    }
    
    public void setOperationType(Integer operationType) {
        this.operationType = operationType;
    }
    
    public LocalDateTime getOperationTime() {
        return operationTime;
    }
    
    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }
    
    public Long getOperatorId() {
        return operatorId;
    }
    
    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }
    
    public String getOperatorName() {
        return operatorName;
    }
    
    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
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
}
