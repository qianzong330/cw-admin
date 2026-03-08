package com.example.hello.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * V12 菜单权限体系重构迁移
 * 将各模块统一为"模块入口 + 操作按钮"两层结构
 */
@Component
@Order(10)
public class MenuPermissionMigration implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        if (!isMigrationNeeded()) {
            System.out.println("=== V12菜单权限迁移已完成，跳过 ===");
            return;
        }
        System.out.println("=== 开始执行V12菜单权限体系重构 ===");
        migrateAccountPermissions();
        migrateAttendancePermissions();
        migrateSalaryPermissions();
        migrateWorkhourPermissions();
        migrateEmployeePermissions();
        migrateProjectPermissions();
        migrateCategoryPermissions();
        migrateRolePermissions();
        assignRootRoleAllPermissions();
        System.out.println("=== V12菜单权限体系重构完成 ===");
    }

    /**
     * 通过检查新权限码是否已存在来判断是否需要迁移
     * 检查所有必要的按钮权限，如果有任何一个不存在则需要迁移
     */
    private boolean isMigrationNeeded() {
        String[] requiredPermissions = {
            "employee:add", "employee:edit", "employee:delete",
            "attendance:add", "attendance:edit", "attendance:delete"
        };
        for (String code : requiredPermissions) {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_menu WHERE menu_code = ?",
                Integer.class, code);
            if (count == null || count == 0) {
                System.out.println("=== 缺少必要权限: " + code + "，需要执行迁移 ===");
                return true;
            }
        }
        System.out.println("=== 所有必要权限已存在，跳过迁移 ===");
        return false;
    }

    /**
     * 记账管理：将 account:add 改为按钮权限，新增 edit/delete/approve
     */
    private void migrateAccountPermissions() {
        try {
            // 将 account:add 改为按钮权限（type=3，挂在 account:list id=3 下）
            jdbcTemplate.update(
                "UPDATE tb_menu SET menu_type=3, parent_id=3, menu_url=NULL WHERE menu_code='account:add'");

            // 新增 account:edit/delete/approve（使用 INSERT IGNORE 防重复）
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('account:edit',   '编辑帐条', 3, 3, 2, 1)");
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('account:delete', '删除帐条', 3, 3, 3, 1)");
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('account:approve','审批帐条', 3, 3, 4, 1)");
            System.out.println("  ✓ account 权限迁移完成");
        } catch (Exception e) {
            System.err.println("  ✗ account 权限迁移失败: " + e.getMessage());
        }
    }

    /**
     * 考勤管理：新增 add/edit/delete 按钮权限（attendance:list id=12）
     */
    private void migrateAttendancePermissions() {
        try {
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('attendance:add',    '新增考勤', 3, 12, 1, 1)");
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('attendance:edit',   '编辑考勤', 3, 12, 2, 1)");
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('attendance:delete', '删除考勤', 3, 12, 3, 1)");
            System.out.println("  ✓ attendance 权限迁移完成");
        } catch (Exception e) {
            System.err.println("  ✗ attendance 权限迁移失败: " + e.getMessage());
        }
    }

    /**
     * 工资条管理：新增 add/edit/delete/pay 按钮权限（salary:list id=14）
     */
    private void migrateSalaryPermissions() {
        try {
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('salary:add',    '新增工资条', 3, 14, 1, 1)");
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('salary:edit',   '编辑工资条', 3, 14, 2, 1)");
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('salary:delete', '删除工资条', 3, 14, 3, 1)");
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('salary:pay',    '发放工资条', 3, 14, 4, 1)");
            System.out.println("  ✓ salary 权限迁移完成");
        } catch (Exception e) {
            System.err.println("  ✗ salary 权限迁移失败: " + e.getMessage());
        }
    }

    /**
     * 工时管理：将旧权限码 workhour:config:* 重命名为 workhour:*
     */
    private void migrateWorkhourPermissions() {
        try {
            jdbcTemplate.update(
                "UPDATE tb_menu SET menu_code='workhour:add',        menu_name='新增配置' WHERE menu_code='workhour:config:add'");
            jdbcTemplate.update(
                "UPDATE tb_menu SET menu_code='workhour:edit',       menu_name='编辑配置' WHERE menu_code='workhour:config:edit'");
            jdbcTemplate.update(
                "UPDATE tb_menu SET menu_code='workhour:delete',     menu_name='删除配置' WHERE menu_code='workhour:config:delete'");
            jdbcTemplate.update(
                "UPDATE tb_menu SET menu_code='workhour:approve',    menu_name='审批配置' WHERE menu_code='workhour:config:approve'");
            jdbcTemplate.update(
                "UPDATE tb_menu SET menu_code='workhour:invalidate', menu_name='作废配置' WHERE menu_code='workhour:config:invalidate'");
            System.out.println("  ✓ workhour 权限重命名完成");
        } catch (Exception e) {
            System.err.println("  ✗ workhour 权限重命名失败: " + e.getMessage());
        }
    }

    /**
     * 员工管理：新增 add/edit/delete 按钮权限（employee:list id=8）
     */
    private void migrateEmployeePermissions() {
        try {
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('employee:add',    '新增员工', 3, 8, 1, 1)");
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('employee:edit',   '编辑员工', 3, 8, 2, 1)");
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('employee:delete', '删除员工', 3, 8, 3, 1)");
            System.out.println("  ✓ employee 权限迁移完成");
        } catch (Exception e) {
            System.err.println("  ✗ employee 权限迁移失败: " + e.getMessage());
        }
    }

    /**
     * 项目管理：新增 add/edit/delete/assign 按钮权限（project:list id=10）
     */
    private void migrateProjectPermissions() {
        try {
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('project:add',    '新增项目', 3, 10, 1, 1)");
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('project:edit',   '编辑项目', 3, 10, 2, 1)");
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('project:delete', '删除项目', 3, 10, 3, 1)");
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('project:assign', '关联员工', 3, 10, 4, 1)");
            System.out.println("  ✓ project 权限迁移完成");
        } catch (Exception e) {
            System.err.println("  ✗ project 权限迁移失败: " + e.getMessage());
        }
    }

    /**
     * 分类管理：新增 add/edit/delete 按钮权限（category:list id=6）
     */
    private void migrateCategoryPermissions() {
        try {
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('category:add',    '新增分类', 3, 6, 1, 1)");
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('category:edit',   '编辑分类', 3, 6, 2, 1)");
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('category:delete', '删除分类', 3, 6, 3, 1)");
            System.out.println("  ✓ category 权限迁移完成");
        } catch (Exception e) {
            System.err.println("  ✗ category 权限迁移失败: " + e.getMessage());
        }
    }

    /**
     * 角色管理：新增 role:edit 按钮权限（role id=36）
     */
    private void migrateRolePermissions() {
        try {
            // 先查找 role 菜单的 id
            Long roleMenuId = null;
            try {
                roleMenuId = jdbcTemplate.queryForObject(
                    "SELECT id FROM tb_menu WHERE menu_code = 'role'", Long.class);
            } catch (Exception ignored) {}

            if (roleMenuId == null) {
                // 尝试用 id=36
                roleMenuId = 36L;
            }

            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES " +
                "('role:edit', '配置权限', 3, " + roleMenuId + ", 1, 1)");
            System.out.println("  ✓ role 权限迁移完成");
        } catch (Exception e) {
            System.err.println("  ✗ role 权限迁移失败: " + e.getMessage());
        }
    }

    /**
     * 为 root 角色（id=1）分配所有新增的按钮权限
     */
    private void assignRootRoleAllPermissions() {
        try {
            String[] newPermissions = {
                "account:add", "account:edit", "account:delete", "account:approve",
                "attendance:add", "attendance:edit", "attendance:delete",
                "salary:add", "salary:edit", "salary:delete", "salary:pay",
                "workhour:add", "workhour:edit", "workhour:delete", "workhour:approve", "workhour:invalidate",
                "employee:add", "employee:edit", "employee:delete",
                "project:add", "project:edit", "project:delete", "project:assign",
                "category:add", "category:edit", "category:delete",
                "role:edit"
            };

            int count = 0;
            for (String code : newPermissions) {
                try {
                    Long menuId = jdbcTemplate.queryForObject(
                        "SELECT id FROM tb_menu WHERE menu_code = ?", Long.class, code);
                    if (menuId != null) {
                        jdbcTemplate.update(
                            "INSERT IGNORE INTO tb_role_menu(role_id, menu_id) VALUES (1, ?)", menuId);
                        count++;
                    }
                } catch (Exception ignored) {}
            }
            System.out.println("  ✓ root 角色分配新权限完成，共 " + count + " 个");
        } catch (Exception e) {
            System.err.println("  ✗ root 角色权限分配失败: " + e.getMessage());
        }
    }
}
