package com.example.hello.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DebugController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/debug/employee")
    public List<Map<String, Object>> debugEmployee() {
        return jdbcTemplate.queryForList(
            "SELECT e.id, e.name, e.role_id, r.role_code, r.role_name " +
            "FROM tb_employee e " +
            "LEFT JOIN tb_role r ON e.role_id = r.id " +
            "WHERE e.name = '韩世昌'"
        );
    }

    @GetMapping("/debug/roles")
    public List<Map<String, Object>> debugRoles() {
        return jdbcTemplate.queryForList(
            "SELECT r.*, COUNT(rm.id) as menu_count " +
            "FROM tb_role r " +
            "LEFT JOIN tb_role_menu rm ON r.id = rm.role_id " +
            "GROUP BY r.id " +
            "ORDER BY r.id"
        );
    }

    /**
     * 诊断年度汇总数据：直接查 tb_salary 和 tb_salary_month_status JOIN 结果
     * 访问示例: /debug/year-salary?projectId=8&year=2026
     */
    @GetMapping("/debug/att-missing-noauth")
    public java.util.Map<String, Object> debugAttMissingNoAuth() {
        var result = new java.util.LinkedHashMap<String, Object>();
        // 直接查看 tb_attendance 有哪些 project+month
        result.put("att", jdbcTemplate.queryForList(
            "SELECT DISTINCT project_id, DATE_FORMAT(work_date,'%Y-%m') AS ym FROM tb_attendance WHERE project_id IS NOT NULL ORDER BY project_id, ym"));
        // 查看 tb_attendance_month_status 有哪些记录
        result.put("ams", jdbcTemplate.queryForList(
            "SELECT project_id, `year_month`, status FROM tb_attendance_month_status ORDER BY project_id, `year_month`"));
        // 执行 missing SQL
        result.put("missing", jdbcTemplate.queryForList(
            "SELECT p.name AS proj_name, a.ym, ams.id AS ams_id, ams.status AS ams_status " +
            "FROM (SELECT DISTINCT project_id, DATE_FORMAT(work_date,'%Y-%m') AS ym FROM tb_attendance WHERE project_id IS NOT NULL) a " +
            "JOIN tb_project p ON a.project_id = p.id " +
            "LEFT JOIN tb_attendance_month_status ams ON ams.project_id = a.project_id AND ams.`year_month` = a.ym " +
            "WHERE (ams.id IS NULL OR ams.status != 5) ORDER BY p.name, a.ym"));
        return result;
    }

    @GetMapping("/debug/attendance-missing")
    public java.util.Map<String, Object> debugAttendanceMissing() {
        var result = new java.util.LinkedHashMap<String, Object>();

        // 1. 查 tb_attendance 中有哪些项目月份有数据
        List<Map<String, Object>> attRows = jdbcTemplate.queryForList(
            "SELECT project_id, DATE_FORMAT(work_date, '%Y-%m') AS ym, COUNT(*) as cnt " +
            "FROM tb_attendance GROUP BY project_id, DATE_FORMAT(work_date, '%Y-%m') ORDER BY project_id, ym");
        result.put("att_rows", attRows);

        // 2. 查月度状态表
        List<Map<String, Object>> statusRows = jdbcTemplate.queryForList(
            "SELECT id, project_id, `year_month`, status FROM tb_attendance_month_status ORDER BY project_id, `year_month`");
        result.put("status_rows", statusRows);

        // 3. 执行 missing SQL 看结果
        List<Map<String, Object>> missingRows = jdbcTemplate.queryForList(
            "SELECT p.name AS proj_name, a.ym, ams.id AS ams_id, ams.status AS ams_status " +
            "FROM (SELECT DISTINCT project_id, DATE_FORMAT(work_date, '%Y-%m') AS ym " +
            "      FROM tb_attendance) a " +
            "JOIN tb_project p ON a.project_id = p.id " +
            "LEFT JOIN tb_attendance_month_status ams " +
            "      ON ams.project_id = a.project_id AND ams.`year_month` = a.ym " +
            "WHERE (ams.id IS NULL OR ams.status != 5) " +
            "ORDER BY p.name, a.ym");
        result.put("missing_rows", missingRows);

        return result;
    }

    @GetMapping("/debug/year-salary")
    public Map<String, Object> debugYearSalary(
            @RequestParam Long projectId,
            @RequestParam String year) {
        var result = new java.util.LinkedHashMap<String, Object>();

        // 1. 查 tb_salary 原始记录
        List<Map<String, Object>> salaryRows = jdbcTemplate.queryForList(
            "SELECT id, employee_id, project_id, salary_period, base_amount, " +
            "addition_amount, deduction_amount, payable_amount " +
            "FROM tb_salary WHERE project_id = ? AND salary_period LIKE ? ORDER BY salary_period",
            projectId, year + "%"
        );
        result.put("salary_rows", salaryRows);

        // 2. 查 tb_salary_month_status 原始记录
        List<Map<String, Object>> statusRows = jdbcTemplate.queryForList(
            "SELECT project_id, `year_month`, status FROM tb_salary_month_status " +
            "WHERE project_id = ? AND `year_month` LIKE ? ORDER BY `year_month`",
            projectId, year + "%"
        );
        result.put("status_rows", statusRows);

        // 3. 查 JOIN 后结果（看有几条能 JOIN 上）
        List<Map<String, Object>> joinRows = jdbcTemplate.queryForList(
            "SELECT s.salary_period, s.employee_id, sms.status, sms.`year_month` " +
            "FROM tb_salary s " +
            "INNER JOIN tb_salary_month_status sms " +
            "  ON s.project_id = sms.project_id " +
            "  AND s.salary_period COLLATE utf8mb4_unicode_ci = sms.`year_month` COLLATE utf8mb4_unicode_ci " +
            "WHERE s.project_id = ? AND s.salary_period LIKE ? " +
            "ORDER BY s.salary_period",
            projectId, year + "%"
        );
        result.put("join_rows", joinRows);

        // 4. 只看 status=5 的
        List<Map<String, Object>> approvedRows = jdbcTemplate.queryForList(
            "SELECT s.salary_period, s.employee_id, sms.status " +
            "FROM tb_salary s " +
            "INNER JOIN tb_salary_month_status sms " +
            "  ON s.project_id = sms.project_id " +
            "  AND s.salary_period COLLATE utf8mb4_unicode_ci = sms.`year_month` COLLATE utf8mb4_unicode_ci " +
            "WHERE s.project_id = ? AND s.salary_period LIKE ? AND sms.status = 5 " +
            "ORDER BY s.salary_period",
            projectId, year + "%"
        );
        result.put("approved_rows", approvedRows);

        return result;
    }
}
