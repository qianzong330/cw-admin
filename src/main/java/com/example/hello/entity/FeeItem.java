package com.example.hello.entity;

import java.time.LocalDateTime;

/**
 * 费用项 - 用于工资条费用加项/减项的基础配置
 */
public class FeeItem {
    
    /**
     * 费用类型：1-加项，2-减项
     */
    public static final int TYPE_PLUS = 1;
    public static final int TYPE_MINUS = 2;
    
    private Long id;
    private String name;           // 费用项名称
    private Integer type;          // 1=加项，2=减项
    private Integer status;        // 0=禁用，1=启用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // 非持久化字段
    private String typeName;
    
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
    
    public Integer getType() {
        return type;
    }
    
    public void setType(Integer type) {
        this.type = type;
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
    
    public String getTypeName() {
        if (type == null) return "";
        return type == TYPE_PLUS ? "加项" : "减项";
    }
    
    public String getTypeBadgeClass() {
        if (type == null) return "bg-secondary";
        return type == TYPE_PLUS ? "bg-success" : "bg-danger";
    }
}
