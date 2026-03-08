package com.example.hello.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工时管理配置实体
 */
public class WorkHourConfig {
    
    private Long id;
    private Integer calcType;  // 计算方式：1-日薪计算，2-月薪计算
    private Integer monthlyWorkDays;  // 每月工作天数，仅月薪计算时使用，默认22天
    
    // 上午工作时间（使用String避免JSON序列化问题）
    private String morningStartTime;   // 上午开始时间
    private String morningEndTime;     // 上午结束时间
    
    // 下午工作时间
    private String afternoonStartTime; // 下午开始时间
    private String afternoonEndTime;   // 下午结束时间
    
    private BigDecimal dailyWorkHours;  // 每日工时（自动计算）
    private String overtimeStartTime;
    private BigDecimal minOvertimeHours;
    
    // 工作日加班
    private BigDecimal weekdayOvertimeRate;
    private BigDecimal weekdayOvertimeHourly;
    
    // 休息日加班
    private BigDecimal restdayOvertimeRate;
    private BigDecimal restdayOvertimeHourly;
    
    // 节假日加班
    private BigDecimal holidayOvertimeRate;
    private BigDecimal holidayOvertimeHourly;
    
    // 状态：0-未生效（草稿/被拒绝），1-审批中，2-生效中
    private Integer status;
    // 发起人信息
    private Long createdById;
    private String createdByName;
    // 审批人信息
    private Long approvedById;
    private String approvedByName;
    private LocalDateTime approvedTime;
    private String approveRemark;  // 审批备注
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Integer getCalcType() {
        return calcType;
    }
    
    public void setCalcType(Integer calcType) {
        this.calcType = calcType;
    }
    
    public BigDecimal getDailyWorkHours() {
        return dailyWorkHours;
    }
    
    public void setDailyWorkHours(BigDecimal dailyWorkHours) {
        this.dailyWorkHours = dailyWorkHours;
    }
    
    public Integer getMonthlyWorkDays() {
        return monthlyWorkDays;
    }
    
    public void setMonthlyWorkDays(Integer monthlyWorkDays) {
        this.monthlyWorkDays = monthlyWorkDays;
    }
    
    public String getMorningStartTime() {
        return morningStartTime;
    }
    
    public void setMorningStartTime(String morningStartTime) {
        this.morningStartTime = morningStartTime;
    }
    
    public String getMorningEndTime() {
        return morningEndTime;
    }
    
    public void setMorningEndTime(String morningEndTime) {
        this.morningEndTime = morningEndTime;
    }
    
    public String getAfternoonStartTime() {
        return afternoonStartTime;
    }
    
    public void setAfternoonStartTime(String afternoonStartTime) {
        this.afternoonStartTime = afternoonStartTime;
    }
    
    public String getAfternoonEndTime() {
        return afternoonEndTime;
    }
    
    public void setAfternoonEndTime(String afternoonEndTime) {
        this.afternoonEndTime = afternoonEndTime;
    }
    
    public String getOvertimeStartTime() {
        return overtimeStartTime;
    }
    
    public void setOvertimeStartTime(String overtimeStartTime) {
        this.overtimeStartTime = overtimeStartTime;
    }
    
    public BigDecimal getMinOvertimeHours() {
        return minOvertimeHours;
    }
    
    public void setMinOvertimeHours(BigDecimal minOvertimeHours) {
        this.minOvertimeHours = minOvertimeHours;
    }
    
    public BigDecimal getWeekdayOvertimeRate() {
        return weekdayOvertimeRate;
    }
    
    public void setWeekdayOvertimeRate(BigDecimal weekdayOvertimeRate) {
        this.weekdayOvertimeRate = weekdayOvertimeRate;
    }
    
    public BigDecimal getWeekdayOvertimeHourly() {
        return weekdayOvertimeHourly;
    }
    
    public void setWeekdayOvertimeHourly(BigDecimal weekdayOvertimeHourly) {
        this.weekdayOvertimeHourly = weekdayOvertimeHourly;
    }
    
    public BigDecimal getRestdayOvertimeRate() {
        return restdayOvertimeRate;
    }
    
    public void setRestdayOvertimeRate(BigDecimal restdayOvertimeRate) {
        this.restdayOvertimeRate = restdayOvertimeRate;
    }
    
    public BigDecimal getRestdayOvertimeHourly() {
        return restdayOvertimeHourly;
    }
    
    public void setRestdayOvertimeHourly(BigDecimal restdayOvertimeHourly) {
        this.restdayOvertimeHourly = restdayOvertimeHourly;
    }
    
    public BigDecimal getHolidayOvertimeRate() {
        return holidayOvertimeRate;
    }
    
    public void setHolidayOvertimeRate(BigDecimal holidayOvertimeRate) {
        this.holidayOvertimeRate = holidayOvertimeRate;
    }
    
    public BigDecimal getHolidayOvertimeHourly() {
        return holidayOvertimeHourly;
    }
    
    public void setHolidayOvertimeHourly(BigDecimal holidayOvertimeHourly) {
        this.holidayOvertimeHourly = holidayOvertimeHourly;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public Long getCreatedById() {
        return createdById;
    }
    
    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }
    
    public String getCreatedByName() {
        return createdByName;
    }
    
    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }
    
    public Long getApprovedById() {
        return approvedById;
    }
    
    public void setApprovedById(Long approvedById) {
        this.approvedById = approvedById;
    }
    
    public String getApprovedByName() {
        return approvedByName;
    }
    
    public void setApprovedByName(String approvedByName) {
        this.approvedByName = approvedByName;
    }
    
    public LocalDateTime getApprovedTime() {
        return approvedTime;
    }
    
    public void setApprovedTime(LocalDateTime approvedTime) {
        this.approvedTime = approvedTime;
    }
    
    public String getApproveRemark() {
        return approveRemark;
    }
    
    public void setApproveRemark(String approveRemark) {
        this.approveRemark = approveRemark;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
    
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
    
    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }
    
    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }
}
