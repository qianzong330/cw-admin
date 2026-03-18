package com.example.hello.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目员工薪资历史实体
 * 支持同一员工在不同项目有不同薪资，各项目独立调薪
 */
public class ProjectEmployeeSalary {
    
    /** 主键ID */
    private Long id;
    
    /** 项目ID */
    private Long projectId;
    
    /** 员工ID */
    private Long employeeId;
    
    /** 薪资金额 */
    private BigDecimal salaryAmount;
    
    /** 薪资类型：1-日薪，2-月薪 */
    private Integer salaryType;
    
    /** 生效日期 */
    private LocalDate effectiveDate;
    
    /** 操作人ID */
    private Long createdBy;
    
    /** 创建时间 */
    private LocalDateTime createdTime;
    
    /** 变更原因 */
    private String remark;
    
    // 关联字段（非数据库字段）
    /** 员工姓名 */
    private String employeeName;
    
    /** 项目名称 */
    private String projectName;
    
    /** 操作人姓名 */
    private String createdByName;
    
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
    
    public Long getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }
    
    public BigDecimal getSalaryAmount() {
        return salaryAmount;
    }
    
    public void setSalaryAmount(BigDecimal salaryAmount) {
        this.salaryAmount = salaryAmount;
    }
    
    public Integer getSalaryType() {
        return salaryType;
    }
    
    public void setSalaryType(Integer salaryType) {
        this.salaryType = salaryType;
    }
    
    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }
    
    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
    
    public Long getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
    
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
    
    public String getEmployeeName() {
        return employeeName;
    }
    
    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }
    
    public String getProjectName() {
        return projectName;
    }
    
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    public String getCreatedByName() {
        return createdByName;
    }
    
    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }
    
    /**
     * 获取薪资类型名称
     */
    public String getSalaryTypeName() {
        return salaryType != null && salaryType == 2 ? "月薪" : "日薪";
    }
}
