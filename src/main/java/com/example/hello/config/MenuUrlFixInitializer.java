package com.example.hello.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 修复菜单 URL 和图标初始化器
 * 确保所有导航菜单都有正确的 url 和 icon
 */
@Component
public class MenuUrlFixInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            System.out.println("=== 开始检查并修复菜单 URL ===");

            // 定义需要修复的菜单
            String[][] menuFixes = {
                {"account", "/account/list", "bi-journal-text"},
                {"attendance", "/attendance/list", "bi-calendar-check"},
                {"salary", "/salary/list", "bi-cash-stack"},
                {"workhour:config", "/workhour/config", "bi-clock"},
                {"role", "/role/list", "bi-person-badge"},
                {"employee", "/employee/list", "bi-people"},
                {"category", "/category/list", "bi-tags"},
                {"jobcategory", "/jobcategory/list", "bi-hammer"},
                {"project", "/project/list", "bi-folder"}
            };

            int fixedCount = 0;
            for (String[] fix : menuFixes) {
                String menuCode = fix[0];
                String url = fix[1];
                String icon = fix[2];

                // 检查菜单是否存在
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tb_menu WHERE menu_code = ?",
                    Integer.class, menuCode
                );

                if (count != null && count > 0) {
                    // 更新 menu_url 和 menu_icon
                    jdbcTemplate.update(
                        "UPDATE tb_menu SET menu_url = ?, menu_icon = ? WHERE menu_code = ?",
                        url, icon, menuCode
                    );
                    System.out.println("  ✓ 修复菜单: " + menuCode + " -> " + url);
                    fixedCount++;
                } else {
                    System.out.println("  ✗ 菜单不存在: " + menuCode);
                }
            }

            System.out.println("=== 菜单 URL 修复完成，共修复 " + fixedCount + " 个菜单 ===");

            // 验证修复结果
            System.out.println("=== 当前导航菜单状态 ===");
            var menus = jdbcTemplate.queryForList(
                "SELECT menu_code, menu_type, menu_url, menu_icon, status FROM tb_menu " +
                "WHERE menu_code IN ('account','attendance','salary','workhour:config','role','employee','category','jobcategory','project')"
            );
            for (Map<String, Object> menu : menus) {
                System.out.println("  " + menu.get("menu_code") + " | url=" + menu.get("menu_url") + " | icon=" + menu.get("menu_icon"));
            }

        } catch (Exception e) {
            System.err.println("菜单 URL 修复失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
