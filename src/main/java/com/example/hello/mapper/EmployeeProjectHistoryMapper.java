package com.example.hello.mapper;

import com.example.hello.entity.EmployeeProjectHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmployeeProjectHistoryMapper {
    
    /**
     * 插入历史记录
     */
    int insert(EmployeeProjectHistory history);
    
    /**
     * 根据项目ID查询历史记录
     */
    List<EmployeeProjectHistory> selectByProjectId(@Param("projectId") Long projectId);
    
    /**
     * 根据员工ID和项目ID查询历史记录
     */
    EmployeeProjectHistory selectByEmployeeAndProject(@Param("employeeId") Long employeeId, @Param("projectId") Long projectId);
}
