package com.example.hello.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import java.util.List;
import java.util.Map;

/**
 * 修复考勤数据：将所有状态改为可编辑状态(status=null)
 */
@Component
@Order(1) // 确保最先执行
public class AttendanceDataFix implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        System.out.println("========== 开始修复考勤数据 ==========");
        try {
            // 先查询当前有多少条记录
            Integer totalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_attendance", 
                Integer.class
            );
            System.out.println("考勤表总记录数: " + totalCount);
            
            // 查询有多少条有状态的记录(status不为null)
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_attendance WHERE status IS NOT NULL", 
                Integer.class
            );
            System.out.println("需要修复的记录数(status不为null): " + count);
            
            // 查询有多少条status=1的记录
            Integer pendingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_attendance WHERE status = 1", 
                Integer.class
            );
            System.out.println("待审批状态记录数(status=1): " + pendingCount);
            
            // 查询有多少条status=2的记录
            Integer approvedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_attendance WHERE status = 2", 
                Integer.class
            );
            System.out.println("已审批状态记录数(status=2): " + approvedCount);
            
            // 将status=1(待审批)的改为status=2(已审批)，保持已审批状态不变
            int updated = jdbcTemplate.update(
                "UPDATE tb_attendance SET status = 2 WHERE status = 1"
            );
            System.out.println("已修复 " + updated + " 条考勤记录，待审批状态改为已审批");
            
            // 清理互斥的考勤数据（同一天既有请假/缺勤又有出勤/加班的，只保留请假/缺勤）
            System.out.println("开始清理互斥的考勤数据...");
            
            // 查询所有有冲突的日期
            List<Map<String, Object>> conflicts = jdbcTemplate.queryForList(
                "SELECT employee_id, work_date FROM tb_attendance GROUP BY employee_id, work_date HAVING " +
                "SUM(CASE WHEN attendance_type IN (3,4) THEN 1 ELSE 0 END) > 0 AND " +
                "SUM(CASE WHEN attendance_type IN (1,2) THEN 1 ELSE 0 END) > 0"
            );
            
            System.out.println("发现 " + conflicts.size() + " 个日期有互斥考勤记录");
            
            int deleted = 0;
            for (Map<String, Object> conflict : conflicts) {
                Long empId = ((Number) conflict.get("employee_id")).longValue();
                java.sql.Date workDate = (java.sql.Date) conflict.get("work_date");
                
                // 删除该日期下的出勤/加班记录，保留请假/缺勤
                int del = jdbcTemplate.update(
                    "DELETE FROM tb_attendance WHERE employee_id = ? AND work_date = ? AND attendance_type IN (1,2)",
                    empId, workDate
                );
                deleted += del;
            }
            
            System.out.println("已删除 " + deleted + " 条互斥的出勤/加班记录");
            System.out.println("========== 考勤数据修复完成 ==========");
        } catch (Exception e) {
            System.err.println("修复考勤数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
