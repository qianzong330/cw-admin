package com.example.hello.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Employee {
    private Long id;
    private String name;
    private Long roleId;
    private String phone;
    private Long jobCategoryId;
    private BigDecimal salaryAmount;
    private Integer salaryType;
    private String password;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 关联字段
    private String roleName;
    private String roleCode;
    private String jobCategoryName;
    
    // 员工隶属的项目列表（非数据库字段）
    private String projectNames;
    
    // 菜单权限码集合（非数据库字段，登录后加载）
    private java.util.Set<String> menuCodes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getJobCategoryId() {
        return jobCategoryId;
    }

    public void setJobCategoryId(Long jobCategoryId) {
        this.jobCategoryId = jobCategoryId;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getJobCategoryName() {
        return jobCategoryName;
    }

    public void setJobCategoryName(String jobCategoryName) {
        this.jobCategoryName = jobCategoryName;
    }
    
    public String getProjectNames() {
        return projectNames;
    }
    
    public void setProjectNames(String projectNames) {
        this.projectNames = projectNames;
    }

    public boolean isBoss() {
        return roleCode != null && "boss".equalsIgnoreCase(roleCode);
    }
    
    // Thymeleaf 使用 boss 属性时会调用此方法
    public boolean getBoss() {
        return isBoss();
    }

    public boolean isFinance() {
        return roleCode != null && "finance".equalsIgnoreCase(roleCode);
    }
    
    // Thymeleaf 使用 finance 属性时会调用此方法
    public boolean getFinance() {
        return isFinance();
    }
    
    public java.util.Set<String> getMenuCodes() {
        return menuCodes;
    }
    
    public void setMenuCodes(java.util.Set<String> menuCodes) {
        this.menuCodes = menuCodes;
    }
    
    /**
     * 检查是否有菜单权限
     * root 和 boss 角色拥有所有权限
     */
    public boolean hasPermission(String menuCode) {
        // root 和 boss 角色拥有所有权限
        if ("root".equalsIgnoreCase(roleCode) || "boss".equalsIgnoreCase(roleCode)) {
            return true;
        }
        if (menuCodes == null || menuCodes.isEmpty()) {
            return false;
        }
        return menuCodes.contains(menuCode);
    }
}
