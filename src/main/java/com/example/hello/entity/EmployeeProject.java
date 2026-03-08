package com.example.hello.entity;

import java.time.LocalDateTime;

/**
 * 员工项目关联实体
 */
public class EmployeeProject {
    
    /** 主键ID */
    private Long id;
    
    /** 员工ID */
    private Long employeeId;
    
    /** 项目ID */
    private Long projectId;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    // 关联字段
    private String employeeName;
    private String projectName;
    private String jobCategoryName;
    
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
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
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
    
    public String getJobCategoryName() {
        return jobCategoryName;
    }
    
    public void setJobCategoryName(String jobCategoryName) {
        this.jobCategoryName = jobCategoryName;
    }
}
