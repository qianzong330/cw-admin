package com.example.hello.mapper;

import com.example.hello.entity.AttendanceChangeLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 考勤修改记录Mapper
 */
@Mapper
public interface AttendanceChangeLogMapper {
    
    /**
     * 插入修改记录
     */
    @Insert("INSERT INTO tb_attendance_change_log (" +
            "attendance_id, project_id, employee_id, work_date, `year_month`, change_type, " +
            "old_attendance_type, old_overtime_type, old_work_hours, old_remark, " +
            "new_attendance_type, new_overtime_type, new_work_hours, new_remark, " +
            "change_reason, status, created_by, created_by_name, created_time) " +
            "VALUES (" +
            "#{attendanceId}, #{projectId}, #{employeeId}, #{workDate}, #{yearMonth}, #{changeType}, " +
            "#{oldAttendanceType}, #{oldOvertimeType}, #{oldWorkHours}, #{oldRemark}, " +
            "#{newAttendanceType}, #{newOvertimeType}, #{newWorkHours}, #{newRemark}, " +
            "#{changeReason}, #{status}, #{createdBy}, #{createdByName}, #{createdTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AttendanceChangeLog log);
    
    /**
     * 根据ID查询
     */
    @Select("SELECT l.*, e.name as employee_name " +
            "FROM tb_attendance_change_log l " +
            "LEFT JOIN tb_employee e ON l.employee_id = e.id " +
            "WHERE l.id = #{id}")
    AttendanceChangeLog selectById(Long id);
    
    /**
     * 查询考勤记录的待审批修改记录
     */
    @Select("SELECT l.*, e.name as employee_name " +
            "FROM tb_attendance_change_log l " +
            "LEFT JOIN tb_employee e ON l.employee_id = e.id " +
            "WHERE l.attendance_id = #{attendanceId} AND l.status = 0 " +
            "ORDER BY l.created_time DESC " +
            "LIMIT 1")
    AttendanceChangeLog selectPendingByAttendanceId(Long attendanceId);
    
    /**
     * 查询员工某日期某类型的待审批ADD记录
     */
    @Select("SELECT * FROM tb_attendance_change_log " +
            "WHERE employee_id = #{employeeId} AND work_date = #{workDate} " +
            "AND new_attendance_type = #{attendanceType} AND change_type = 'ADD' AND status = 0 " +
            "ORDER BY created_time DESC LIMIT 1")
    AttendanceChangeLog selectPendingAddByEmployeeDateAndType(@Param("employeeId") Long employeeId,
                                                               @Param("workDate") LocalDate workDate,
                                                               @Param("attendanceType") Integer attendanceType);
    
    /**
     * 查询项目和月份的待审批修改记录
     */
    @Select("SELECT l.*, e.name as employee_name " +
            "FROM tb_attendance_change_log l " +
            "LEFT JOIN tb_employee e ON l.employee_id = e.id " +
            "WHERE (l.project_id = #{projectId} OR (l.project_id IS NULL AND #{projectId} IS NULL)) AND l.`year_month` = #{yearMonth} AND l.status = 0 " +
            "ORDER BY l.created_time DESC")
    List<AttendanceChangeLog> selectPendingByProjectAndMonth(@Param("projectId") Long projectId, 
                                                               @Param("yearMonth") String yearMonth);
    
    /**
     * 查询项目和月份的所有修改记录
     */
    @Select("SELECT l.*, e.name as employee_name " +
            "FROM tb_attendance_change_log l " +
            "LEFT JOIN tb_employee e ON l.employee_id = e.id " +
            "WHERE (l.project_id = #{projectId} OR (l.project_id IS NULL AND #{projectId} IS NULL)) AND l.`year_month` = #{yearMonth} " +
            "ORDER BY l.created_time DESC")
    List<AttendanceChangeLog> selectByProjectAndMonth(@Param("projectId") Long projectId, 
                                                        @Param("yearMonth") String yearMonth);
    
    /**
     * 更新审批状态
     */
    @Update("UPDATE tb_attendance_change_log SET " +
            "status = #{status}, " +
            "approved_by = #{approvedBy}, " +
            "approved_by_name = #{approvedByName}, " +
            "approved_time = #{approvedTime}, " +
            "approve_remark = #{approveRemark} " +
            "WHERE id = #{id}")
    int updateApproveStatus(AttendanceChangeLog log);
    
    /**
     * 批量更新审批状态（按项目和月份）
     */
    @Update("UPDATE tb_attendance_change_log SET " +
            "status = #{status}, " +
            "approved_by = #{approvedBy}, " +
            "approved_by_name = #{approvedByName}, " +
            "approved_time = #{approvedTime}, " +
            "approve_remark = #{approveRemark} " +
            "WHERE project_id = #{projectId} AND `year_month` = #{yearMonth} AND status = 0")
    int batchUpdateApproveStatus(@Param("projectId") Long projectId,
                                  @Param("yearMonth") String yearMonth,
                                  @Param("status") Integer status,
                                  @Param("approvedBy") Long approvedBy,
                                  @Param("approvedByName") String approvedByName,
                                  @Param("approvedTime") java.time.LocalDateTime approvedTime,
                                  @Param("approveRemark") String approveRemark);
    
    /**
     * 更新修改记录
     */
    @Update("UPDATE tb_attendance_change_log SET " +
            "old_attendance_type = #{oldAttendanceType}, " +
            "old_overtime_type = #{oldOvertimeType}, " +
            "old_work_hours = #{oldWorkHours}, " +
            "old_remark = #{oldRemark}, " +
            "new_attendance_type = #{newAttendanceType}, " +
            "new_overtime_type = #{newOvertimeType}, " +
            "new_work_hours = #{newWorkHours}, " +
            "new_remark = #{newRemark}, " +
            "change_type = #{changeType}, " +
            "change_reason = #{changeReason}, " +
            "created_time = #{createdTime} " +
            "WHERE id = #{id}")
    int update(AttendanceChangeLog log);
    
    /**
     * 删除记录
     */
    @Delete("DELETE FROM tb_attendance_change_log WHERE id = #{id}")
    int deleteById(Long id);
    
    /**
     * 删除员工某月份的所有变更日志（用于导入前清空）
     */
    @Delete("DELETE FROM tb_attendance_change_log " +
            "WHERE employee_id = #{employeeId} AND (project_id = #{projectId} OR (project_id IS NULL AND #{projectId} IS NULL)) AND `year_month` = #{yearMonth}")
    int deleteByEmployeeProjectAndMonth(@Param("employeeId") Long employeeId,
                                         @Param("projectId") Long projectId,
                                         @Param("yearMonth") String yearMonth);
    
    /**
     * 查询是否存在待审批的修改记录
     */
    @Select("SELECT COUNT(*) FROM tb_attendance_change_log " +
            "WHERE (project_id = #{projectId} OR (project_id IS NULL AND #{projectId} IS NULL)) AND `year_month` = #{yearMonth} AND status = 0")
    int countPendingByProjectAndMonth(@Param("projectId") Long projectId, 
                                        @Param("yearMonth") String yearMonth);
    
    /**
     * 查询所有变更记录数量（包括已审批和已驳回的）
     */
    @Select("SELECT COUNT(*) FROM tb_attendance_change_log " +
            "WHERE (project_id = #{projectId} OR (project_id IS NULL AND #{projectId} IS NULL)) AND `year_month` = #{yearMonth}")
    int countByProjectAndMonth(@Param("projectId") Long projectId, 
                                @Param("yearMonth") String yearMonth);
}
