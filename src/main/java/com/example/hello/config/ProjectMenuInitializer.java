package com.example.hello.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProjectMenuInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            System.out.println("[ProjectMenuInitializer] 开始检查项目管理菜单...");
            
            // 1. 检查项目管理菜单是否存在
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_menu WHERE menu_code = 'project'", 
                Integer.class
            );
            
            if (count != null && count > 0) {
                System.out.println("[ProjectMenuInitializer] 项目管理菜单已存在，跳过创建");
                return;
            }
            
            System.out.println("[ProjectMenuInitializer] 项目管理菜单不存在，开始创建...");
            
            // 2. 获取工程管理目录的ID
            Long projectDirId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_menu WHERE menu_code = 'project_dir'",
                Long.class
            );
            
            if (projectDirId == null) {
                System.err.println("[ProjectMenuInitializer] 错误：找不到工程管理目录!");
                return;
            }
            
            System.out.println("[ProjectMenuInitializer] 工程管理目录ID: " + projectDirId);
            
            // 3. 创建项目管理菜单
            jdbcTemplate.update(
                "INSERT INTO tb_menu (menu_code, menu_name, menu_type, menu_url, menu_icon, parent_id, sort_order, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                "project", "项目管理", 2, "/project/list", "bi-folder", projectDirId, 1, 1
            );
            
            // 4. 获取新创建的项目管理菜单ID
            Long projectId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_menu WHERE menu_code = 'project'",
                Long.class
            );
            
            System.out.println("[ProjectMenuInitializer] 项目管理菜单创建成功，ID: " + projectId);
            
            // 5. 给各角色分配权限
            // BOSS角色
            Long bossId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_role WHERE role_code = 'boss'",
                Long.class
            );
            if (bossId != null) {
                jdbcTemplate.update(
                    "INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (?, ?)",
                    bossId, projectId
                );
                System.out.println("[ProjectMenuInitializer] 已给BOSS角色分配权限");
            }
            
            // 财务角色
            Long financeId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_role WHERE role_code = 'finance'",
                Long.class
            );
            if (financeId != null) {
                jdbcTemplate.update(
                    "INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (?, ?)",
                    financeId, projectId
                );
                System.out.println("[ProjectMenuInitializer] 已给财务角色分配权限");
            }
            
            // 员工角色
            Long employeeId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_role WHERE role_code = 'employee'",
                Long.class
            );
            if (employeeId != null) {
                jdbcTemplate.update(
                    "INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (?, ?)",
                    employeeId, projectId
                );
                System.out.println("[ProjectMenuInitializer] 已给员工角色分配权限");
            }
            
            System.out.println("[ProjectMenuInitializer] 项目管理菜单初始化完成!");
            
        } catch (Exception e) {
            System.err.println("[ProjectMenuInitializer] 初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
