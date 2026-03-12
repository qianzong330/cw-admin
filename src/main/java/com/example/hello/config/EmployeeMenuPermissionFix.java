package com.example.hello.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 员工管理菜单权限修复初始化器（已禁用）
 * 权限分配由用户在角色管理页面手动维护，不再自动初始化
 */
// @Component  // 已禁用，不在启动时自动执行
public class EmployeeMenuPermissionFix {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void run(String... args) {
        try {
            System.out.println("=== 开始修复员工管理菜单权限 ===");
            
            // 1. 查找 employee 菜单的ID
            Long employeeMenuId = null;
            try {
                employeeMenuId = jdbcTemplate.queryForObject(
                    "SELECT id FROM tb_menu WHERE menu_code = 'employee'", Long.class);
            } catch (Exception e) {
                System.out.println("  ✗ 未找到 employee 菜单");
                return;
            }
            
            if (employeeMenuId == null) {
                System.out.println("  ✗ employee 菜单不存在");
                return;
            }
            
            // 2. 将 employee 菜单归入基础配置目录
            Long basicConfigId = null;
            try {
                basicConfigId = jdbcTemplate.queryForObject(
                    "SELECT id FROM tb_menu WHERE menu_code = 'basic_config'", Long.class);
            } catch (Exception e) {
                System.out.println("  ✗ 未找到 basic_config 目录");
            }
            
            if (basicConfigId != null) {
                jdbcTemplate.update(
                    "UPDATE tb_menu SET parent_id = ?, menu_type = 2, sort_order = 3 WHERE id = ?",
                    basicConfigId, employeeMenuId
                );
                System.out.println("  ✓ employee 菜单已归入基础配置目录");
            }
            
            // 3. 给 BOSS 角色分配 employee 菜单权限
            Long bossRoleId = null;
            try {
                bossRoleId = jdbcTemplate.queryForObject(
                    "SELECT id FROM tb_role WHERE role_code = 'boss'", Long.class);
            } catch (Exception e) {
                System.out.println("  ✗ 未找到 boss 角色");
            }
            
            if (bossRoleId != null) {
                jdbcTemplate.update(
                    "INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (?, ?)",
                    bossRoleId, employeeMenuId
                );
                System.out.println("  ✓ BOSS 角色已分配 employee 菜单权限");
            }
            
            // 4. 给 root 角色分配 employee 菜单权限
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (1, ?)",
                employeeMenuId
            );
            System.out.println("  ✓ root 角色已分配 employee 菜单权限");
            
            System.out.println("=== 员工管理菜单权限修复完成 ===");
            
        } catch (Exception e) {
            System.err.println("=== 员工管理菜单权限修复失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
