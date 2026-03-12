package com.example.hello.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 初始化角色表结构（仅检查表结构，不初始化数据）
 * 角色数据由用户在角色管理页面维护
 */
@Component
public class PermissionInitializer implements CommandLineRunner {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public void run(String... args) {
        // 1. 先检查并重建 tb_role表 (防止 EmployeeMapper 查询失败)
        createRoleTableIfNotExist();
            
        // 2. 检查是否需要添加 role_code 字段
        addRoleCodeColumnIfNotExist();
            
        // 注意：角色数据由用户在角色管理页面维护，不再自动初始化
        // 如需默认角色，请在数据库中手动创建
    }
    
    /**
     * 添加 role_code 字段 (如果不存在)
     */
    private void addRoleCodeColumnIfNotExist() {
        try {
            // 检查 role_code 字段是否存在
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_role' AND COLUMN_NAME = 'role_code'",
                Integer.class);
            
            if (count != null && count == 0) {
                // 字段不存在，删除旧表重新创建
                System.out.println("=== 删除旧的 tb_role 表 ===");
                jdbcTemplate.execute("DROP TABLE IF EXISTS tb_role");
                
                // 重新创建表
                createRoleTableIfNotExist();
                
                System.out.println("=== 重新创建 tb_role 表成功 ===");
            } else {
                System.out.println("=== role_code 字段已存在 ===");
            }
        } catch (Exception e) {
            System.err.println("添加 role_code 字段失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建角色表 (如果不存在)
     */
    private void createRoleTableIfNotExist() {
        try {
            // 检查表是否存在
            Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_role'",
                Integer.class);
            
            if (tableCount == null || tableCount == 0) {
                // 表不存在，创建表
                jdbcTemplate.execute(
                    "CREATE TABLE tb_role (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色 ID'," +
                    "role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码'," +
                    "role_name VARCHAR(100) NOT NULL COMMENT '角色名称'," +
                    "remark VARCHAR(200) COMMENT '备注'," +
                    "created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表'");
                System.out.println("=== tb_role 表创建成功 ===");
            } else {
                System.out.println("=== tb_role 表已存在 ===");
            }
        } catch (Exception e) {
            System.err.println("创建 tb_role 表失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}
