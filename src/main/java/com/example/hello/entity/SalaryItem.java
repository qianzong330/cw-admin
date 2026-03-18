package com.example.hello.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工资费用项实体（费用+ 和 费用-）
 */
public class SalaryItem {
    
    /** 主键ID */
    private Long id;
    
    /** 工资条ID */
    private Long salarySlipId;
    
    /** 费用类型：1-费用+（应加），2-费用-（应减） */
    private Integer itemType;
    
    /** 费用名称，如：路费、微信转账 */
    private String itemName;
    
    /** 金额 */
    private BigDecimal amount;
    
    /** 备注 */
    private String remark;
    
    /** 修改人姓名 */
    private String modifier;
    
    /** 创建时间 */
    private LocalDateTime createdTime;
    
    /** 更新时间 */
    private LocalDateTime updatedTime;
    
    /**
     * 获取费用类型文本
     */
    public String getItemTypeText() {
        return itemType != null && itemType == 1 ? "费用+" : "费用-";
    }
    
    /**
     * 是否为加项
     */
    public boolean isAddition() {
        return itemType != null && itemType == 1;
    }
    
    /**
     * 是否为减项
     */
    public boolean isDeduction() {
        return itemType != null && itemType == 2;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSalarySlipId() { return salarySlipId; }
    public void setSalarySlipId(Long salarySlipId) { this.salarySlipId = salarySlipId; }
    public Integer getItemType() { return itemType; }
    public void setItemType(Integer itemType) { this.itemType = itemType; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getModifier() { return modifier; }
    public void setModifier(String modifier) { this.modifier = modifier; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
