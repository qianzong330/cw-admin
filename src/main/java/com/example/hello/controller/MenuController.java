package com.example.hello.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/menu")
public class MenuController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 菜单列表页面
     */
    @GetMapping("/list")
    public String list(Model model) {
        // 1. 单独查顶层菜单（parent_id IS NULL 或 0），按 sort_order 正确排序
        List<Map<String, Object>> topMenus = jdbcTemplate.queryForList(
            "SELECT * FROM tb_menu WHERE status = 1 AND (parent_id IS NULL OR parent_id = 0) AND menu_type IN (1, 2) ORDER BY sort_order"
        );

        // 2. 为每个一级目录（type=1）查询其直接子菜单（type=2）
        for (Map<String, Object> menu : topMenus) {
            if (Integer.valueOf(1).equals(menu.get("menu_type"))) {
                Object menuId = menu.get("id");
                List<Map<String, Object>> children = jdbcTemplate.queryForList(
                    "SELECT * FROM tb_menu WHERE status = 1 AND parent_id = ? AND menu_type = 2 ORDER BY sort_order",
                    menuId
                );
                menu.put("children", children);
            }
        }

        model.addAttribute("topMenus", topMenus);
        return "menu/list";
    }

    /**
     * 新增菜单页面
     */
    @GetMapping("/add")
    public String addPage(Model model) {
        // 查询可作为父菜单的菜单（目录和页面）
        List<Map<String, Object>> parentMenus = jdbcTemplate.queryForList(
            "SELECT * FROM tb_menu WHERE menu_type IN (1, 2) AND status = 1 ORDER BY sort_order"
        );
        model.addAttribute("parentMenus", parentMenus);
        // 新增时传 null，模板里用 menu != null 判断
        return "menu/form";
    }

    /**
     * 保存新菜单
     */
    @PostMapping("/add")
    @ResponseBody
    public Map<String, Object> addMenu(@RequestBody Map<String, Object> menuData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String menuCode = (String) menuData.get("menuCode");
            String menuName = (String) menuData.get("menuName");
            Integer menuType = (Integer) menuData.get("menuType");
            Integer parentId = (Integer) menuData.get("parentId");
            String menuIcon = (String) menuData.get("menuIcon");
            String menuUrl = (String) menuData.get("menuUrl");
            Integer sortOrder = (Integer) menuData.get("sortOrder");
            Integer status = (Integer) menuData.get("status");
            
            // 检查菜单编码是否已存在
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_menu WHERE menu_code = ?", Integer.class, menuCode);
            if (count != null && count > 0) {
                result.put("success", false);
                result.put("message", "菜单编码已存在");
                return result;
            }
            
            // 插入新菜单
            jdbcTemplate.update(
                "INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                menuCode, menuName, menuType, 
                (parentId != null && parentId > 0) ? parentId : null, 
                menuIcon, menuUrl, sortOrder, status
            );
            
            result.put("success", true);
            result.put("message", "菜单创建成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "创建失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 编辑菜单页面
     */
    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable Long id, Model model) {
        // 查询当前菜单
        Map<String, Object> menu = jdbcTemplate.queryForMap(
            "SELECT * FROM tb_menu WHERE id = ?", id
        );
        model.addAttribute("menu", menu);
        
        // 查询可作为父菜单的菜单（排除自己）
        List<Map<String, Object>> parentMenus = jdbcTemplate.queryForList(
            "SELECT * FROM tb_menu WHERE menu_type IN (1, 2) AND status = 1 AND id != ? ORDER BY sort_order",
            id
        );
        model.addAttribute("parentMenus", parentMenus);
        
        return "menu/form";
    }

    /**
     * 更新菜单
     */
    @PostMapping("/update/{id}")
    @ResponseBody
    public Map<String, Object> updateMenu(@PathVariable Long id, @RequestBody Map<String, Object> menuData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String menuCode = (String) menuData.get("menuCode");
            String menuName = (String) menuData.get("menuName");
            Integer menuType = (Integer) menuData.get("menuType");
            Integer parentId = (Integer) menuData.get("parentId");
            String menuIcon = (String) menuData.get("menuIcon");
            String menuUrl = (String) menuData.get("menuUrl");
            Integer sortOrder = (Integer) menuData.get("sortOrder");
            Integer status = (Integer) menuData.get("status");
            
            // 检查菜单编码是否被其他菜单使用
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_menu WHERE menu_code = ? AND id != ?", Integer.class, menuCode, id);
            if (count != null && count > 0) {
                result.put("success", false);
                result.put("message", "菜单编码已被其他菜单使用");
                return result;
            }
            
            // 更新菜单
            jdbcTemplate.update(
                "UPDATE tb_menu SET menu_code = ?, menu_name = ?, menu_type = ?, parent_id = ?, " +
                "menu_icon = ?, menu_url = ?, sort_order = ?, status = ? WHERE id = ?",
                menuCode, menuName, menuType, 
                (parentId != null && parentId > 0) ? parentId : null, 
                menuIcon, menuUrl, sortOrder, status, id
            );
            
            result.put("success", true);
            result.put("message", "菜单更新成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "更新失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 更新菜单排序
     */
    @PostMapping("/updateOrder")
    @ResponseBody
    public Map<String, Object> updateOrder(@RequestBody List<Map<String, Object>> orders) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            for (Map<String, Object> order : orders) {
                Long id = Long.valueOf(order.get("id").toString());
                Integer sortOrder = Integer.valueOf(order.get("sortOrder").toString());
                jdbcTemplate.update("UPDATE tb_menu SET sort_order = ? WHERE id = ?", sortOrder, id);
            }
            result.put("success", true);
            result.put("message", "排序保存成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "保存失败：" + e.getMessage());
        }
        
        return result;
    }
}
