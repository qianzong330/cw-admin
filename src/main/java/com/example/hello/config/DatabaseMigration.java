package com.example.hello.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigration implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        migrateAttendanceTable();
        migrateSalarySlipTable();
    }
    
    private void migrateAttendanceTable() {
        // 检查并添加 overtime_type 字段
        addColumnIfNotExists("attendance", "overtime_type", "TINYINT", "加班类型：1-工作日加班，2-休息日加班，3-法定假期加班", "attendance_type");
        
        // 删除 tb_attendance 的无用字段（状态由月份状态表控制）
        dropColumnIfExists("attendance", "status");
        dropColumnIfExists("attendance", "approved_by");
        dropColumnIfExists("attendance", "approved_time");
    }
    
    private void dropColumnIfExists(String tableName, String columnName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'accounting' AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName, columnName
            );
            if (count != null && count > 0) {
                jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP COLUMN " + columnName);
                System.out.println("成功删除 " + tableName + "." + columnName + " 字段");
            }
        } catch (Exception e) {
            System.err.println("删除 " + columnName + " 字段失败: " + e.getMessage());
        }
    }
    
    private void migrateSalarySlipTable() {
        // 工资条表已改为月份状态控制，无需单个工资条审批字段

        // 1. 添加 project_id 字段（如不存在）
        addColumnIfNotExists("tb_salary", "project_id", "BIGINT DEFAULT NULL COMMENT '项目ID'", "项目ID", "created_by");

        // 2. 为现有工资条补充 project_id（通过员工-项目关联表）
        try {
            jdbcTemplate.execute(
                "UPDATE tb_salary s SET project_id = (" +
                "  SELECT ep.project_id FROM tb_employee_project ep" +
                "  WHERE ep.employee_id = s.employee_id LIMIT 1" +
                ") WHERE s.project_id IS NULL"
            );
        } catch (Exception e) {
            System.err.println("补充 tb_salary.project_id 失败: " + e.getMessage());
        }

        // 3. 将唯一约束从 (employee_id, salary_period) 改为 (employee_id, salary_period, project_id)
        try {
            Integer oldIndexExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS" +
                " WHERE TABLE_SCHEMA='accounting' AND TABLE_NAME='tb_salary' AND INDEX_NAME='uk_employee_period'",
                Integer.class
            );
            Integer newIndexExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS" +
                " WHERE TABLE_SCHEMA='accounting' AND TABLE_NAME='tb_salary' AND INDEX_NAME='uk_employee_period_project'",
                Integer.class
            );
            if (oldIndexExists != null && oldIndexExists > 0) {
                jdbcTemplate.execute("ALTER TABLE tb_salary DROP INDEX uk_employee_period");
                System.out.println("已删除旧唯一约束 uk_employee_period");
            }
            if (newIndexExists == null || newIndexExists == 0) {
                jdbcTemplate.execute(
                    "ALTER TABLE tb_salary ADD UNIQUE KEY uk_employee_period_project (employee_id, salary_period, project_id)"
                );
                System.out.println("已创建新唯一约束 uk_employee_period_project");
            }
        } catch (Exception e) {
            System.err.println("修改 tb_salary 唯一约束失败: " + e.getMessage());
        }
    }
    
    private void addColumnIfNotExists(String tableName, String columnName, String columnDef, String comment, String afterColumn) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'accounting' AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName, columnName
            );
            if (count != null && count > 0) {
                System.out.println(tableName + "." + columnName + " 字段已存在，跳过");
            } else {
                jdbcTemplate.execute(
                    "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDef + " COMMENT '" + comment + "' AFTER " + afterColumn
                );
                System.out.println("成功添加 " + columnName + " 字段到 " + tableName + " 表");
            }
        } catch (Exception e) {
            System.err.println("添加 " + columnName + " 字段失败: " + e.getMessage());
        }
    }
}
