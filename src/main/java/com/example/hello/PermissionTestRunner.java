package com.example.hello;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PermissionTestRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public PermissionTestRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n========== 财务角色权限检查 ==========");
        
        String sql = """
            SELECT r.role_code, r.role_name, m.menu_code, m.menu_name, m.menu_type
            FROM tb_role r 
            LEFT JOIN tb_role_menu rm ON r.id = rm.role_id 
            LEFT JOIN tb_menu m ON rm.menu_id = m.id 
            WHERE r.role_code = 'finance' 
            ORDER BY m.sort_order
        """;
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        
        System.out.println("角色编码 | 角色名称 | 菜单编码 | 菜单名称 | 类型");
        System.out.println("--------|---------|---------|---------|-----");
        
        for (Map<String, Object> row : results) {
            String menuType = row.get("menu_type") != null ? row.get("menu_type").toString() : "";
            String typeStr = switch (menuType) {
                case "1" -> "[目录]";
                case "2" -> "[菜单]";
                case "3" -> "[按钮]";
                default -> "[未知]";
            };
            
            String roleCode = row.get("role_code") != null ? row.get("role_code").toString() : "";
            String roleName = row.get("role_name") != null ? row.get("role_name").toString() : "";
            String menuCode = row.get("menu_code") != null ? row.get("menu_code").toString() : "";
            String menuName = row.get("menu_name") != null ? row.get("menu_name").toString() : "";
            
            System.out.printf("%-8s| %-9s| %-25s| %-20s| %s%n", 
                roleCode, roleName, menuCode, menuName, typeStr);
        }
        
        System.out.println("\n总计：" + results.size() + " 个权限");
        System.out.println("======================================\n");
    }
}
