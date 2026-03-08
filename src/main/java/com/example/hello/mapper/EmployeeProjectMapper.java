package com.example.hello.mapper;

import com.example.hello.entity.EmployeeProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 员工项目关联 Mapper
 */
@Mapper
public interface EmployeeProjectMapper {
    
    /**
     * 插入关联关系
     */
    int insert(EmployeeProject employeeProject);
    
    /**
     * 删除关联关系
     */
    int deleteById(Long id);
    
    /**
     * 根据员工ID和项目ID删除
     */
    int deleteByEmployeeAndProject(@Param("employeeId") Long employeeId, @Param("projectId") Long projectId);
    
    /**
     * 删除员工的所有项目关联
     */
    int deleteByEmployeeId(@Param("employeeId") Long employeeId);
    
    /**
     * 根据ID查询
     */
    EmployeeProject selectById(Long id);
    
    /**
     * 根据员工ID查询关联的项目ID列表
     */
    List<Long> selectProjectIdsByEmployeeId(Long employeeId);
    
    /**
     * 根据项目ID查询关联的员工ID列表
     */
    List<Long> selectEmployeeIdsByProjectId(Long projectId);
    
    /**
     * 查询员工的所有关联关系
     */
    List<EmployeeProject> selectByEmployeeId(Long employeeId);
    
    /**
     * 查询项目的所有关联关系
     */
    List<EmployeeProject> selectByProjectId(Long projectId);
    
    /**
     * 检查关联是否存在
     */
    int countByEmployeeAndProject(@Param("employeeId") Long employeeId, @Param("projectId") Long projectId);
}
