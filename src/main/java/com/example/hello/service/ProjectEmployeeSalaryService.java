package com.example.hello.service;

import com.example.hello.entity.Employee;
import com.example.hello.entity.ProjectEmployeeSalary;
import com.example.hello.mapper.ProjectEmployeeSalaryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 项目员工薪资历史 Service
 */
@Service
public class ProjectEmployeeSalaryService {
    
    @Autowired
    private ProjectEmployeeSalaryMapper salaryMapper;
    
    /**
     * 添加薪资记录（调薪）
     */
    @Transactional
    public void addSalaryRecord(ProjectEmployeeSalary salary) {
        // 检查同一日期是否已有记录
        int count = salaryMapper.countByEffectiveDate(
            salary.getProjectId(), salary.getEmployeeId(), salary.getEffectiveDate());
        if (count > 0) {
            throw new RuntimeException("该生效日期已存在薪资记录，请勿重复添加");
        }
        salaryMapper.insert(salary);
    }
    
    /**
     * 员工加入项目时，初始化薪资记录
     * 使用员工表的当前薪资作为初始薪资
     * @param effectiveDate 生效日期（传入项目创建日期向前推3年，确保可追溯历史考勤）
     */
    @Transactional
    public void initSalaryForEmployee(Long projectId, Employee employee, Long operatorId, LocalDate effectiveDate) {
        // 检查该生效日期是否已有薪资记录（避免重复初始化）
        int count = salaryMapper.countByEffectiveDate(projectId, employee.getId(), effectiveDate);
        if (count > 0) {
            System.out.println("员工 " + employee.getId() + " 在项目 " + projectId + " 该日期已有薪资记录，跳过初始化");
            return;
        }
        
        ProjectEmployeeSalary salary = new ProjectEmployeeSalary();
        salary.setProjectId(projectId);
        salary.setEmployeeId(employee.getId());
        salary.setSalaryAmount(employee.getSalaryAmount());
        salary.setSalaryType(employee.getSalaryType());
        salary.setEffectiveDate(effectiveDate);
        salary.setCreatedBy(operatorId);
        salary.setRemark("员工加入项目时初始化");
        
        salaryMapper.insert(salary);
        System.out.println("员工 " + employee.getId() + " 在项目 " + projectId + " 的薪资记录初始化成功，金额：" + employee.getSalaryAmount() + "，生效日期：" + effectiveDate);
    }
    
    /**
     * 查询项目员工的所有薪资历史
     */
    public List<ProjectEmployeeSalary> getSalaryHistory(Long projectId, Long employeeId) {
        return salaryMapper.selectByProjectAndEmployee(projectId, employeeId);
    }
    
    /**
     * 查询项目的所有员工薪资历史
     */
    public List<ProjectEmployeeSalary> getProjectSalaryHistory(Long projectId) {
        return salaryMapper.selectByProject(projectId);
    }
    
    /**
     * 获取指定月份生效的薪资（用于工资条计算）
     * 取生效日期 <= 月份第一天的最新记录
     */
    public ProjectEmployeeSalary getEffectiveSalary(Long projectId, Long employeeId, YearMonth yearMonth) {
        LocalDate firstDayOfMonth = yearMonth.atDay(1);
        return salaryMapper.selectEffectiveSalary(projectId, employeeId, firstDayOfMonth);
    }
    
    /**
     * 获取指定日期生效的薪资（用于前端展示）
     * 取生效日期 <= 指定日期的最新记录
     */
    public ProjectEmployeeSalary getEffectiveSalaryForDate(Long projectId, Long employeeId, LocalDate date) {
        return salaryMapper.selectEffectiveSalary(projectId, employeeId, date);
    }
    
    /**
     * 获取员工在项目的最新薪资
     */
    public ProjectEmployeeSalary getLatestSalary(Long projectId, Long employeeId) {
        return salaryMapper.selectLatestSalary(projectId, employeeId);
    }
    
    /**
     * 删除薪资记录
     */
    @Transactional
    public void deleteSalaryRecord(Long id) {
        salaryMapper.deleteById(id);
    }
    
    /**
     * 计算工资条应使用的薪资
     * 如果没有项目薪资记录，则返回员工表的默认薪资
     */
    public BigDecimal calculateSalaryForPeriod(Long projectId, Employee employee, YearMonth yearMonth) {
        ProjectEmployeeSalary projectSalary = getEffectiveSalary(projectId, employee.getId(), yearMonth);
        if (projectSalary != null) {
            return projectSalary.getSalaryAmount();
        }
        // 没有项目薪资记录，使用员工表默认薪资
        return employee.getSalaryAmount();
    }
    
    /**
     * 获取工资条应使用的薪资类型
     */
    public Integer getSalaryTypeForPeriod(Long projectId, Employee employee, YearMonth yearMonth) {
        ProjectEmployeeSalary projectSalary = getEffectiveSalary(projectId, employee.getId(), yearMonth);
        if (projectSalary != null) {
            return projectSalary.getSalaryType();
        }
        // 没有项目薪资记录，使用员工表默认类型
        return employee.getSalaryType();
    }
}
