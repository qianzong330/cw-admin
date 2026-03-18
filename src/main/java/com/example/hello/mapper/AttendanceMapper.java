package com.example.hello.mapper;

import com.example.hello.entity.Attendance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 考勤记录Mapper
 */
@Mapper
public interface AttendanceMapper {
    
    /**
     * 插入考勤记录
     */
    int insert(Attendance attendance);
    
    /**
     * 更新考勤记录
     */
    int update(Attendance attendance);
    
    /**
     * 根据ID删除考勤记录
     */
    int deleteById(Long id);
    
    /**
     * 根据ID查询考勤记录
     */
    Attendance selectById(Long id);
    
    /**
     * 查询考勤记录列表（带员工姓名）
     */
    List<Attendance> selectList(@Param("employeeId") Long employeeId,
                                 @Param("projectId") Long projectId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);
    
    /**
     * 统计员工在日期范围内的出勤天数
     */
    Integer countAttendanceDays(@Param("employeeId") Long employeeId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);
    
    /**
     * 统计员工在日期范围内的已审批出勤天数
     */
    Integer countApprovedAttendanceDays(@Param("employeeId") Long employeeId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    /**
     * 按考勤类型和加班类型汇总工时
     * attendanceType: 1=出勤, 2=加班
     * overtimeType: null=非加班, 1=工作日加班, 2=休息日加班, 3=节假日加班
     */
    java.math.BigDecimal sumWorkHoursByType(@Param("employeeId") Long employeeId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate,
                                            @Param("attendanceType") Integer attendanceType,
                                            @Param("overtimeType") Integer overtimeType,
                                            @Param("projectId") Long projectId);
    
    /**
     * 检查指定日期是否已有考勤记录
     */
    List<Attendance> selectByEmployeeAndDate(@Param("employeeId") Long employeeId,
                                        @Param("workDate") LocalDate workDate);
    
    /**
     * 根据员工、日期和类型查询考勤记录
     */
    Attendance selectByEmployeeAndDateAndType(@Param("employeeId") Long employeeId,
                                               @Param("workDate") LocalDate workDate,
                                               @Param("attendanceType") Integer attendanceType);
    
    /**
     * 根据员工、项目和月份查询考勤记录
     */
    List<Attendance> selectByEmployeeProjectAndMonth(@Param("employeeId") Long employeeId,
                                                      @Param("projectId") Long projectId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);
    
    /**
     * 根据员工和日期范围查询考勤记录（可按项目过滤）
     */
    List<Attendance> selectByEmployeeAndDateRange(@Param("employeeId") Long employeeId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate,
                                                   @Param("projectId") Long projectId);
    
    /**
     * 根据员工、项目和月份删除考勤记录
     */
    int deleteByEmployeeProjectAndMonth(@Param("employeeId") Long employeeId,
                                        @Param("projectId") Long projectId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);
}
