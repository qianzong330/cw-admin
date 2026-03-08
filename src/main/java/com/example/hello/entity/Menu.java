package com.example.hello.entity;

import java.time.LocalDateTime;

/**
 * 菜单实体类
 */
public class Menu {
    
    private Long id;
    private String menuCode;
    private String menuName;
    private Long parentId;
    private Integer menuType; // 1-目录，2-菜单，3-按钮
    private String menuUrl;
    private String menuIcon;
    private Integer sortOrder;
    private Integer status; // 0-禁用，1-启用
    private String remark;
    private LocalDateTime createdTime;
    
    public Menu() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getMenuCode() { return menuCode; }
    public void setMenuCode(String menuCode) { this.menuCode = menuCode; }
    
    public String getMenuName() { return menuName; }
    public void setMenuName(String menuName) { this.menuName = menuName; }
    
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    
    public Integer getMenuType() { return menuType; }
    public void setMenuType(Integer menuType) { this.menuType = menuType; }
    
    public String getMenuUrl() { return menuUrl; }
    public void setMenuUrl(String menuUrl) { this.menuUrl = menuUrl; }
    
    public String getMenuIcon() { return menuIcon; }
    public void setMenuIcon(String menuIcon) { this.menuIcon = menuIcon; }
    
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
}
