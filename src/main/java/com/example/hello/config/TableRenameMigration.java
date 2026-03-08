package com.example.hello.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库表名迁移：将旧表名改为tb_前缀
 */
@Component
@Order(0) // 最先执行
public class TableRenameMigration implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        System.out.println("========== 开始表名迁移 ==========");
        try {
            // 1. 迁移 attendance -> tb_attendance
            renameTable("attendance", "tb_attendance");
            
            // 2. 迁移 salary_slip -> tb_salary
            renameTable("salary_slip", "tb_salary");
            
            // 3. 迁移 salary_item -> tb_salary_item
            renameTable("salary_item", "tb_salary_item");
            
            // 4. 删除旧的 salary_slip 表（如果存在）
            dropOldTable("salary_slip");
            
            System.out.println("========== 表名迁移完成 ==========");
        } catch (Exception e) {
            System.err.println("表名迁移失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void renameTable(String oldName, String newName) {
        try {
            // 检查旧表是否存在
            Integer oldCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, oldName
            );
            
            // 检查新表是否已存在
            Integer newCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, newName
            );
            
            if (oldCount != null && oldCount > 0) {
                // 旧表存在
                if (newCount != null && newCount > 0) {
                    // 新表也存在，删除旧表
                    jdbcTemplate.execute("DROP TABLE IF EXISTS " + oldName);
                    System.out.println("表 " + newName + " 已存在，删除旧表 " + oldName);
                } else {
                    // 新表不存在，重命名
                    jdbcTemplate.execute("RENAME TABLE " + oldName + " TO " + newName);
                    System.out.println("表 " + oldName + " 已重命名为 " + newName);
                }
            } else {
                System.out.println("表 " + oldName + " 不存在，跳过");
            }
        } catch (Exception e) {
            System.err.println("迁移表 " + oldName + " 失败: " + e.getMessage());
        }
    }
    
    private void dropOldTable(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, tableName
            );
            if (count != null && count > 0) {
                jdbcTemplate.execute("DROP TABLE IF EXISTS " + tableName);
                System.out.println("已删除旧表 " + tableName);
            }
        } catch (Exception e) {
            System.err.println("删除旧表 " + tableName + " 失败: " + e.getMessage());
        }
    }
}
