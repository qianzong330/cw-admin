package com.example.hello.entity;

import java.time.LocalDateTime;
import java.util.List;

public class Project {
    private Long id;
    private String name;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // 关联字段：项目管理员列表
    private List<ProjectAdmin> admins;
    private String adminNames; // 用于列表展示的管理员姓名（逗号分隔）

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

    public List<ProjectAdmin> getAdmins() {
        return admins;
    }

    public void setAdmins(List<ProjectAdmin> admins) {
        this.admins = admins;
    }

    public String getAdminNames() {
        return adminNames;
    }

    public void setAdminNames(String adminNames) {
        this.adminNames = adminNames;
    }
}
