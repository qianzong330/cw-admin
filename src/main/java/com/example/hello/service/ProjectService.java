package com.example.hello.service;

import com.example.hello.entity.Employee;
import com.example.hello.entity.EmployeeProject;
import com.example.hello.entity.EmployeeProjectFlow;
import com.example.hello.entity.Project;
import com.example.hello.entity.ProjectAdmin;
import com.example.hello.mapper.EmployeeProjectFlowMapper;
import com.example.hello.mapper.EmployeeProjectMapper;
import com.example.hello.mapper.ProjectAdminMapper;
import com.example.hello.mapper.ProjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private EmployeeProjectMapper employeeProjectMapper;

    @Autowired
    private ProjectAdminMapper projectAdminMapper;
    
    @Autowired
    private EmployeeProjectFlowMapper employeeProjectFlowMapper;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private ProjectEmployeeSalaryService projectEmployeeSalaryService;

    public Project findById(Long id) {
        return projectMapper.findById(id);
    }

    public List<Project> findAll() {
        return projectMapper.findAll();
    }

    /**
     * 根据用户ID查询可见的项目列表
     * BOSS可以看到所有项目，项目管理员看到自己管理的项目，普通员工只能看到关联的项目
     */
    public List<Project> findByUserId(Long userId, boolean isBoss, boolean isProjectAdmin) {
        if (isBoss) {
            return projectMapper.findAll();
        } else if (isProjectAdmin) {
            // 项目管理员看到自己管理的项目
            return projectMapper.findByAdminId(userId);
        } else {
            return projectMapper.findByEmployeeId(userId);
        }
    }

    public boolean save(Project project) {
        if (project.getId() == null) {
            return projectMapper.insert(project) > 0;
        } else {
            return projectMapper.update(project) > 0;
        }
    }

    /**
     * 保存项目并设置管理员
     */
    @Transactional
    public boolean saveWithAdmins(Project project, List<Long> adminIds) {
        System.out.println("=== ProjectService.saveWithAdmins ===");
        System.out.println("project.id: " + project.getId());
        System.out.println("project.name: " + project.getName());
        System.out.println("adminIds: " + adminIds);
        
        // 1. 保存项目
        boolean success;
        if (project.getId() == null) {
            System.out.println("执行insert操作");
            success = projectMapper.insert(project) > 0;
            System.out.println("insert结果: " + success + ", 新ID: " + project.getId());
        } else {
            System.out.println("执行update操作");
            success = projectMapper.update(project) > 0;
        }
        
        if (!success) {
            System.out.println("项目保存失败");
            return false;
        }
        
        // 2. 删除旧的管理员关联
        System.out.println("删除旧管理员关联，projectId: " + project.getId());
        projectAdminMapper.deleteByProjectId(project.getId());
        
        // 3. 添加新的管理员关联
        if (adminIds != null && !adminIds.isEmpty()) {
            System.out.println("添加新管理员关联: " + adminIds);
            for (Long adminId : adminIds) {
                ProjectAdmin pa = new ProjectAdmin();
                pa.setProjectId(project.getId());
                pa.setEmployeeId(adminId);
                projectAdminMapper.insert(pa);
            }
        }
        
        System.out.println("保存完成");
        return true;
    }

    /**
     * 查询所有项目（带管理员信息）
     */
    public List<Project> findAllWithAdmins() {
        List<Project> projects = projectMapper.findAll();
        for (Project project : projects) {
            List<ProjectAdmin> admins = projectAdminMapper.findByProjectId(project.getId());
            project.setAdmins(admins);
            // 设置管理员姓名列表（逗号分隔）
            if (admins != null && !admins.isEmpty()) {
                String adminNames = admins.stream()
                    .map(ProjectAdmin::getEmployeeName)
                    .collect(Collectors.joining(", "));
                project.setAdminNames(adminNames);
            }
        }
        return projects;
    }

    /**
     * 根据ID查询项目（带管理员信息）
     */
    public Project findByIdWithAdmins(Long id) {
        Project project = projectMapper.findById(id);
        if (project != null) {
            List<ProjectAdmin> admins = projectAdminMapper.findByProjectId(id);
            project.setAdmins(admins);
        }
        return project;
    }

    public boolean deleteById(Long id) {
        return projectMapper.deleteById(id) > 0;
    }

    /**
     * 关联员工到项目
     */
    /**
     * 关联员工到项目，并记录流动记录
     */
    @Transactional
    public boolean assignEmployeeToProject(Long employeeId, Long projectId, Long operatorId, String operatorName) {
        // 检查是否已关联
        if (employeeProjectMapper.countByEmployeeAndProject(employeeId, projectId) > 0) {
            return true; // 已关联，直接返回成功
        }
        
        // 先查询员工信息（从员工表获取）
        String employeeName = null;
        String jobCategoryName = null;
        try {
            // 使用JdbcTemplate查询员工信息
            var empInfo = jdbcTemplate.queryForMap(
                "SELECT e.name, jc.name as job_category_name FROM tb_employee e " +
                "LEFT JOIN tb_job_category jc ON e.job_category_id = jc.id WHERE e.id = ?", 
                employeeId);
            if (empInfo != null) {
                employeeName = (String) empInfo.get("name");
                jobCategoryName = (String) empInfo.get("job_category_name");
            }
        } catch (Exception e) {
            // 查询失败继续执行
        }
            
        EmployeeProject ep = new EmployeeProject();
        ep.setEmployeeId(employeeId);
        ep.setProjectId(projectId);
        ep.setCreateTime(LocalDateTime.now()); // 记录加入项目时间
        int result = employeeProjectMapper.insert(ep);
            
        if (result > 0) {
            // 记录加入流动记录
            EmployeeProjectFlow flow = new EmployeeProjectFlow();
            flow.setEmployeeId(employeeId);
            flow.setProjectId(projectId);
            flow.setEmployeeName(employeeName);
            flow.setJobCategoryName(jobCategoryName);
            flow.setOperationType(EmployeeProjectFlow.OPERATION_JOIN);
            flow.setOperationTime(LocalDateTime.now());
            flow.setOperatorId(operatorId);
            flow.setOperatorName(operatorName);
            employeeProjectFlowMapper.insert(flow);
            
            // 初始化员工在项目中的薪资记录
            try {
                // 使用员工加入项目的日期（今天）向前推3年作为生效日期
                LocalDate joinDate = LocalDate.now();
                LocalDate effectiveDate = joinDate.minusYears(3);
                
                var empInfo = jdbcTemplate.queryForMap(
                    "SELECT salary_amount, salary_type FROM tb_employee WHERE id = ?", 
                    employeeId);
                if (empInfo != null) {
                    Object salaryAmountObj = empInfo.get("salary_amount");
                    Object salaryTypeObj = empInfo.get("salary_type");
                    
                    // 检查薪资数据是否有效
                    if (salaryAmountObj == null || salaryTypeObj == null) {
                        System.err.println("员工 " + employeeId + " 的薪资数据为空，无法初始化项目薪资记录");
                    } else {
                        Employee emp = new Employee();
                        emp.setId(employeeId);
                        emp.setSalaryAmount(new java.math.BigDecimal(salaryAmountObj.toString()));
                        emp.setSalaryType(((Number) salaryTypeObj).intValue());
                        // 使用员工加入日期向前推3年作为生效日期，确保可以追溯历史考勤
                        projectEmployeeSalaryService.initSalaryForEmployee(projectId, emp, operatorId, effectiveDate);
                        System.out.println("员工 " + employeeId + " 在项目 " + projectId + " 的薪资记录初始化成功，生效日期：" + effectiveDate);
                    }
                }
            } catch (Exception e) {
                // 初始化薪资失败不影响主流程
                System.err.println("初始化员工薪资记录失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
            
        return result > 0;
    }
        
    /**
     * 取消员工与项目的关联，并记录流动记录
     */
    @Transactional
    public boolean removeEmployeeFromProject(Long employeeId, Long projectId, Long operatorId, String operatorName) {
        // 先查询关联信息
        EmployeeProject ep = employeeProjectMapper.selectByEmployeeAndProject(employeeId, projectId);
        if (ep == null) {
            return false;
        }
            
        // 记录移除流动记录
        EmployeeProjectFlow flow = new EmployeeProjectFlow();
        flow.setEmployeeId(employeeId);
        flow.setProjectId(projectId);
        flow.setEmployeeName(ep.getEmployeeName());
        flow.setJobCategoryName(ep.getJobCategoryName());
        flow.setOperationType(EmployeeProjectFlow.OPERATION_LEAVE);
        flow.setOperationTime(LocalDateTime.now());
        flow.setOperatorId(operatorId);
        flow.setOperatorName(operatorName);
        employeeProjectFlowMapper.insert(flow);
            
        // 删除关联
        return employeeProjectMapper.deleteByEmployeeAndProject(employeeId, projectId) > 0;
    }
    
    /**
     * 获取项目的员工流动记录
     */
    public List<EmployeeProjectFlow> getProjectEmployeeFlow(Long projectId) {
        return employeeProjectFlowMapper.selectByProjectId(projectId);
    }

    /**
     * 获取项目的所有关联员工
     */
    public List<EmployeeProject> getProjectEmployees(Long projectId) {
        return employeeProjectMapper.selectByProjectId(projectId);
    }

    /**
     * 获取员工的所有关联项目
     */
    public List<EmployeeProject> getEmployeeProjects(Long employeeId) {
        return employeeProjectMapper.selectByEmployeeId(employeeId);
    }
}
