package com.example.hello.mapper;

import com.example.hello.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmployeeMapper {
    
    Employee findById(Long id);
    
    Employee findByName(@Param("name") String name);
    
    Employee findByPhone(@Param("phone") String phone);
    
    Employee findByNameAndPassword(@Param("name") String name, @Param("password") String password);
    
    /**
     * 根据员工 ID 查询角色信息
     */
    java.util.Map<String, Object> findRoleByEmployeeId(@Param("employeeId") Long employeeId);
    
    List<Employee> findAll();

    List<Employee> findExcludeRoot();
    
    List<Employee> findByRoleCode(String roleCode);
    
    List<Employee> findFinanceList();
    
    /**
     * 根据项目ID查询员工列表
     */
    List<Employee> findByProjectId(@Param("projectId") Long projectId);
    
    /**
     * 查询员工隶属的项目名称列表
     */
    List<String> findProjectNamesByEmployeeId(@Param("employeeId") Long employeeId);
    
    /**
     * 查询员工隶属的项目 ID 列表
     */
    List<Long> findProjectIdsByEmployeeId(@Param("employeeId") Long employeeId);
    
    int insert(Employee employee);
    
    int update(Employee employee);
    
    int deleteById(Long id);
}
