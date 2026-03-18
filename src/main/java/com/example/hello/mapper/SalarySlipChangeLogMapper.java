package com.example.hello.mapper;

import com.example.hello.entity.SalarySlipChangeLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 工资条修改记录Mapper
 */
@Mapper
public interface SalarySlipChangeLogMapper {
    
    /**
     * 插入修改记录
     */
    @Insert("INSERT INTO tb_salary_slip_change_log (" +
            "salary_slip_id, project_id, employee_id, salary_period, " +
            "old_attendance_days, old_base_salary, old_base_amount, " +
            "old_addition_amount, old_deduction_amount, old_payable_amount, " +
            "new_attendance_days, new_base_salary, new_base_amount, " +
            "new_addition_amount, new_deduction_amount, new_payable_amount, " +
            "change_reason, created_by, created_by_name, created_time, " +
            "change_type, fee_items_change_detail, old_fee_items_json, new_fee_items_json) " +
            "VALUES (" +
            "#{salarySlipId}, #{projectId}, #{employeeId}, #{salaryPeriod}, " +
            "#{oldAttendanceDays}, #{oldBaseSalary}, #{oldBaseAmount}, " +
            "#{oldAdditionAmount}, #{oldDeductionAmount}, #{oldPayableAmount}, " +
            "#{newAttendanceDays}, #{newBaseSalary}, #{newBaseAmount}, " +
            "#{newAdditionAmount}, #{newDeductionAmount}, #{newPayableAmount}, " +
            "#{changeReason}, #{createdBy}, #{createdByName}, #{createdTime}, " +
            "#{changeType}, #{feeItemsChangeDetail}, #{oldFeeItemsJson}, #{newFeeItemsJson})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SalarySlipChangeLog log);
    
    /**
     * 根据ID查询
     */
    @Select("SELECT l.*, e.name as employee_name " +
            "FROM tb_salary_slip_change_log l " +
            "LEFT JOIN tb_employee e ON l.employee_id = e.id " +
            "WHERE l.id = #{id}")
    SalarySlipChangeLog selectById(Long id);
    
    /**
     * 查询工资条的修改记录
     */
    @Select("SELECT l.*, e.name as employee_name " +
            "FROM tb_salary_slip_change_log l " +
            "LEFT JOIN tb_employee e ON l.employee_id = e.id " +
            "WHERE l.salary_slip_id = #{salarySlipId} " +
            "ORDER BY l.created_time DESC " +
            "LIMIT 1")
    SalarySlipChangeLog selectBySalarySlipId(Long salarySlipId);
    
    /**
     * 查询项目和月份的修改记录
     */
    @Select("SELECT l.*, e.name as employee_name " +
            "FROM tb_salary_slip_change_log l " +
            "LEFT JOIN tb_employee e ON l.employee_id = e.id " +
            "WHERE l.project_id = #{projectId} AND l.salary_period = #{salaryPeriod} " +
            "ORDER BY l.created_time DESC")
    List<SalarySlipChangeLog> selectByProjectAndPeriod(@Param("projectId") Long projectId, 
                                                         @Param("salaryPeriod") String salaryPeriod);
    
    /**
     * 删除记录
     */
    @Delete("DELETE FROM tb_salary_slip_change_log WHERE id = #{id}")
    int deleteById(Long id);
    
    /**
     * 按项目和月份删除变更记录
     */
    @Delete("DELETE FROM tb_salary_slip_change_log " +
            "WHERE project_id = #{projectId} AND salary_period = #{salaryPeriod}")
    int deleteByProjectAndPeriod(@Param("projectId") Long projectId, 
                                  @Param("salaryPeriod") String salaryPeriod);
    
    /**
     * 查询是否存在变更记录
     */
    @Select("SELECT COUNT(*) FROM tb_salary_slip_change_log " +
            "WHERE project_id = #{projectId} AND salary_period = #{salaryPeriod}")
    int countByProjectAndPeriod(@Param("projectId") Long projectId, 
                                 @Param("salaryPeriod") String salaryPeriod);
    
    /**
     * 检查工资条是否有变更
     */
    @Select("SELECT COUNT(*) FROM tb_salary_slip_change_log " +
            "WHERE salary_slip_id = #{salarySlipId}")
    int countBySalarySlipId(Long salarySlipId);
    
    /**
     * 查询工资条的所有变更记录（用于抵消计算）
     */
    @Select("SELECT * FROM tb_salary_slip_change_log " +
            "WHERE project_id = #{projectId} AND salary_period = #{salaryPeriod} " +
            "ORDER BY created_time ASC")
    List<SalarySlipChangeLog> selectAllByProjectAndPeriod(@Param("projectId") Long projectId, 
                                                           @Param("salaryPeriod") String salaryPeriod);
    
    /**
     * 查询工资条费用项的变更记录
     */
    @Select("SELECT * FROM tb_salary_slip_change_log " +
            "WHERE salary_slip_id = #{salarySlipId} " +
            "AND fee_items_change_detail LIKE CONCAT('%\"itemId\":', #{itemId}, '%') " +
            "ORDER BY created_time DESC LIMIT 1")
    SalarySlipChangeLog selectLatestBySlipAndItem(@Param("salarySlipId") Long salarySlipId, 
                                                   @Param("itemId") Long itemId);
    
    /**
     * 删除工资条的所有变更记录
     */
    @Delete("DELETE FROM tb_salary_slip_change_log WHERE salary_slip_id = #{salarySlipId}")
    int deleteBySalarySlipId(Long salarySlipId);
}
