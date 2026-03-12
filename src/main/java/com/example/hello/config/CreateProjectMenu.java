package com.example.hello.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 创建工程目录一级菜单，并将独立页面移到该目录下
 * 同时修复顶层菜单 sort_order，确保与子菜单数字空间不冲突
 */
@Component
public class CreateProjectMenu implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            System.out.println("=== 开始创建工程目录菜单 ===");

            // 1. 检查是否已存在工程目录
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_menu WHERE menu_code = 'project_dir'",
                Integer.class
            );

            Long projectDirId;
            if (count != null && count > 0) {
                // 已存在，获取ID
                projectDirId = jdbcTemplate.queryForObject(
                    "SELECT id FROM tb_menu WHERE menu_code = 'project_dir'",
                    Long.class
                );
                System.out.println("=== 工程目录菜单已存在，ID: " + projectDirId + " ===");
            } else {
                // 2. 创建工程目录一级菜单
                jdbcTemplate.update(
                    "INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    "project_dir", "工程目录", 1, null, "bi-folder", "", 0, 1
                );
                
                projectDirId = jdbcTemplate.queryForObject(
                    "SELECT id FROM tb_menu WHERE menu_code = 'project_dir'",
                    Long.class
                );
                System.out.println("=== 工程目录菜单创建成功，ID: " + projectDirId + " ===");
                // 注意：不自动给任何角色分配权限，由管理员在角色管理页面手动配置
            }

            // 4. 查找需要移动的独立页面菜单（排除首页和记账管理），包含工时配置
            // 记账管理(account)已独立为一级菜单，不归入工程目录
            List<Map<String, Object>> independentMenus = jdbcTemplate.queryForList(
                "SELECT id, menu_code, menu_name FROM tb_menu WHERE menu_type = 2 AND (parent_id IS NULL OR parent_id = 0) " +
                "AND menu_code IN ('employee', 'attendance', 'salary')"
            );
            // 额外将 workhour:config 也移到工程目录下（不受 parent_id=0 限制）
            List<Map<String, Object>> workhourMenus = jdbcTemplate.queryForList(
                "SELECT id, menu_code, menu_name FROM tb_menu WHERE menu_code = 'workhour:config'"
            );

            System.out.println("=== 找到 " + independentMenus.size() + " 个需要移动的独立页面 ===");
            
            // 5. 如果首页已在工程目录或系统设置下，将其移出来恢复为独立菜单（只修复parent_id，不重置sort_order）
            jdbcTemplate.update(
                "UPDATE tb_menu SET parent_id = NULL WHERE menu_code = 'home' AND parent_id IS NOT NULL AND parent_id != 0"
            );
            System.out.println("  - 已确保首页为独立菜单（parent_id=NULL）");
            
            // 6. 将其他菜单移到工程目录下
            int sortOrder = 0;
            for (Map<String, Object> menu : independentMenus) {
                Long menuId = ((Number) menu.get("id")).longValue();
                String menuCode = (String) menu.get("menu_code");
                String menuName = (String) menu.get("menu_name");
                
                jdbcTemplate.update(
                    "UPDATE tb_menu SET parent_id = ?, sort_order = ? WHERE id = ?",
                    projectDirId, sortOrder++, menuId
                );
                System.out.println("  - 已将 " + menuName + " (" + menuCode + ") 移到工程目录下");
            }

            // 7. 将工时配置也移到工程目录下
            for (Map<String, Object> menu : workhourMenus) {
                Long menuId = ((Number) menu.get("id")).longValue();
                String menuName = (String) menu.get("menu_name");
                jdbcTemplate.update(
                    "UPDATE tb_menu SET parent_id = ?, sort_order = ? WHERE id = ?",
                    projectDirId, sortOrder++, menuId
                );
                System.out.println("  - 已将 " + menuName + " (workhour:config) 移到工程目录下");
            }

            System.out.println("=== 工程目录菜单配置完成 ===");

            // 8. 将记账管理移出工程目录，设为独立一级菜单
            // 先检查account菜单当前状态
            List<Map<String, Object>> accountMenus = jdbcTemplate.queryForList(
                "SELECT id, parent_id, menu_name FROM tb_menu WHERE menu_code = 'account'"
            );
            if (!accountMenus.isEmpty()) {
                Map<String, Object> accountMenu = accountMenus.get(0);
                Long accountId = ((Number) accountMenu.get("id")).longValue();
                Long currentParentId = accountMenu.get("parent_id") != null ? ((Number) accountMenu.get("parent_id")).longValue() : 0L;
                
                // 如果当前在工程目录下，移出来
                if (currentParentId != 0L) {
                    jdbcTemplate.update(
                        "UPDATE tb_menu SET parent_id = NULL, sort_order = 100 WHERE id = ?",
                        accountId
                    );
                    System.out.println("=== 记账管理已移出工程目录，设为独立一级菜单 ===");
                } else {
                    System.out.println("=== 记账管理已是独立菜单，无需移动 ===");
                }
            }

        } catch (Exception e) {
            System.err.println("=== 创建工程目录菜单失败 ===");
            e.printStackTrace();
        }
    }
}
