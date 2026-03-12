package com.example.hello.config;

import com.example.hello.entity.Employee;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Map;

/**
 * 全局菜单数据注入：每个页面自动获取导航菜单列表
 */
@ControllerAdvice
public class MenuAdvice {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 自动注入 navMenus 到所有页面的 model
     */
    @ModelAttribute("navMenus")
    public List<Map<String, Object>> navMenus(HttpSession session) {
        // 未登录时不查询
        if (session.getAttribute("currentUser") == null) {
            return null;
        }
        try {
            Employee currentUser = (Employee) session.getAttribute("currentUser");
            Long roleId = currentUser.getRoleId();
            
            List<Map<String, Object>> topMenus;
            List<Map<String, Object>> subMenus;
            
            if (roleId != null) {
                // 所有角色一律只查询该角色已分配的菜单
                topMenus = jdbcTemplate.queryForList(
                    "SELECT m.id, m.menu_code, m.menu_name, m.menu_url, m.menu_icon, " +
                    "m.menu_type, m.parent_id, m.status, m.sort_order " +
                    "FROM tb_menu m " +
                    "INNER JOIN tb_role_menu rm ON m.id = rm.menu_id " +
                    "WHERE m.status = 1 AND m.menu_type IN (1, 2) " +
                    "AND (m.parent_id IS NULL OR m.parent_id = 0) " +
                    "AND rm.role_id = ? " +
                    "ORDER BY m.sort_order ASC",
                    roleId
                );
                subMenus = jdbcTemplate.queryForList(
                    "SELECT m.id, m.menu_code, m.menu_name, m.menu_url, m.menu_icon, " +
                    "m.menu_type, m.parent_id, m.status, m.sort_order " +
                    "FROM tb_menu m " +
                    "INNER JOIN tb_role_menu rm ON m.id = rm.menu_id " +
                    "WHERE m.status = 1 AND m.menu_type IN (1, 2) " +
                    "AND m.parent_id IS NOT NULL AND m.parent_id != 0 " +
                    "AND rm.role_id = ? " +
                    "ORDER BY m.sort_order ASC",
                    roleId
                );
            } else {
                // 无角色ID，返回空列表
                topMenus = new java.util.ArrayList<>();
                subMenus = new java.util.ArrayList<>();
            }
            
            List<Map<String, Object>> allMenus = new java.util.ArrayList<>(topMenus);
            allMenus.addAll(subMenus);
            return allMenus;
        } catch (Exception e) {
            System.err.println("MenuAdvice 查询失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 待审批数量徽章：考勤、工时、工资条待审总数
     */
    @ModelAttribute("pendingApprovalCount")
    public Integer pendingApprovalCount(HttpSession session) {
        if (session.getAttribute("currentUser") == null) {
            return 0;
        }
        try {
            // 查询待审批的月度考勤表数量
            Integer attendanceCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_attendance_month_status WHERE status IN (1, 2)",
                Integer.class);
            // 查询待审批的工时配置数量（如果有这个表的话）
            Integer workhourCount = 0;
            try {
                workhourCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tb_work_hour_config WHERE status = 1",
                    Integer.class);
            } catch (Exception ignore) {}
            // 查询待审批的工资条数量
            Integer salaryCount = 0;
            try {
                salaryCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tb_salary_slip WHERE status = 1",
                    Integer.class);
            } catch (Exception ignore) {}
            
            return (attendanceCount != null ? attendanceCount : 0) + 
                   (workhourCount != null ? workhourCount : 0) + 
                   (salaryCount != null ? salaryCount : 0);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 考勤待审批数量（子菜单徽章用）
     */
    @ModelAttribute("pendingAttendanceCount")
    public Integer pendingAttendanceCount(HttpSession session) {
        if (session.getAttribute("currentUser") == null) {
            return 0;
        }
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_attendance_month_status WHERE status IN (1, 2)",
                Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 工时待审批数量（子菜单徽章用）
     */
    @ModelAttribute("pendingWorkhourCount")
    public Integer pendingWorkhourCount(HttpSession session) {
        if (session.getAttribute("currentUser") == null) {
            return 0;
        }
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_work_hour_config WHERE status = 1",
                Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 工资条待审批数量（子菜单徽章用）- 查询月份状态表
     */
    @ModelAttribute("pendingSalaryCount")
    public Integer pendingSalaryCount(HttpSession session) {
        if (session.getAttribute("currentUser") == null) {
            return 0;
        }
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_salary_month_status WHERE status = 1",
                Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 记账待管理员审批数量（当前用户是项目管理员的项目中的待审批帐条）
     */
    @ModelAttribute("pendingAccountAdminCount")
    public Integer pendingAccountAdminCount(HttpSession session) {
        if (session.getAttribute("currentUser") == null) {
            return 0;
        }
        try {
            Employee user = (Employee) session.getAttribute("currentUser");
            Long userId = user.getId();
            if (userId == null) {
                return 0;
            }
            
            String roleCode = user.getRoleCode() != null ? user.getRoleCode().toLowerCase() : "";
            boolean isBoss = "boss".equals(roleCode) || "root".equals(roleCode);
            boolean isAdmin = "admin".equals(roleCode);
            
            if (isBoss) {
                // BOSS看到所有待管理员审批的帐条（实际上不展示，因为boss看的是bossCount）
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tb_account WHERE status = 1 AND approval_stage = 1",
                    Integer.class);
                return count != null ? count : 0;
            } else if (isAdmin) {
                // admin角色：看自己管理的项目中待审批的帐条
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tb_account a " +
                    "WHERE a.status = 1 AND a.approval_stage = 1 " +
                    "AND a.project_id IN (SELECT pa.project_id FROM tb_project_admin pa WHERE pa.employee_id = ?)",
                    Integer.class, userId);
                return count != null ? count : 0;
            } else {
                // 普通员工：看自己作为项目管理员的项目中的待审批帐条
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tb_account a " +
                    "WHERE a.status = 1 AND a.approval_stage = 1 " +
                    "AND a.project_id IN (SELECT pa.project_id FROM tb_project_admin pa WHERE pa.employee_id = ?)",
                    Integer.class, userId);
                return count != null ? count : 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 记账待BOSS审批数量
     */
    @ModelAttribute("pendingAccountBossCount")
    public Integer pendingAccountBossCount(HttpSession session) {
        if (session.getAttribute("currentUser") == null) {
            return 0;
        }
        try {
            Employee user = (Employee) session.getAttribute("currentUser");
            String roleCode = user.getRoleCode() != null ? user.getRoleCode().toLowerCase() : "";
            boolean isBoss = "boss".equals(roleCode) || "root".equals(roleCode);
            // 只有BOSS角色才需要统计数量
            if (!isBoss) {
                return 0;
            }
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_account WHERE status = 1 AND approval_stage = 2",
                Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
