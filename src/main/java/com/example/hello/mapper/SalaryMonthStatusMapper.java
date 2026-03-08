package com.example.hello.mapper;

import com.example.hello.entity.SalaryMonthStatus;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SalaryMonthStatusMapper {

    @Select("SELECT * FROM tb_salary_month_status WHERE `year_month` = #{yearMonth} AND project_id = #{projectId}")
    SalaryMonthStatus findByYearMonthAndProject(@Param("yearMonth") String yearMonth, @Param("projectId") Long projectId);
    
    @Select("SELECT COUNT(*) FROM tb_salary_month_status WHERE `year_month` = #{yearMonth} AND status = 5")
    int countApprovedByYearMonth(@Param("yearMonth") String yearMonth);

    @Insert("INSERT INTO tb_salary_month_status (`year_month`, project_id, status, submit_by, submit_time, submit_remark) " +
            "VALUES (#{yearMonth}, #{projectId}, #{status}, #{submitBy}, #{submitTime}, #{submitRemark}) " +
            "ON DUPLICATE KEY UPDATE status = #{status}, submit_by = #{submitBy}, submit_time = #{submitTime}, submit_remark = #{submitRemark}")
    int insertOrUpdate(SalaryMonthStatus record);

    @Update("UPDATE tb_salary_month_status SET status = #{status}, approve_by = #{approveBy}, approve_time = #{approveTime}, approve_remark = #{approveRemark} " +
            "WHERE `year_month` = #{yearMonth} AND project_id = #{projectId}")
    int updateStatus(SalaryMonthStatus record);

    @Select("SELECT COUNT(*) FROM tb_salary_month_status WHERE status = #{status}")
    int countByStatus(@Param("status") int status);
}
