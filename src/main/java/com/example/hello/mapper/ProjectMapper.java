package com.example.hello.mapper;

import com.example.hello.entity.Project;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProjectMapper {
    
    Project findById(Long id);
    
    List<Project> findAll();
    
    /**
     * 根据员工ID查询关联的项目列表
     */
    List<Project> findByEmployeeId(Long employeeId);
    
    /**
     * 根据管理员ID查询管理的项目列表
     */
    List<Project> findByAdminId(Long adminId);
    
    int insert(Project project);
    
    int update(Project project);
    
    int deleteById(Long id);
}
