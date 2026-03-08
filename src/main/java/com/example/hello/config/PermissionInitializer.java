package com.example.hello.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 初始化角色菜单权限
 */
@Component
public class PermissionInitializer implements CommandLineRunner {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public void run(String... args) {
        // 1. 先检查并重建 tb_role表 (防止 EmployeeMapper 查询失败)
        createRoleTableIfNotExist();
            
        // 2. 检查是否需要添加 role_code 字段
        addRoleCodeColumnIfNotExist();
            
        // 3. 初始化角色数据
        initRolesIfNotExist();
            
        // 4. 分配工时配置权限
        assignWorkHourPermissions();
            
        // 5. 分配审批菜单权限给 BOSS
        assignApprovalMenuToBoss();
            
        // 6. 分配考勤管理权限给财务角色
        assignAttendancePermissionsToFinance();
            
        // 7. 分配员工管理按钮权限给 BOSS
        assignEmployeePermissionsToBoss();
        
        // 8. 分配考勤管理按钮权限给 BOSS
        assignAttendancePermissionsToBoss();
        
        // 9. 分配基础配置目录权限给 BOSS（包含角色管理）
        assignBasicConfigPermissionsToBoss();
    }
    
    /**
     * 添加 role_code 字段 (如果不存在)
     */
    private void addRoleCodeColumnIfNotExist() {
        try {
            // 检查 role_code 字段是否存在
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_role' AND COLUMN_NAME = 'role_code'",
                Integer.class);
            
            if (count != null && count == 0) {
                // 字段不存在，删除旧表重新创建
                System.out.println("=== 删除旧的 tb_role 表 ===");
                jdbcTemplate.execute("DROP TABLE IF EXISTS tb_role");
                
                // 重新创建表
                createRoleTableIfNotExist();
                
                System.out.println("=== 重新创建 tb_role 表成功 ===");
            } else {
                System.out.println("=== role_code 字段已存在 ===");
            }
        } catch (Exception e) {
            System.err.println("添加 role_code 字段失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建角色表 (如果不存在)
     */
    private void createRoleTableIfNotExist() {
        try {
            // 检查表是否存在
            Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_role'",
                Integer.class);
            
            if (tableCount == null || tableCount == 0) {
                // 表不存在，创建表
                jdbcTemplate.execute(
                    "CREATE TABLE tb_role (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色 ID'," +
                    "role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码'," +
                    "role_name VARCHAR(100) NOT NULL COMMENT '角色名称'," +
                    "remark VARCHAR(200) COMMENT '备注'," +
                    "created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表'");
                System.out.println("=== tb_role 表创建成功 ===");
            } else {
                System.out.println("=== tb_role 表已存在 ===");
            }
        } catch (Exception e) {
            System.err.println("创建 tb_role 表失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 初始化角色数据 (如果不存在)
     */
    private void initRolesIfNotExist() {
        try {
            String[] roles = {"root", "boss", "finance", "hr", "employee"};
            String[] roleNames = {"超级管理员", "BOSS", "财务", "HR", "员工"};
            
            for (int i = 0; i < roles.length; i++) {
                // 先检查是否存在
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tb_role WHERE role_code = ?",
                    Integer.class, roles[i]);
                
                if (count != null && count == 0) {
                    // 不存在则插入
                    jdbcTemplate.update(
                        "INSERT INTO tb_role (role_code, role_name, remark) VALUES (?, ?, ?)",
                        roles[i], roleNames[i], roleNames[i] + "角色");
                }
            }
            System.out.println("=== 角色数据初始化完成 ===");
        } catch (Exception e) {
            System.err.println("初始化角色数据失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 为 boss 和 finance 角色分配工时配置权限
     */
    private void assignWorkHourPermissions() {
        try {
            // 获取 boss 角色 ID
            Long bossRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_role WHERE role_code = 'boss'", Long.class);
            
            // 获取 finance 角色 ID
            Long financeRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_role WHERE role_code = 'finance'", Long.class);
            
            if (bossRoleId == null || financeRoleId == null) {
                System.out.println("角色不存在，跳过权限分配");
                return;
            }
            
            // 工时配置相关菜单编码（workhour:config 已归入 project_dir 工程目录）
            String[] menuCodes = {
                "project_dir",
                "workhour:config", 
                "workhour:add", 
                "workhour:edit", 
                "workhour:delete", 
                "workhour:approve", 
                "workhour:invalidate"
            };
            
            int count = 0;
            for (String menuCode : menuCodes) {
                // 获取菜单 ID
                Long menuId = jdbcTemplate.queryForObject(
                    "SELECT id FROM tb_menu WHERE menu_code = ?", Long.class, menuCode);
                
                if (menuId != null) {
                    // 为 boss 分配权限
                    jdbcTemplate.update(
                        "INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (?, ?)",
                        bossRoleId, menuId);
                    
                    // 为 finance 分配权限
                    jdbcTemplate.update(
                        "INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (?, ?)",
                        financeRoleId, menuId);
                    
                    count++;
                }
            }
            
            System.out.println("=== 工时配置权限分配完成，共分配 " + count + " 个菜单权限 ===");
        } catch (Exception e) {
            System.err.println("分配权限时出错：" + e.getMessage());
        }
    }
    
    /**
     * 为 finance 角色分配考勤管理权限
     */
    private void assignAttendancePermissionsToFinance() {
        try {
            Long financeRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_role WHERE role_code = 'finance'", Long.class);
            
            if (financeRoleId == null) {
                System.out.println("未找到 finance 角色，跳过考勤权限分配");
                return;
            }
            
            // 考勤管理相关菜单编码
            String[] menuCodes = {
                "attendance",
                "attendance:add",
                "attendance:edit",
                "attendance:delete"
            };
            
            int count = 0;
            for (String menuCode : menuCodes) {
                Long menuId = jdbcTemplate.queryForObject(
                    "SELECT id FROM tb_menu WHERE menu_code = ?", Long.class, menuCode);
                
                if (menuId != null) {
                    jdbcTemplate.update(
                        "INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (?, ?)",
                        financeRoleId, menuId);
                    count++;
                }
            }
            
            System.out.println("=== 财务角色考勤权限分配完成，共分配 " + count + " 个菜单权限 ===");
        } catch (Exception e) {
            System.err.println("分配考勤权限时出错：" + e.getMessage());
        }
    }
    
    /**
     * 分配审批相关菜单权限给 BOSS 角色
     */
    private void assignApprovalMenuToBoss() {
        try {
            Long bossRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_role WHERE role_code = 'boss'", Long.class);
            if (bossRoleId == null) {
                System.out.println("未找到 boss 角色，跳过审批菜单分配");
                return;
            }
            
            String[] approvalMenuCodes = {
                "approval",
                "approval:workhour",
                "approval:attendance",
                "approval:salary"
            };
            
            int count = 0;
            for (String menuCode : approvalMenuCodes) {
                try {
                    Long menuId = jdbcTemplate.queryForObject(
                        "SELECT id FROM tb_menu WHERE menu_code = ?", Long.class, menuCode);
                    if (menuId != null) {
                        jdbcTemplate.update(
                            "INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (?, ?)",
                            bossRoleId, menuId);
                        count++;
                    }
                } catch (Exception ignore) {}
            }
            System.out.println("=== BOSS 审批菜单权限分配完成，共分配 " + count + " 个 ===");
        } catch (Exception e) {
            System.err.println("分配 BOSS 审批菜单权限失败：" + e.getMessage());
        }
    }
    
    /**
     * 分配员工管理按钮权限给 BOSS 角色
     */
    private void assignEmployeePermissionsToBoss() {
        try {
            Long bossRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_role WHERE role_code = 'boss'", Long.class);
            if (bossRoleId == null) {
                System.out.println("未找到 boss 角色，跳过员工按钮权限分配");
                return;
            }
            
            String[] employeePermissions = {
                "employee:add", "employee:edit", "employee:delete"
            };
            
            int count = 0;
            for (String menuCode : employeePermissions) {
                try {
                    Long menuId = jdbcTemplate.queryForObject(
                        "SELECT id FROM tb_menu WHERE menu_code = ?", Long.class, menuCode);
                    if (menuId != null) {
                        jdbcTemplate.update(
                            "INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (?, ?)",
                            bossRoleId, menuId);
                        count++;
                    }
                } catch (Exception ignore) {}
            }
            System.out.println("=== BOSS 员工管理按钮权限分配完成，共分配 " + count + " 个 ===");
        } catch (Exception e) {
            System.err.println("分配 BOSS 员工管理权限失败：" + e.getMessage());
        }
    }
    
    /**
     * 分配考勤管理按钮权限给 BOSS 角色
     */
    private void assignAttendancePermissionsToBoss() {
        try {
            Long bossRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_role WHERE role_code = 'boss'", Long.class);
            if (bossRoleId == null) {
                System.out.println("未找到 boss 角色，跳过考勤按钮权限分配");
                return;
            }
            
            String[] attendancePermissions = {
                "attendance:add", "attendance:edit", "attendance:delete"
            };
            
            int count = 0;
            for (String menuCode : attendancePermissions) {
                try {
                    Long menuId = jdbcTemplate.queryForObject(
                        "SELECT id FROM tb_menu WHERE menu_code = ?", Long.class, menuCode);
                    if (menuId != null) {
                        jdbcTemplate.update(
                            "INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (?, ?)",
                            bossRoleId, menuId);
                        count++;
                    }
                } catch (Exception ignore) {}
            }
            System.out.println("=== BOSS 考勤管理按钮权限分配完成，共分配 " + count + " 个 ===");
        } catch (Exception e) {
            System.err.println("分配 BOSS 考勤管理权限失败：" + e.getMessage());
        }
    }
    
    /**
     * 分配基础配置目录权限给 BOSS 角色（包含角色管理）
     */
    private void assignBasicConfigPermissionsToBoss() {
        try {
            Long bossRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_role WHERE role_code = 'boss'", Long.class);
            if (bossRoleId == null) {
                System.out.println("未找到 BOSS 角色，跳过基础配置权限分配");
                return;
            }
            
            // 基础配置目录及其子菜单
            String[] menuCodes = {
                "basic_config",
                "jobcategory:list",
                "project",
                "role",
                "workhour:config",
                "category"
            };
            
            int count = 0;
            for (String menuCode : menuCodes) {
                try {
                    Long menuId = jdbcTemplate.queryForObject(
                        "SELECT id FROM tb_menu WHERE menu_code = ?", Long.class, menuCode);
                    if (menuId != null) {
                        jdbcTemplate.update(
                            "INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (?, ?)",
                            bossRoleId, menuId);
                        count++;
                    }
                } catch (Exception ignore) {}
            }
            System.out.println("=== BOSS 基础配置菜单权限分配完成，共分配 " + count + " 个 ===");
        } catch (Exception e) {
            System.err.println("分配 BOSS 基础配置权限失败：" + e.getMessage());
        }
    }
}
