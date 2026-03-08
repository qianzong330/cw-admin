package com.example.hello.dto;

import java.math.BigDecimal;

public class ProjectStatsDTO {
    private Long projectId;
    private String projectName;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal balance;
    
    public Long getProjectId() {
        return projectId;
    }
    
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
    
    public String getProjectName() {
        return projectName;
    }
    
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    public BigDecimal getIncome() {
        return income;
    }
    
    public void setIncome(BigDecimal income) {
        this.income = income;
    }
    
    public BigDecimal getExpense() {
        return expense;
    }
    
    public void setExpense(BigDecimal expense) {
        this.expense = expense;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
