package com.example.hello.config;

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
            // 查询所有启用菜单（目录+页面，包含首页），按 sort_order 排序
            // 注意：直接 ORDER BY sort_order 会把子菜单（sort_order=0,1,2）混在顶层菜单前面
            // 所以先查顶层菜单，再追加子菜单，保证顶层顺序正确
            List<Map<String, Object>> topMenus = jdbcTemplate.queryForList(
                "SELECT id, menu_code, menu_name, menu_url, menu_icon, " +
                "menu_type, parent_id, status, sort_order " +
                "FROM tb_menu " +
                "WHERE status = 1 AND menu_type IN (1, 2) AND (parent_id IS NULL OR parent_id = 0) " +
                "ORDER BY sort_order ASC"
            );
            List<Map<String, Object>> subMenus = jdbcTemplate.queryForList(
                "SELECT id, menu_code, menu_name, menu_url, menu_icon, " +
                "menu_type, parent_id, status, sort_order " +
                "FROM tb_menu " +
                "WHERE status = 1 AND menu_type IN (1, 2) AND parent_id IS NOT NULL AND parent_id != 0 " +
                "ORDER BY sort_order ASC"
            );
            List<Map<String, Object>> allMenus = new java.util.ArrayList<>(topMenus);
            allMenus.addAll(subMenus);
            
            // 调试日志：输出菜单数量
            System.out.println("[MenuAdvice] 顶层菜单数量: " + topMenus.size() + ", 子菜单数量: " + subMenus.size());
            for (Map<String, Object> menu : topMenus) {
                System.out.println("[MenuAdvice] 顶层菜单: " + menu.get("menu_code") + " - " + menu.get("menu_name"));
            }
            for (Map<String, Object> menu : subMenus) {
                System.out.println("[MenuAdvice] 子菜单: " + menu.get("menu_code") + " - " + menu.get("menu_name") + ", parent_id=" + menu.get("parent_id"));
            }
            
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
     * 记账待财务审批数量
     */
    @ModelAttribute("pendingAccountFinanceCount")
    public Integer pendingAccountFinanceCount(HttpSession session) {
        if (session.getAttribute("currentUser") == null) {
            return 0;
        }
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_account WHERE status = 1 AND approval_stage = 1",
                Integer.class);
            return count != null ? count : 0;
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
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_account WHERE status = 1 AND approval_stage = 2",
                Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
