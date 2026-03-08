package com.example.hello.controller;

import com.example.hello.entity.Employee;
import com.example.hello.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/role")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 新增角色页面
     */
    @GetMapping("/add")
    public String addPage() {
        return "role/add";
    }

    /**
     * 保存新角色
     */
    @PostMapping("/add")
    @ResponseBody
    public Map<String, Object> addRole(
        @RequestParam String roleCode,
        @RequestParam String roleName,
        @RequestParam(required = false) String remark
    ) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查角色编码是否已存在
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_role WHERE role_code = ?", Integer.class, roleCode);
            if (count != null && count > 0) {
                result.put("success", false);
                result.put("message", "角色编码已存在");
                return result;
            }
            
            // 插入新角色
            jdbcTemplate.update(
                "INSERT INTO tb_role (role_code, role_name, remark) VALUES (?, ?, ?)",
                roleCode, roleName, remark
            );
            
            result.put("success", true);
            result.put("message", "角色创建成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "创建失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 角色列表页面
     */
    @GetMapping("/list")
    public String list(Model model) {
        List<Map<String, Object>> roles = jdbcTemplate.queryForList(
            "SELECT r.*, COUNT(rm.id) as menu_count " +
            "FROM tb_role r " +
            "LEFT JOIN tb_role_menu rm ON r.id = rm.role_id " +
            "GROUP BY r.id " +
            "ORDER BY r.id"
        );
        model.addAttribute("roles", roles);
        return "role/list";
    }

    /**
     * 编辑角色权限页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        try {
            // 查询角色信息
            Map<String, Object> role = jdbcTemplate.queryForMap(
                "SELECT * FROM tb_role WHERE id = ?", id
            );
            model.addAttribute("role", role);
        
            // 查询所有启用的菜单（目录、菜单、按钮）
            // 先按 parent_id 排（顶层 parent_id=null/0 在前），再按 sort_order 排
            List<Map<String, Object>> allMenus = jdbcTemplate.queryForList(
                "SELECT * FROM tb_menu WHERE status = 1 ORDER BY COALESCE(parent_id, 0), sort_order"
            );
        
            // 重新组织数据结构：模块 -> 页面 -> 操作
            List<Map<String, Object>> modules = new java.util.ArrayList<>();
            
            // 第一步：收集所有一级目录（模块）
            for (Map<String, Object> menu : allMenus) {
                Integer menuType = (Integer) menu.get("menu_type");
                if (menuType == 1) {
                    Map<String, Object> module = new HashMap<>(menu);
                    module.put("pages", new java.util.ArrayList<Map<String, Object>>());
                    modules.add(module);
                }
            }
            
            // 第二步：处理二级菜单（页面）
            for (Map<String, Object> menu : allMenus) {
                Integer menuType = (Integer) menu.get("menu_type");
                Long parentId = menu.get("parent_id") != null ? ((Number) menu.get("parent_id")).longValue() : 0L;
                
                if (menuType == 2) {
                    if (parentId > 0) {
                        // 挂在目录下的页面，找到对应的模块
                        boolean found = false;
                        for (Map<String, Object> module : modules) {
                            if (module.get("id").equals(parentId)) {
                                Map<String, Object> page = new HashMap<>(menu);
                                page.put("operations", new java.util.ArrayList<Map<String, Object>>());
                                ((List<Map<String, Object>>) module.get("pages")).add(page);
                                found = true;
                                break;
                            }
                        }
                        // 如果没找到父模块，创建一个虚拟模块
                        if (!found) {
                            Map<String, Object> virtualModule = new HashMap<>();
                            virtualModule.put("id", parentId);
                            virtualModule.put("menu_name", "其他模块");
                            virtualModule.put("menu_icon", "bi-folder");
                            virtualModule.put("pages", new java.util.ArrayList<Map<String, Object>>());
                            modules.add(virtualModule);
                            
                            Map<String, Object> page = new HashMap<>(menu);
                            page.put("operations", new java.util.ArrayList<Map<String, Object>>());
                            ((List<Map<String, Object>>) virtualModule.get("pages")).add(page);
                        }
                    } else {
                        // 独立的页面（没有父目录），创建为单页面模块
                        Map<String, Object> singlePageModule = new HashMap<>(menu);
                        singlePageModule.put("is_single_page", true);
                        singlePageModule.put("pages", new java.util.ArrayList<Map<String, Object>>());
                        
                        // 将自己作为一个页面添加
                        Map<String, Object> selfAsPage = new HashMap<>(menu);
                        selfAsPage.put("operations", new java.util.ArrayList<Map<String, Object>>());
                        ((List<Map<String, Object>>) singlePageModule.get("pages")).add(selfAsPage);
                        
                        modules.add(singlePageModule);
                    }
                }
            }
            
            // 第三步：处理三级按钮（操作）
            for (Map<String, Object> menu : allMenus) {
                Integer menuType = (Integer) menu.get("menu_type");
                if (menuType == 3) {
                    Long parentId = menu.get("parent_id") != null ? ((Number) menu.get("parent_id")).longValue() : 0L;
                    
                    // 在所有模块的页面中查找父页面
                    for (Map<String, Object> module : modules) {
                        for (Map<String, Object> page : (List<Map<String, Object>>) module.get("pages")) {
                            if (page.get("id").equals(parentId)) {
                                ((List<Map<String, Object>>) page.get("operations")).add(menu);
                                break;
                            }
                        }
                    }
                }
            }
            
            // 过滤掉没有 id 的模块（安全检查）
            modules.removeIf(module -> module.get("id") == null);
            
            // 按 sort_order 对模块重新排序，确保与菜单管理页面顺序一致
            modules.sort((a, b) -> {
                Object ao = a.get("sort_order");
                Object bo = b.get("sort_order");
                int av = ao != null ? ((Number) ao).intValue() : 0;
                int bv = bo != null ? ((Number) bo).intValue() : 0;
                return Integer.compare(av, bv);
            });
            
            System.out.println("=== 角色权限页面 - 角色ID: " + id + " ===");
            System.out.println("=== 模块数量: " + modules.size() + " ===");
            for (Map<String, Object> module : modules) {
                Object moduleId = module.get("id");
                String name = (String) module.get("menu_name");
                List<?> pages = (List<?>) module.get("pages");
                System.out.println("  - " + name + " (ID: " + moduleId + ", 页面数: " + pages.size() + ")");
            }
            
            model.addAttribute("modules", modules);
            
            // 查询该角色已分配的菜单权限
            List<Map<String, Object>> roleMenus = jdbcTemplate.queryForList(
                "SELECT menu_id FROM tb_role_menu WHERE role_id = ?", id
            );
            List<Long> menuIds = new java.util.ArrayList<>();
            if (roleMenus != null) {
                for (Map<String, Object> rm : roleMenus) {
                    Object menuId = rm.get("menu_id");
                    if (menuId != null) {
                        menuIds.add(((Number) menuId).longValue());
                    }
                }
            }
            model.addAttribute("menuIds", menuIds);
            System.out.println("=== 已分配权限数量: " + menuIds.size() + " ===");
        
            return "role/form";
        } catch (Exception e) {
            System.err.println("=== 角色权限页面错误 ===");
            e.printStackTrace();
            model.addAttribute("error", "加载权限数据失败: " + e.getMessage());
            return "error";
        }
    }

    /**
     * 保存角色权限
     */
    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(
        @RequestParam Long roleId,
        @RequestParam(required = false) List<Long> menuIds
    ) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 删除旧的权限
            jdbcTemplate.update("DELETE FROM tb_role_menu WHERE role_id = ?", roleId);
            
            // 添加新的权限
            if (menuIds != null && !menuIds.isEmpty()) {
                for (Long menuId : menuIds) {
                    jdbcTemplate.update(
                        "INSERT INTO tb_role_menu (role_id, menu_id) VALUES (?, ?)",
                        roleId, menuId
                    );
                }
            }
            
            result.put("success", true);
            result.put("message", "权限保存成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "保存失败：" + e.getMessage());
        }
        
        return result;
    }
}
