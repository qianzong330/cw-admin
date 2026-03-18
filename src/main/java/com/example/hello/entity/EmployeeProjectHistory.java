package com.example.hello.entity;

import java.time.LocalDateTime;

/**
 * 员工项目关联历史实体（记录已移除的员工）
 */
public class EmployeeProjectHistory {
    
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
    
    /** 加入项目时间 */
    private LocalDateTime joinTime;
    
    /** 离开项目时间 */
    private LocalDateTime leaveTime;
    
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
    
    public LocalDateTime getJoinTime() {
        return joinTime;
    }
    
    public void setJoinTime(LocalDateTime joinTime) {
        this.joinTime = joinTime;
    }
    
    public LocalDateTime getLeaveTime() {
        return leaveTime;
    }
    
    public void setLeaveTime(LocalDateTime leaveTime) {
        this.leaveTime = leaveTime;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
