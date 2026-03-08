package com.example.hello.service;

import com.example.hello.entity.Employee;
import com.example.hello.entity.WorkHourConfig;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 加班时薪计算服务
 */
@Service
public class OvertimePayService {
    
    /**
     * 计算员工的基础时薪（不含加班费率）
     * 
     * @param employee 员工信息
     * @param config 工时配置
     * @return 基础时薪（元/小时）
     */
    public BigDecimal calculateBaseHourlyRate(Employee employee, WorkHourConfig config) {
        if (employee == null || employee.getSalaryAmount() == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal salaryAmount = employee.getSalaryAmount();
        Integer calcType = config.getCalcType() != null ? config.getCalcType() : 2;  // 默认月薪计算
        BigDecimal dailyHours = config.getDailyWorkHours();
        Integer monthlyDays = config.getMonthlyWorkDays() != null ? config.getMonthlyWorkDays() : 22;
        
        // 默认每日8小时
        if (dailyHours == null) {
            dailyHours = new BigDecimal("8");
        }
        
        BigDecimal hourlyRate;
        
        if (calcType == 1) {
            // 日薪计算：时薪 = 日薪 / 每日工作小时数
            hourlyRate = salaryAmount.divide(dailyHours, 2, RoundingMode.HALF_UP);
        } else {
            // 月薪计算（默认）：时薪 = 月薪 / 每月工作天数 / 每日工作小时数
            hourlyRate = salaryAmount
                    .divide(new BigDecimal(monthlyDays), 2, RoundingMode.HALF_UP)
                    .divide(dailyHours, 2, RoundingMode.HALF_UP);
        }
        
        return hourlyRate;
    }
    
    /**
     * 计算工作日加班时薪
     * 
     * @param employee 员工信息
     * @param config 工时配置
     * @return 工作日加班时薪
     */
    public BigDecimal calculateWeekdayOvertimeHourly(Employee employee, WorkHourConfig config) {
        // 如果配置了固定时薪，直接使用
        if (config.getWeekdayOvertimeHourly() != null 
                && config.getWeekdayOvertimeHourly().compareTo(BigDecimal.ZERO) > 0) {
            return config.getWeekdayOvertimeHourly();
        }
        
        // 否则自动计算：基础时薪 * 加班费率
        BigDecimal baseHourly = calculateBaseHourlyRate(employee, config);
        BigDecimal rate = config.getWeekdayOvertimeRate();
        if (rate == null) {
            rate = new BigDecimal("1.5");
        }
        
        return baseHourly.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算休息日加班时薪
     */
    public BigDecimal calculateRestdayOvertimeHourly(Employee employee, WorkHourConfig config) {
        if (config.getRestdayOvertimeHourly() != null 
                && config.getRestdayOvertimeHourly().compareTo(BigDecimal.ZERO) > 0) {
            return config.getRestdayOvertimeHourly();
        }
        
        BigDecimal baseHourly = calculateBaseHourlyRate(employee, config);
        BigDecimal rate = config.getRestdayOvertimeRate();
        if (rate == null) {
            rate = new BigDecimal("2.0");
        }
        
        return baseHourly.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算节假日加班时薪
     */
    public BigDecimal calculateHolidayOvertimeHourly(Employee employee, WorkHourConfig config) {
        if (config.getHolidayOvertimeHourly() != null 
                && config.getHolidayOvertimeHourly().compareTo(BigDecimal.ZERO) > 0) {
            return config.getHolidayOvertimeHourly();
        }
        
        BigDecimal baseHourly = calculateBaseHourlyRate(employee, config);
        BigDecimal rate = config.getHolidayOvertimeRate();
        if (rate == null) {
            rate = new BigDecimal("3.0");
        }
        
        return baseHourly.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
