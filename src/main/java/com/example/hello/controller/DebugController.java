package com.example.hello.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DebugController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/nav")
    public String navMenusDebug(jakarta.servlet.http.HttpSession session) {
        StringBuilder sb = new StringBuilder();
        try {
            // 直接执行 MenuAdvice 同款查询
            List<Map<String, Object>> menus = jdbcTemplate.queryForList(
                "SELECT id, menu_code, menu_name, menu_url, menu_icon, " +
                "menu_type, parent_id, status, sort_order " +
                "FROM tb_menu " +
                "WHERE status = 1 AND menu_type IN (1, 2) AND menu_code != 'home' " +
                "ORDER BY sort_order ASC"
            );
            sb.append("MenuAdvice同款查询结果: ").append(menus.size()).append(" 条\n");
            for (Map<String, Object> m : menus) {
                sb.append("  id=").append(m.get("id"))
                  .append(" code=").append(m.get("menu_code"))
                  .append(" url=").append(m.get("menu_url"))
                  .append(" type=").append(m.get("menu_type"))
                  .append(" parent=").append(m.get("parent_id")).append("\n");
            }
            // 检查 session 里有没有 currentUser
            Object user = session.getAttribute("currentUser");
            sb.append("\nsession currentUser: ").append(user != null ? user.getClass().getSimpleName() : "null").append("\n");
            // 检查 navMenus model attribute
            Object navMenus = session.getAttribute("navMenus");
            sb.append("session navMenus: ").append(navMenus).append("\n");
        } catch (Exception e) {
            sb.append("ERROR: ").append(e.getMessage());
        }
        return "<pre>" + sb + "</pre>";
    }

    @GetMapping("/menu")
    public String menuDebug() {
        StringBuilder sb = new StringBuilder();
        try {
            // 1. 查表结构
            List<Map<String, Object>> cols = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tb_menu' ORDER BY ORDINAL_POSITION"
            );
            sb.append("=== tb_menu 字段 ===\n");
            for (Map<String, Object> c : cols) sb.append(c.get("COLUMN_NAME")).append(", ");
            sb.append("\n\n");

            // 2. 查所有菜单数据（包含 menu_url/menu_icon）
            List<Map<String, Object>> menus = jdbcTemplate.queryForList(
                "SELECT id, menu_code, menu_name, menu_type, parent_id, menu_url, menu_icon, status, sort_order FROM tb_menu WHERE menu_type IN (1,2) ORDER BY sort_order"
            );
            sb.append("=== 菜单数据 (").append(menus.size()).append(" 条) ===\n");
            for (Map<String, Object> m : menus) {
                sb.append("id=").append(m.get("id"))
                  .append(" code=").append(m.get("menu_code"))
                  .append(" name=").append(m.get("menu_name"))
                  .append(" type=").append(m.get("menu_type"))
                  .append(" parent=").append(m.get("parent_id"))
                  .append(" url=").append(m.get("menu_url"))
                  .append(" status=").append(m.get("status"))
                  .append("\n");
            }

            // 3. 查 root 角色权限
            sb.append("\n=== root 角色(id=1)菜单权限 ===\n");
            List<Map<String, Object>> roleMenus = jdbcTemplate.queryForList(
                "SELECT m.menu_code, m.menu_name FROM tb_role_menu rm JOIN tb_menu m ON rm.menu_id=m.id WHERE rm.role_id=1"
            );
            for (Map<String, Object> m : roleMenus) {
                sb.append(m.get("menu_code")).append(" - ").append(m.get("menu_name")).append("\n");
            }

        } catch (Exception e) {
            sb.append("ERROR: ").append(e.getMessage());
        }
        return "<pre>" + sb + "</pre>";
    }

    @GetMapping("/attendance/{employeeId}")
    public String attendanceDebug(@PathVariable Long employeeId,
                                   @RequestParam String yearMonth) {
        StringBuilder sb = new StringBuilder();
        try {
            List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT id, work_date, attendance_type, overtime_type, work_hours, status " +
                "FROM tb_attendance " +
                "WHERE employee_id = ? AND DATE_FORMAT(work_date, '%Y-%m') = ? " +
                "ORDER BY work_date",
                employeeId, yearMonth
            );
            sb.append("=== 员工 ").append(employeeId).append(" 的考勤数据 (").append(yearMonth).append(") ===\n");
            sb.append("共 ").append(list.size()).append(" 条记录\n\n");
            
            double weekdayHours = 0, restdayHours = 0, holidayHours = 0;
            for (Map<String, Object> a : list) {
                Integer attType = (Integer) a.get("attendance_type");
                Integer otType = (Integer) a.get("overtime_type");
                Double hours = ((Number) a.get("work_hours")).doubleValue();
                
                sb.append("日期=").append(a.get("work_date"))
                  .append(" 类型=").append(attType)
                  .append(" 加班类型=").append(otType)
                  .append(" 工时=").append(hours).append("h")
                  .append(" 状态=").append(a.get("status")).append("\n");
                
                if (attType != null && attType == 2) {
                    if (otType != null && otType == 2) {
                        restdayHours += hours;
                    } else if (otType != null && otType == 3) {
                        holidayHours += hours;
                    } else {
                        weekdayHours += hours;
                    }
                }
            }
            
            sb.append("\n=== 统计结果 ===\n");
            sb.append("工作日加班=").append(weekdayHours).append("h\n");
            sb.append("休息日加班=").append(restdayHours).append("h\n");
            sb.append("节假日加班=").append(holidayHours).append("h\n");
            
        } catch (Exception e) {
            sb.append("ERROR: ").append(e.getMessage());
            e.printStackTrace();
        }
        return "<pre>" + sb + "</pre>";
    }

    @GetMapping("/fixBossPermission")
    public String fixBossPermission() {
        try {
            List<Map<String, Object>> bossRole = jdbcTemplate.queryForList(
                "SELECT id FROM tb_role WHERE role_code = 'boss'");
            if (bossRole.isEmpty()) return "<pre>boss role not found</pre>";
            Long bossId = ((Number) bossRole.get(0).get("id")).longValue();
            String[] codes = {"system_settings", "menu:list", "role"};
            int count = 0;
            for (String code : codes) {
                try {
                    Long menuId = jdbcTemplate.queryForObject(
                        "SELECT id FROM tb_menu WHERE menu_code = ?", Long.class, code);
                    jdbcTemplate.update(
                        "INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (?, ?)", bossId, menuId);
                    count++;
                } catch (Exception e) { /* skip */ }
            }
            return "<pre>Done. Assigned " + count + " permissions to boss role (id=" + bossId + ")</pre>";
        } catch (Exception e) {
            return "<pre>ERROR: " + e.getMessage() + "</pre>";
        }
    }

    @GetMapping("/fixRole")
    public String fixRole() {
        try {
            // 查 system_settings id
            Long sysId = jdbcTemplate.queryForObject("SELECT id FROM tb_menu WHERE menu_code='system_settings'", Long.class);
            // 检查 role 是否存在
            Integer cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_menu WHERE menu_code='role'", Integer.class);
            if (cnt == null || cnt == 0) {
                jdbcTemplate.update(
                    "INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status) VALUES (?, ?, ?, 2, '/role/list', 'bi-person-badge', 0, 1)",
                    "role", "\u89d2\u8272\u7ba1\u7406", sysId
                );
                Long newId = jdbcTemplate.queryForObject("SELECT id FROM tb_menu WHERE menu_code='role'", Long.class);
                jdbcTemplate.update("INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (1, ?)", newId);
                // 更新 role:edit 的 parent_id
                jdbcTemplate.update("UPDATE tb_menu SET parent_id=? WHERE menu_code='role:edit'", newId);
                return "<pre>Created role menu id=" + newId + " under system_settings id=" + sysId + "</pre>";
            } else {
                jdbcTemplate.update("UPDATE tb_menu SET parent_id=?, menu_type=2, menu_url='/role/list', menu_icon='bi-person-badge', sort_order=0, status=1 WHERE menu_code='role'", sysId);
                Long roleId = jdbcTemplate.queryForObject("SELECT id FROM tb_menu WHERE menu_code='role'", Long.class);
                jdbcTemplate.update("INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (1, ?)", roleId);
                return "<pre>Updated role menu, now under system_settings id=" + sysId + "</pre>";
            }
        } catch (Exception e) {
            return "<pre>ERROR: " + e.getMessage() + "</pre>";
        }
    }

    @GetMapping("/roleMenu")
    public String roleMenu() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, menu_code, menu_name, menu_type, parent_id, menu_url, status, sort_order FROM tb_menu WHERE menu_code LIKE 'role%'"
        );
        StringBuilder sb = new StringBuilder("<pre>");
        for (Map<String, Object> r : rows) sb.append(r).append("\n");
        return sb.append("</pre>").toString();
    }

    @GetMapping("/fixAllButtonParents")
    public String fixAllButtonParents() {
        StringBuilder sb = new StringBuilder();
        try {
            // 修复所有按鈕的 parent_id 与对应页面实际 id 对齐
            // 格式: UPDATE tb_menu SET parent_id=(SELECT id ... WHERE menu_code='xxx') WHERE menu_code IN (...)
            Object[][] fixes = {
                // {"页面menu_code", new String[]{"btn_code1","btn_code2",...}}
                {"account",    new String[]{"account:add","account:edit","account:delete","account:approve"}},
                {"employee",   new String[]{"employee:add","employee:edit","employee:delete"}},
                {"salary",     new String[]{"salary:add","salary:edit","salary:delete","salary:pay"}},
                {"attendance", new String[]{"attendance:add","attendance:edit","attendance:delete"}},
                {"project",    new String[]{"project:add","project:edit","project:delete","project:assign"}},
                {"category",   new String[]{"category:add","category:edit","category:delete"}},
                {"jobcategory",new String[]{"jobcategory:add","jobcategory:edit","jobcategory:delete"}},
                {"role",       new String[]{"role:edit"}},
            };
            for (Object[] fix : fixes) {
                String pageCode = (String) fix[0];
                String[] btnCodes = (String[]) fix[1];
                try {
                    Long pageId = jdbcTemplate.queryForObject(
                        "SELECT id FROM tb_menu WHERE menu_code = ?", Long.class, pageCode);
                    for (String btn : btnCodes) {
                        int n = jdbcTemplate.update(
                            "UPDATE tb_menu SET parent_id = ? WHERE menu_code = ?", pageId, btn);
                        if (n > 0) sb.append("✓ ").append(btn).append(" -> parent=").append(pageId).append("\n");
                    }
                } catch (Exception e) {
                    sb.append("✗ ").append(pageCode).append(": ").append(e.getMessage()).append("\n");
                }
            }
            sb.append("\nDone!");
        } catch (Exception e) {
            sb.append("ERROR: ").append(e.getMessage());
        }
        return "<pre>" + sb + "</pre>";
    }

    @GetMapping("/fixCategoryParent")
    public String fixCategoryParent() {
        try {
            // category:add/edit/delete 的 parent_id 错误地指向了 6，应该指向 category(id=5)
            Long categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_menu WHERE menu_code = 'category'", Long.class);
            int updated = jdbcTemplate.update(
                "UPDATE tb_menu SET parent_id = ? WHERE menu_code IN ('category:add','category:edit','category:delete')",
                categoryId);
            return "<pre>Fixed! Updated " + updated + " rows. category buttons now point to parent_id=" + categoryId + "</pre>";
        } catch (Exception e) {
            return "<pre>ERROR: " + e.getMessage() + "</pre>";
        }
    }

    @GetMapping("/fixButtonPermissions")
    public String fixButtonPermissions() {
        StringBuilder sb = new StringBuilder();
        try {
            Long bossId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_role WHERE role_code = 'boss'", Long.class);
            Long rootId = 1L;

            // 1. 补全 jobcategory 的按钮权限（如果不存在）
            Long jobcategoryMenuId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_menu WHERE menu_code = 'jobcategory'", Long.class);
            String[][] jobBtns = {
                {"jobcategory:add", "新增工种"},
                {"jobcategory:edit", "编辑工种"},
                {"jobcategory:delete", "删除工种"}
            };
            for (String[] btn : jobBtns) {
                Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tb_menu WHERE menu_code = ?", Integer.class, btn[0]);
                if (cnt == null || cnt == 0) {
                    jdbcTemplate.update(
                        "INSERT INTO tb_menu (menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES (?, ?, 3, ?, 0, 1)",
                        btn[0], btn[1], jobcategoryMenuId);
                    sb.append("Created: ").append(btn[0]).append("\n");
                }
            }

            // 2. 给 boss 和 root 分配所有 category 和 jobcategory 相关权限
            String[] allCodes = {
                "category", "category:add", "category:edit", "category:delete",
                "jobcategory", "jobcategory:add", "jobcategory:edit", "jobcategory:delete"
            };
            int assigned = 0;
            for (String code : allCodes) {
                try {
                    Long menuId = jdbcTemplate.queryForObject(
                        "SELECT id FROM tb_menu WHERE menu_code = ?", Long.class, code);
                    jdbcTemplate.update(
                        "INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (?, ?)", bossId, menuId);
                    jdbcTemplate.update(
                        "INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (?, ?)", rootId, menuId);
                    assigned++;
                } catch (Exception e) { sb.append("Skip ").append(code).append(": ").append(e.getMessage()).append("\n"); }
            }
            sb.append("Assigned ").append(assigned).append(" permissions to boss and root.\n");
            sb.append("Done!");
        } catch (Exception e) {
            sb.append("ERROR: ").append(e.getMessage());
        }
        return "<pre>" + sb + "</pre>";
    }


    @GetMapping("/topMenus")
    public String topMenusDebug() {
        StringBuilder sb = new StringBuilder();
        try {
            List<Map<String, Object>> menus = jdbcTemplate.queryForList(
                "SELECT id, menu_code, menu_name, menu_type, parent_id, sort_order " +
                "FROM tb_menu WHERE status=1 ORDER BY sort_order, id"
            );
            sb.append("所有菜单（按sort_order排序）:\n");
            for (Map<String, Object> m : menus) {
                sb.append("  id=").append(m.get("id"))
                  .append(" code=").append(m.get("menu_code"))
                  .append(" name=").append(m.get("menu_name"))
                  .append(" type=").append(m.get("menu_type"))
                  .append(" parent=").append(m.get("parent_id"))
                  .append(" sort_order=").append(m.get("sort_order")).append("\n");
            }
        } catch (Exception e) {
            sb.append("ERROR: ").append(e.getMessage());
        }
        return "<pre>" + sb + "</pre>";
    }
}
