package com.example.hello.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AdminRoleInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            System.out.println("[AdminRoleInitializer] 开始检查admin角色...");
            
            // 检查admin角色是否存在
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_role WHERE role_code = 'admin'", 
                Integer.class
            );
            
            if (count != null && count > 0) {
                System.out.println("[AdminRoleInitializer] admin角色已存在，跳过创建");
                return;
            }
            
            System.out.println("[AdminRoleInitializer] admin角色不存在，开始创建...");
            
            // 创建admin角色
            jdbcTemplate.update(
                "INSERT INTO tb_role (role_code, role_name, remark) VALUES (?, ?, ?)",
                "admin", "项目管理员", "项目管理员角色"
            );
            
            System.out.println("[AdminRoleInitializer] admin角色创建成功!");
            
        } catch (Exception e) {
            System.err.println("[AdminRoleInitializer] 初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
