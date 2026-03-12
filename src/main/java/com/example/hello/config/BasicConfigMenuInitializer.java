package com.example.hello.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 基础配置菜单初始化器
 * 应用启动时检查并创建基础配置目录菜单
 */
@Component
public class BasicConfigMenuInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            // ====== 1. 确保 basic_config 目录菜单存在 ======
            List<Map<String, Object>> existingMenus = jdbcTemplate.queryForList(
                "SELECT id FROM tb_menu WHERE menu_code = 'basic_config'"
            );
            Long basicConfigId;
            if (existingMenus.isEmpty()) {
                jdbcTemplate.update(
                    "INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status) " +
                    "VALUES ('basic_config', '基础配置', 0, 1, NULL, 'bi-gear-wide-connected', 90, 1)"
                );
                basicConfigId = jdbcTemplate.queryForObject(
                    "SELECT id FROM tb_menu WHERE menu_code = 'basic_config'", Long.class
                );
                System.out.println("=== 创建基础配置菜单成功，ID: " + basicConfigId);
            } else {
                basicConfigId = ((Number) existingMenus.get(0).get("id")).longValue();
                System.out.println("=== 基础配置菜单已存在，ID: " + basicConfigId);
            }

            // ====== 2. 确保 system_settings 目录菜单存在 ======
            List<Map<String, Object>> sysMenus = jdbcTemplate.queryForList(
                "SELECT id FROM tb_menu WHERE menu_code = 'system_settings'"
            );
            Long sysSettingsId;
            if (sysMenus.isEmpty()) {
                jdbcTemplate.update(
                    "INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status) " +
                    "VALUES ('system_settings', '系统设置', 0, 1, NULL, 'bi-gear', 100, 1)"
                );
                sysSettingsId = jdbcTemplate.queryForObject(
                    "SELECT id FROM tb_menu WHERE menu_code = 'system_settings'", Long.class
                );
                System.out.println("=== 创建系统设置菜单成功，ID: " + sysSettingsId);
            } else {
                sysSettingsId = ((Number) sysMenus.get(0).get("id")).longValue();
                System.out.println("=== 系统设置菜单已存在，ID: " + sysSettingsId);
            }

            // ====== 3. 确保菜单管理作为系统设置子菜单存在 ======
            List<Map<String, Object>> menuMgmt = jdbcTemplate.queryForList(
                "SELECT id FROM tb_menu WHERE menu_code = 'menu:list'"
            );
            if (menuMgmt.isEmpty()) {
                jdbcTemplate.update(
                    "INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status) " +
                    "VALUES ('menu:list', '菜单管理', ?, 2, '/menu/list', 'bi-list-ol', 1, 1)",
                    sysSettingsId
                );
                System.out.println("=== 创建菜单管理菜单成功");
            } else {
                jdbcTemplate.update(
                    "UPDATE tb_menu SET parent_id = ?, menu_type = 2, menu_url = '/menu/list', menu_icon = 'bi-list-ol', sort_order = 1 WHERE menu_code = 'menu:list'",
                    sysSettingsId
                );
            }

            // ====== 4. 确保角色管理作为系统设置子菜单存在 ======
            List<Map<String, Object>> roleMenus = jdbcTemplate.queryForList(
                "SELECT id, menu_type, status FROM tb_menu WHERE menu_code = 'role'"
            );
            if (roleMenus.isEmpty()) {
                // 不存在则新建
                jdbcTemplate.update(
                    "INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status) " +
                    "VALUES ('role', '\u89d2\u8272\u7ba1\u7406', ?, 2, '/role/list', 'bi-person-badge', 0, 1)",
                    sysSettingsId
                );
                Long newRoleId = jdbcTemplate.queryForObject("SELECT id FROM tb_menu WHERE menu_code = 'role'", Long.class);
                // role:edit 的 parent_id 指向新的 role id
                jdbcTemplate.update("UPDATE tb_menu SET parent_id = ? WHERE menu_code = 'role:edit'", newRoleId);
                System.out.println("=== \u521b\u5efa\u89d2\u8272\u7ba1\u7406\u83dc\u5355\u6210\u529f, id=" + newRoleId);
            } else {
                // 已存在：确保 parent_id、menu_type、status 正确
                jdbcTemplate.update(
                    "UPDATE tb_menu SET parent_id = ?, menu_type = 2, menu_url = '/role/list', menu_icon = 'bi-person-badge', sort_order = 0, status = 1 WHERE menu_code = 'role'",
                    sysSettingsId
                );
                System.out.println("=== 角色管理菜单已归入系统设置");
            }

            // ====== 5. 仅给 root 角色分配系统设置、菜单管理、角色管理权限（不自动分配给其他角色）======
            jdbcTemplate.update("INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (1, ?)", sysSettingsId);
            Long menuListId = jdbcTemplate.queryForObject("SELECT id FROM tb_menu WHERE menu_code = 'menu:list'", Long.class);
            jdbcTemplate.update("INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (1, ?)", menuListId);
            try {
                Long roleMenuId = jdbcTemplate.queryForObject("SELECT id FROM tb_menu WHERE menu_code = 'role'", Long.class);
                jdbcTemplate.update("INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (1, ?)", roleMenuId);
            } catch (Exception ignore) {}
            // ====== 6. 将基础配置子菜单归入 basic_config 目录 ======
            // 注意：role 已在上面归入系统设置，workhour:config 归入工程管理(project)，这里不包含
            String[] basicMenuCodes = {"jobcategory", "jobcategory:list", "category"};
            int[] basicSortOrders = {1, 1, 2};
            for (int i = 0; i < basicMenuCodes.length; i++) {
                int updated = jdbcTemplate.update(
                    "UPDATE tb_menu SET parent_id = ?, menu_type = 2, sort_order = ? WHERE menu_code = ?",
                    basicConfigId, basicSortOrders[i], basicMenuCodes[i]
                );
                if (updated > 0) System.out.println("=== 更新菜单 " + basicMenuCodes[i] + " -> 基础配置");
            }

            // ====== 6b. 将工时配置归入工程目录(project_dir) - 由 CreateProjectMenu 处理，此处跳过 ======
            System.out.println("=== workhour:config 将由 CreateProjectMenu 归入工程目录 ===");

            // ====== 6. 给 root 角色分配基础配置权限 ======
            jdbcTemplate.update("INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (1, ?)", basicConfigId);

            // ====== 7. 修复直接链接类菜单的 menu_type 为 2 ======
            jdbcTemplate.update("UPDATE tb_menu SET menu_type = 2 WHERE menu_code IN ('account', 'attendance', 'salary', 'employee', 'home')");

            // ====== 8. 确保 jobcategory 有正确的 menu_url/menu_icon ======
            jdbcTemplate.update("UPDATE tb_menu SET menu_url = '/jobcategory/list', menu_icon = 'bi-hammer' WHERE menu_code = 'jobcategory' AND (menu_url IS NULL OR menu_url = '')");

            // ====== 9. 确保目录菜单有图标 ======
            jdbcTemplate.update("UPDATE tb_menu SET menu_icon = 'bi-gear-wide-connected' WHERE menu_code = 'basic_config' AND (menu_icon IS NULL OR menu_icon = '')");
            jdbcTemplate.update("UPDATE tb_menu SET menu_icon = 'bi-gear' WHERE menu_code = 'system_settings' AND (menu_icon IS NULL OR menu_icon = '')");

            // ====== 10. 确保费用项管理菜单存在 ======
            List<Map<String, Object>> feeItemMenus = jdbcTemplate.queryForList(
                "SELECT id FROM tb_menu WHERE menu_code = 'feeitem:list'"
            );
            if (feeItemMenus.isEmpty()) {
                jdbcTemplate.update(
                    "INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status) " +
                    "VALUES ('feeitem:list', '费用项管理', ?, 2, '/feeitem/list', 'bi-tags', 6, 1)",
                    basicConfigId
                );
                System.out.println("=== 费用项管理菜单创建完成 ===");
            } else {
                // 如果 menu_url 为空，修复链接
                jdbcTemplate.update(
                    "UPDATE tb_menu SET menu_url = '/feeitem/list', menu_icon = 'bi-tags', parent_id = ?, sort_order = 6 " +
                    "WHERE menu_code = 'feeitem:list' AND (menu_url IS NULL OR menu_url = '')",
                    basicConfigId
                );
                System.out.println("=== 费用项管理菜单已存在 ===");
            }

            System.out.println("=== 菜单初始化完成（基础配置 + 系统设置）===");

            // 确保顶层目录菜单的 sort_order 不为 0（避免与子菜单冲突）
            // basic_config 初始据点设为 90，如果当前是 0 说明被错误覆盖，修复为 90000
            jdbcTemplate.update(
                "UPDATE tb_menu SET sort_order = 90000 WHERE menu_code = 'basic_config' AND sort_order = 0"
            );

        } catch (Exception e) {
            System.err.println("=== 菜单初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
