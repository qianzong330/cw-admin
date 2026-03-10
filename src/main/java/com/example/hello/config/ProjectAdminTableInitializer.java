package com.example.hello.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProjectAdminTableInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            System.out.println("[ProjectAdminTableInitializer] 开始检查tb_project_admin表...");
            
            // 检查表是否存在
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = 'tb_project_admin'", 
                Integer.class
            );
            
            if (count != null && count > 0) {
                System.out.println("[ProjectAdminTableInitializer] tb_project_admin表已存在，跳过创建");
                return;
            }
            
            System.out.println("[ProjectAdminTableInitializer] tb_project_admin表不存在，开始创建...");
            
            // 创建表
            jdbcTemplate.execute(
                "CREATE TABLE tb_project_admin (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "project_id BIGINT NOT NULL COMMENT '项目ID', " +
                "employee_id BIGINT NOT NULL COMMENT '管理员员工ID', " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "UNIQUE KEY uk_project_employee (project_id, employee_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目管理员关联表'"
            );
            
            System.out.println("[ProjectAdminTableInitializer] tb_project_admin表创建成功!");
            
        } catch (Exception e) {
            System.err.println("[ProjectAdminTableInitializer] 初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
