package com.example.hello.mapper;

import com.example.hello.entity.ProjectEmployeeSalary;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目员工薪资历史 Mapper
 */
@Mapper
public interface ProjectEmployeeSalaryMapper {
    
    /**
     * 插入薪资记录
     */
    @Insert("INSERT INTO tb_project_employee_salary (project_id, employee_id, salary_amount, " +
            "salary_type, effective_date, created_by, remark) " +
            "VALUES (#{projectId}, #{employeeId}, #{salaryAmount}, #{salaryType}, " +
            "#{effectiveDate}, #{createdBy}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProjectEmployeeSalary salary);
    
    /**
     * 根据ID查询
     */
    @Select("SELECT * FROM tb_project_employee_salary WHERE id = #{id}")
    ProjectEmployeeSalary selectById(Long id);
    
    /**
     * 查询项目员工的所有薪资历史（按生效日期倒序）
     */
    @Select("SELECT s.*, e.name as employee_name, p.name as project_name, " +
            "op.name as created_by_name " +
            "FROM tb_project_employee_salary s " +
            "LEFT JOIN tb_employee e ON s.employee_id = e.id " +
            "LEFT JOIN tb_project p ON s.project_id = p.id " +
            "LEFT JOIN tb_employee op ON s.created_by = op.id " +
            "WHERE s.project_id = #{projectId} AND s.employee_id = #{employeeId} " +
            "ORDER BY s.effective_date DESC, s.created_time DESC")
    List<ProjectEmployeeSalary> selectByProjectAndEmployee(@Param("projectId") Long projectId, 
                                                            @Param("employeeId") Long employeeId);
    
    /**
     * 查询项目的所有员工薪资历史
     */
    @Select("SELECT s.*, e.name as employee_name, p.name as project_name, " +
            "op.name as created_by_name " +
            "FROM tb_project_employee_salary s " +
            "LEFT JOIN tb_employee e ON s.employee_id = e.id " +
            "LEFT JOIN tb_project p ON s.project_id = p.id " +
            "LEFT JOIN tb_employee op ON s.created_by = op.id " +
            "WHERE s.project_id = #{projectId} " +
            "ORDER BY s.employee_id, s.effective_date DESC")
    List<ProjectEmployeeSalary> selectByProject(@Param("projectId") Long projectId);
    
    /**
     * 查询指定日期生效的薪资（用于工资条计算）
     * 取生效日期 <= 指定日期的最新一条记录
     */
    @Select("SELECT s.*, e.name as employee_name " +
            "FROM tb_project_employee_salary s " +
            "LEFT JOIN tb_employee e ON s.employee_id = e.id " +
            "WHERE s.project_id = #{projectId} AND s.employee_id = #{employeeId} " +
            "AND s.effective_date <= #{date} " +
            "ORDER BY s.effective_date DESC, s.created_time DESC " +
            "LIMIT 1")
    ProjectEmployeeSalary selectEffectiveSalary(@Param("projectId") Long projectId,
                                                 @Param("employeeId") Long employeeId,
                                                 @Param("date") LocalDate date);
    
    /**
     * 查询员工在项目中的最新薪资
     */
    @Select("SELECT s.*, e.name as employee_name " +
            "FROM tb_project_employee_salary s " +
            "LEFT JOIN tb_employee e ON s.employee_id = e.id " +
            "WHERE s.project_id = #{projectId} AND s.employee_id = #{employeeId} " +
            "ORDER BY s.effective_date DESC, s.created_time DESC " +
            "LIMIT 1")
    ProjectEmployeeSalary selectLatestSalary(@Param("projectId") Long projectId,
                                              @Param("employeeId") Long employeeId);
    
    /**
     * 删除薪资记录
     */
    @Delete("DELETE FROM tb_project_employee_salary WHERE id = #{id}")
    int deleteById(Long id);
    
    /**
     * 查询项目员工在指定生效日期的记录数（用于判断重复）
     */
    @Select("SELECT COUNT(*) FROM tb_project_employee_salary " +
            "WHERE project_id = #{projectId} AND employee_id = #{employeeId} " +
            "AND effective_date = #{effectiveDate}")
    int countByEffectiveDate(@Param("projectId") Long projectId,
                              @Param("employeeId") Long employeeId,
                              @Param("effectiveDate") LocalDate effectiveDate);
}
