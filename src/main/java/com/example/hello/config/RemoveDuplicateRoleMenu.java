package com.example.hello.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 删除重复的权限分配菜单（基础配置下的），只保留系统设置下的角色管理
 */
@Component
public class RemoveDuplicateRoleMenu implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            // 查找所有 menu_code = 'role' 的菜单
            List<Map<String, Object>> roleMenus = jdbcTemplate.queryForList(
                "SELECT id, menu_name, parent_id FROM tb_menu WHERE menu_code = 'role'"
            );
            
            if (roleMenus.isEmpty()) {
                System.out.println("=== 没有找到 role 菜单，无需删除 ===");
                return;
            }
            
            System.out.println("=== 发现 " + roleMenus.size() + " 个 role 菜单 ===");
            for (Map<String, Object> menu : roleMenus) {
                Long menuId = ((Number) menu.get("id")).longValue();
                String menuName = (String) menu.get("menu_name");
                Long parentId = menu.get("parent_id") != null ? ((Number) menu.get("parent_id")).longValue() : 0L;
                
                System.out.println("  - ID: " + menuId + ", 名称: " + menuName + ", 父ID: " + parentId);
                
                // 删除该菜单的角色权限关联
                jdbcTemplate.update("DELETE FROM tb_role_menu WHERE menu_id = ?", menuId);
                System.out.println("    已删除角色关联");
                
                // 删除该菜单
                jdbcTemplate.update("DELETE FROM tb_menu WHERE id = ?", menuId);
                System.out.println("    已删除菜单: " + menuName);
            }
            
            System.out.println("=== 已清理所有 role 菜单，只保留系统设置下的硬编码菜单 ===");
        } catch (Exception e) {
            System.err.println("删除权限分配菜单失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
