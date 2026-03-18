package com.example.hello.mapper;

import com.example.hello.entity.EmployeeProjectFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmployeeProjectFlowMapper {
    
    /**
     * 插入流动记录
     */
    int insert(EmployeeProjectFlow flow);
    
    /**
     * 根据项目ID查询流动记录
     */
    List<EmployeeProjectFlow> selectByProjectId(@Param("projectId") Long projectId);
    
    /**
     * 根据员工ID查询流动记录
     */
    List<EmployeeProjectFlow> selectByEmployeeId(@Param("employeeId") Long employeeId);
    
    /**
     * 根据项目和员工查询流动记录
     */
    List<EmployeeProjectFlow> selectByProjectAndEmployee(@Param("projectId") Long projectId, @Param("employeeId") Long employeeId);
}
