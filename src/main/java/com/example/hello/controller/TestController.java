package com.example.hello.controller;

import com.example.hello.service.SalarySlipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试控制器 - 检查用户和角色数据
 */
@RestController
public class TestController {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private SalarySlipService salarySlipService;
    
    @GetMapping("/api/test/check-data")
    public Map<String, Object> checkData() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 查询所有员工
            String empSql = "SELECT id, name, role_id, status FROM tb_employee ORDER BY id";
            List<Map<String, Object>> employees = jdbcTemplate.queryForList(empSql);
            result.put("employees", employees);
            
            // 查询所有角色
            String roleSql = "SELECT id, role_code, role_name FROM tb_role ORDER BY id";
            List<Map<String, Object>> roles = jdbcTemplate.queryForList(roleSql);
            result.put("roles", roles);
            
            // 查询所有菜单
            try {
                String menuSql = "SELECT id, menu_code, menu_name, menu_type, parent_id FROM tb_menu ORDER BY id";
                List<Map<String, Object>> menus = jdbcTemplate.queryForList(menuSql);
                result.put("menus", menus);
                result.put("menuCount", menus.size());
            } catch (Exception e) {
                result.put("menuError", e.getMessage());
            }
            
            // 查询角色菜单关联
            try {
                String roleMenuSql = "SELECT role_id, menu_id FROM tb_role_menu ORDER BY role_id, menu_id";
                List<Map<String, Object>> roleMenus = jdbcTemplate.queryForList(roleMenuSql);
                result.put("roleMenus", roleMenus);
                result.put("roleMenuCount", roleMenus.size());
            } catch (Exception e) {
                result.put("roleMenuError", e.getMessage());
            }
            
            // 测试登录查询
            String loginSql = "SELECT e.id, e.name, e.role_id, e.status FROM tb_employee e WHERE e.name = ? AND e.password = ? AND e.status = 1";
            try {
                Map<String, Object> loginUser = jdbcTemplate.queryForMap(loginSql, "韩世昌", "1");
                result.put("loginTest", "SUCCESS");
                result.put("loginUser", loginUser);
                
                // 测试查询角色信息
                Long employeeId = ((Number) loginUser.get("id")).longValue();
                try {
                    String roleSql2 = "SELECT r.role_code, r.role_name FROM tb_role r INNER JOIN tb_employee e ON r.id = e.role_id WHERE e.id = ?";
                    Map<String, Object> roleInfo = jdbcTemplate.queryForMap(roleSql2, employeeId);
                    result.put("roleInfo", roleInfo);
                    
                    // 测试查询菜单权限
                    try {
                        String menuCodeSql = "SELECT m.menu_code FROM tb_menu m INNER JOIN tb_role_menu rm ON m.id = rm.menu_id INNER JOIN tb_role r ON rm.role_id = r.id WHERE r.role_code = ? AND m.status = 1 ORDER BY m.sort_order";
                        List<Map<String, Object>> menuCodes = jdbcTemplate.queryForList(menuCodeSql, roleInfo.get("role_code"));
                        result.put("menuCodes", menuCodes);
                        result.put("menuCodeCount", menuCodes.size());
                    } catch (Exception e2) {
                        result.put("menuCodesError", e2.getMessage());
                    }
                } catch (Exception e1) {
                    result.put("roleInfoError", e1.getMessage());
                }
            } catch (Exception e) {
                result.put("loginTest", "FAILED");
                result.put("loginError", e.getMessage());
            }
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * 测试：手动触发工资条批量创建
     */
    @GetMapping("/api/test/create-salary")
    public Map<String, Object> createSalary(@RequestParam Long projectId, @RequestParam String period) {
        Map<String, Object> result = new HashMap<>();
        try {
            salarySlipService.batchCreateForProject(projectId, period);
            result.put("status", "success");
            
            // 查询工资条数量
            int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_salary WHERE salary_period = ?", Integer.class, period);
            result.put("salaryCount", count);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
    
    /**
     * 查询数据库状态
     */
    @GetMapping("/api/test/db-status")
    public Map<String, Object> dbStatus() {
        Map<String, Object> result = new HashMap<>();
        
        // 考勤月度状态
        result.put("attendanceMonthStatus", jdbcTemplate.queryForList(
            "SELECT id, `year_month`, project_id, status FROM tb_attendance_month_status"));
        
        // 工资条
        result.put("salarySlips", jdbcTemplate.queryForList(
            "SELECT id, employee_id, salary_period, status FROM tb_salary"));
        
        // 项目
        result.put("projects", jdbcTemplate.queryForList(
            "SELECT id, name FROM tb_project"));
        
        return result;
    }
}
