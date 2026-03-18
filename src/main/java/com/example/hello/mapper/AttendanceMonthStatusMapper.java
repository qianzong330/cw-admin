package com.example.hello.mapper;

import com.example.hello.entity.AttendanceMonthStatus;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AttendanceMonthStatusMapper {
    
    @Select("SELECT s.*, p.name AS projectName, e.name AS submitByName " +
            "FROM tb_attendance_month_status s " +
            "LEFT JOIN tb_project p ON s.project_id = p.id " +
            "LEFT JOIN tb_employee e ON s.submit_by = e.id " +
            "WHERE s.`year_month` = #{yearMonth} AND (s.project_id = #{projectId} OR (s.project_id IS NULL AND #{projectId} IS NULL))")
    AttendanceMonthStatus findByYearMonthAndProject(@Param("yearMonth") String yearMonth, @Param("projectId") Long projectId);
    
    @Select("SELECT s.*, p.name AS projectName, e.name AS submitByName " +
            "FROM tb_attendance_month_status s " +
            "LEFT JOIN tb_project p ON s.project_id = p.id " +
            "LEFT JOIN tb_employee e ON s.submit_by = e.id " +
            "WHERE s.status IN (1, 2) " +
            "ORDER BY s.submit_time DESC")
    List<AttendanceMonthStatus> findPendingList();
    
    @Insert("INSERT INTO tb_attendance_month_status (`year_month`, project_id, status, submit_by, submit_time, submit_remark, approve_by, approve_time, approve_remark) " +
            "VALUES (#{yearMonth}, #{projectId}, #{status}, #{submitBy}, #{submitTime}, #{submitRemark}, #{approveBy}, #{approveTime}, #{approveRemark}) " +
            "ON DUPLICATE KEY UPDATE status = #{status}, submit_by = #{submitBy}, submit_time = #{submitTime}, " +
            "submit_remark = #{submitRemark}, approve_by = #{approveBy}, approve_time = #{approveTime}, approve_remark = #{approveRemark}")
    int saveOrUpdate(AttendanceMonthStatus status);
    
    @Update("UPDATE tb_attendance_month_status SET status = #{status}, approve_by = #{approveBy}, approve_time = #{approveTime}, approve_remark = #{approveRemark} " +
            "WHERE `year_month` = #{yearMonth} AND (project_id = #{projectId} OR (project_id IS NULL AND #{projectId} IS NULL))")
    int approve(AttendanceMonthStatus status);
    
    @Update("UPDATE tb_attendance_month_status SET sign_image_urls = #{signImageUrls} " +
            "WHERE `year_month` = #{yearMonth} AND (project_id = #{projectId} OR (project_id IS NULL AND #{projectId} IS NULL))")
    int updateSignImageUrls(@Param("yearMonth") String yearMonth,
                            @Param("projectId") Long projectId,
                            @Param("signImageUrls") String signImageUrls);
    
}