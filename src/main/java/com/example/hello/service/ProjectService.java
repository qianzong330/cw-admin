package com.example.hello.service;

import com.example.hello.entity.EmployeeProject;
import com.example.hello.entity.Project;
import com.example.hello.entity.ProjectAdmin;
import com.example.hello.mapper.EmployeeProjectMapper;
import com.example.hello.mapper.ProjectAdminMapper;
import com.example.hello.mapper.ProjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public Project findById(Long id) {
        return projectMapper.findById(id);
    }

    public List<Project> findAll() {
        return projectMapper.findAll();
    }

    /**
     * 根据用户ID查询可见的项目列表
     * BOSS/财务可以看到所有项目，普通员工只能看到关联的项目
     */
    public List<Project> findByUserId(Long userId, boolean isBoss) {
        if (isBoss) {
            return projectMapper.findAll();
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
    @Transactional
    public boolean assignEmployeeToProject(Long employeeId, Long projectId) {
        // 检查是否已关联
        if (employeeProjectMapper.countByEmployeeAndProject(employeeId, projectId) > 0) {
            return true; // 已关联，直接返回成功
        }
        EmployeeProject ep = new EmployeeProject();
        ep.setEmployeeId(employeeId);
        ep.setProjectId(projectId);
        return employeeProjectMapper.insert(ep) > 0;
    }

    /**
     * 取消员工与项目的关联
     */
    @Transactional
    public boolean removeEmployeeFromProject(Long employeeId, Long projectId) {
        return employeeProjectMapper.deleteByEmployeeAndProject(employeeId, projectId) > 0;
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
