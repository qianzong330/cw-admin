package com.example.hello.service;

import com.example.hello.entity.EmployeeProject;
import com.example.hello.entity.Project;
import com.example.hello.mapper.EmployeeProjectMapper;
import com.example.hello.mapper.ProjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private EmployeeProjectMapper employeeProjectMapper;

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
