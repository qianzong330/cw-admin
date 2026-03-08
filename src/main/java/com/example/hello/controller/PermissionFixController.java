package com.example.hello.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 临时工具接口 - 用于重建权限表
 */
@RestController
public class PermissionFixController {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @PostMapping("/fix/permission")
    public Map<String, Object> fixPermissionTables() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            System.out.println("=== 开始重建权限表 ===");
            
            // 1. 删除旧表
            jdbcTemplate.execute("DROP TABLE IF EXISTS tb_role_menu");
            System.out.println("已删除 tb_role_menu");
            
            jdbcTemplate.execute("DROP TABLE IF EXISTS tb_menu");
            System.out.println("已删除 tb_menu");
            
            jdbcTemplate.execute("DROP TABLE IF EXISTS tb_role");
            System.out.println("已删除 tb_role");
            
            // 2. 创建角色表
            jdbcTemplate.execute(
                "CREATE TABLE tb_role (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色 ID'," +
                "role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码'," +
                "role_name VARCHAR(100) NOT NULL COMMENT '角色名称'," +
                "remark VARCHAR(200) COMMENT '备注'," +
                "created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表'");
            System.out.println("已创建 tb_role 表");
                        
            // 3. 插入角色数据
            String[] roles = {"root", "boss", "finance", "hr", "employee"};
            String[] roleNames = {"超级管理员", "BOSS", "财务", "HR", "员工"};
                        
            for (int i = 0; i < roles.length; i++) {
                jdbcTemplate.update(
                    "INSERT INTO tb_role (role_code, role_name, remark) VALUES (?, ?, ?)",
                    roles[i], roleNames[i], roleNames[i] + "角色");
            }
            System.out.println("已初始化角色数据");
                        
            // 4. 创建菜单表
            jdbcTemplate.execute(
                "CREATE TABLE tb_menu (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '菜单 ID'," +
                "menu_code VARCHAR(100) NOT NULL UNIQUE COMMENT '菜单编码'," +
                "menu_name VARCHAR(100) NOT NULL COMMENT '菜单名称'," +
                "menu_type TINYINT NOT NULL DEFAULT 1 COMMENT '菜单类型：1-目录，2-菜单，3-按钮'," +
                "parent_id BIGINT DEFAULT 0 COMMENT '父菜单 ID'," +
                "icon VARCHAR(50) COMMENT '图标'," +
                "url VARCHAR(200) COMMENT 'URL'," +
                "sort_order INT DEFAULT 0 COMMENT '排序'," +
                "status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用'," +
                "created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表'");
            System.out.println("已创建 tb_menu 表");
                        
            // 5. 插入菜单数据
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('home', '首页', 2, 0, 'bi-house-door', '/index', 1)");
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('account', '记账管理', 1, 0, 'bi-book', null, 2)");
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('account:list', '记账列表', 2, 2, 'bi-list', '/account/list', 1)");
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('account:add', '新增记账', 2, 2, 'bi-plus-circle', '/account/form', 2)");
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('category', '分类管理', 1, 0, 'bi-grid', null, 3)");
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('category:list', '分类列表', 2, 5, 'bi-list', '/category/list', 1)");
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('employee', '员工管理', 1, 0, 'bi-people', null, 4)");
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('employee:list', '员工列表', 2, 7, 'bi-list', '/employee/list', 1)");
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('project', '项目管理', 1, 0, 'bi-briefcase', null, 5)");
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('project:list', '项目列表', 2, 9, 'bi-list', '/project/list', 1)");
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('attendance', '考勤管理', 1, 0, 'bi-calendar-check', null, 6)");
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('attendance:list', '考勤列表', 2, 11, 'bi-list', '/attendance/list', 1)");
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('salary', '工资条管理', 1, 0, 'bi-cash-stack', null, 7)");
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('salary:list', '工资条列表', 2, 13, 'bi-list', '/salary/list', 1)");
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('workhour:config', '工时配置', 1, 0, 'bi-gear', null, 8)");
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('workhour:config:list', '工时配置列表', 2, 15, 'bi-list', '/workhour/config/list', 1)");
                        
            // 添加工时配置的按鈕权限
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('workhour:config:add', '新增工时配置', 3, 16, null, null, 1)");
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('workhour:config:edit', '编辑工时配置', 3, 16, null, null, 2)");
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('workhour:config:delete', '删除工时配置', 3, 16, null, null, 3)");
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('workhour:config:approve', '审批工时配置', 3, 16, null, null, 4)");
            jdbcTemplate.update("INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order) VALUES ('workhour:config:invalidate', '使工时配置失效', 3, 16, null, null, 5)");
            System.out.println("已初始化按钮权限数据");
            System.out.println("已初始化菜单数据");
                        
            // 6. 创建角色菜单关联表
            jdbcTemplate.execute(
                "CREATE TABLE tb_role_menu (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID'," +
                "role_id BIGINT NOT NULL COMMENT '角色 ID'," +
                "menu_id BIGINT NOT NULL COMMENT '菜单 ID'," +
                "created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "UNIQUE KEY uk_role_menu (role_id, menu_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表'");
            System.out.println("已创建 tb_role_menu 表");
                        
            // 7. 分配 root 角色所有菜单权限
            for (int menuId = 1; menuId <= 16; menuId++) {
                jdbcTemplate.update("INSERT INTO tb_role_menu (role_id, menu_id) VALUES (1, ?)", menuId);
            }
            System.out.println("已分配 root 角色所有菜单权限");
                        
            // 8. 分配 boss 角色所有菜单权限
            for (int menuId = 1; menuId <= 16; menuId++) {
                jdbcTemplate.update("INSERT INTO tb_role_menu (role_id, menu_id) VALUES (2, ?)", menuId);
            }
            System.out.println("已分配 boss 角色所有菜单权限");
            
            result.put("success", true);
            result.put("message", "权限表重建成功");
            result.put("roles", roles);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "重建失败：" + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
}
