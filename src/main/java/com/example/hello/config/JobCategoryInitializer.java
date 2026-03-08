package com.example.hello.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 工种（JobCategory）初始化数据及菜单权限
 */
@Component
public class JobCategoryInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        initJobCategoryData();
        initJobCategoryMenu();
    }
    
    private void initJobCategoryData() {
        // 检查是否已有数据
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tb_job_category", Integer.class);
        
        if (count == null || count == 0) {
            System.out.println("=== 初始化工种数据 ===");
            String[] jobCategories = {
                "钢筋工", "木工", "瓦工", "水电工", "油漆工", 
                "架子工", "焊工", "机械操作工", "普工", "测量员",
                "安全员", "质量员", "施工员", "资料员", "预算员"
            };
            
            for (String name : jobCategories) {
                jdbcTemplate.update(
                    "INSERT INTO tb_job_category (name, salary_amount, salary_type) VALUES (?, 0, 1)",
                    name
                );
            }
            System.out.println("  工种数据初始化完成，共 " + jobCategories.length + " 条");
        }
    }
    
    private void initJobCategoryMenu() {
        // 检查工种管理菜单是否已存在
        Integer menuCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tb_menu WHERE menu_code = 'jobcategory'", Integer.class);
        
        if (menuCount == null || menuCount == 0) {
            System.out.println("=== 添加工种管理菜单 ===");
            
            // 添加工种管理目录 (type=1)
            jdbcTemplate.update(
                "INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, sort_order, status) " +
                "VALUES ('jobcategory', '工种管理', 1, 0, 8, 1)"
            );
            
            // 获取刚插入的目录 ID
            Long parentId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_menu WHERE menu_code = 'jobcategory'", Long.class);
            
            // 添加工种列表菜单 (type=2)
            jdbcTemplate.update(
                "INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, sort_order, status) " +
                "VALUES ('jobcategory:list', '工种列表', 2, ?, 1, 1)",
                parentId
            );
            
            // 获取列表菜单 ID
            Long listId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_menu WHERE menu_code = 'jobcategory:list'", Long.class);
            
            // 添加工种按钮权限 (type=3)
            jdbcTemplate.update(
                "INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, sort_order, status) " +
                "VALUES ('jobcategory:add', '新增工种', 3, ?, 1, 1)",
                listId
            );
            jdbcTemplate.update(
                "INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, sort_order, status) " +
                "VALUES ('jobcategory:edit', '编辑工种', 3, ?, 2, 1)",
                listId
            );
            jdbcTemplate.update(
                "INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, sort_order, status) " +
                "VALUES ('jobcategory:delete', '删除工种', 3, ?, 3, 1)",
                listId
            );
            
            // 给 root 角色分配权限
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_role_menu (role_id, menu_id) " +
                "SELECT 1, id FROM tb_menu WHERE menu_code IN ('jobcategory', 'jobcategory:list', 'jobcategory:add', 'jobcategory:edit', 'jobcategory:delete')"
            );
            
            System.out.println("  工种管理菜单添加完成并已分配给 root");
        }
    }
}
