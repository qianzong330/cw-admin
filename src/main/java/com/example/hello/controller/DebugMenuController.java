package com.example.hello.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DebugMenuController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/debug/menu")
    public String debugMenu() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 菜单调试信息 ===\n\n");
        
        // 1. 查询所有菜单
        sb.append("1. 所有菜单:\n");
        List<Map<String, Object>> allMenus = jdbcTemplate.queryForList(
            "SELECT id, menu_code, menu_name, menu_type, parent_id, status, sort_order FROM tb_menu WHERE status = 1 ORDER BY sort_order"
        );
        for (Map<String, Object> menu : allMenus) {
            sb.append(String.format("   id=%s, code=%s, name=%s, type=%s, parent_id=%s, status=%s\n",
                menu.get("id"), menu.get("menu_code"), menu.get("menu_name"),
                menu.get("menu_type"), menu.get("parent_id"), menu.get("status")));
        }
        
        // 2. 查询工程管理目录
        sb.append("\n2. 工程管理目录 (project_dir):\n");
        try {
            Map<String, Object> projectDir = jdbcTemplate.queryForMap(
                "SELECT id, menu_code, menu_name, menu_type, parent_id, status FROM tb_menu WHERE menu_code = 'project_dir'"
            );
            sb.append(String.format("   id=%s, code=%s, name=%s, type=%s, parent_id=%s, status=%s\n",
                projectDir.get("id"), projectDir.get("menu_code"), projectDir.get("menu_name"),
                projectDir.get("menu_type"), projectDir.get("parent_id"), projectDir.get("status")));
        } catch (Exception e) {
            sb.append("   未找到工程管理目录!\n");
        }
        
        // 3. 查询项目管理菜单
        sb.append("\n3. 项目管理菜单 (project):\n");
        try {
            Map<String, Object> project = jdbcTemplate.queryForMap(
                "SELECT id, menu_code, menu_name, menu_type, parent_id, status FROM tb_menu WHERE menu_code = 'project'"
            );
            sb.append(String.format("   id=%s, code=%s, name=%s, type=%s, parent_id=%s, status=%s\n",
                project.get("id"), project.get("menu_code"), project.get("menu_name"),
                project.get("menu_type"), project.get("parent_id"), project.get("status")));
        } catch (Exception e) {
            sb.append("   未找到项目管理菜单!\n");
        }
        
        // 4. 查询BOSS角色的菜单权限
        sb.append("\n4. BOSS角色的菜单权限:\n");
        try {
            List<Map<String, Object>> bossMenus = jdbcTemplate.queryForList(
                "SELECT m.menu_code, m.menu_name FROM tb_role_menu rm " +
                "JOIN tb_role r ON rm.role_id = r.id " +
                "JOIN tb_menu m ON rm.menu_id = m.id " +
                "WHERE r.role_code = 'boss' AND m.status = 1"
            );
            for (Map<String, Object> menu : bossMenus) {
                sb.append(String.format("   %s - %s\n", menu.get("menu_code"), menu.get("menu_name")));
            }
        } catch (Exception e) {
            sb.append("   查询失败: " + e.getMessage() + "\n");
        }
        
        return sb.toString();
    }
}
