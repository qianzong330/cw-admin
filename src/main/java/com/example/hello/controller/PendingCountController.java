package com.example.hello.controller;

import com.example.hello.service.WorkHourConfigService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pending")
public class PendingCountController {

    @Autowired
    private WorkHourConfigService workHourConfigService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 获取各类待审批数量
     */
    @GetMapping("/count")
    public Map<String, Integer> getPendingCount(HttpSession session) {
        Map<String, Integer> counts = new HashMap<>();
        
        // 检查用户是否登录
        if (session.getAttribute("currentUser") == null) {
            counts.put("account", 0);
            counts.put("attendance", 0);
            counts.put("salary", 0);
            counts.put("workhour", 0);
            return counts;
        }
        
        // 工时配置待审批数量
        counts.put("workhour", workHourConfigService.getPendingConfigs().size());

        counts.put("account", 0); // 记账审批有单独接口，这里迎键为0

        // 考勤待审批数量
        try {
            Integer attendanceCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_attendance_month_status WHERE status IN (1, 2)",
                Integer.class);
            counts.put("attendance", attendanceCount != null ? attendanceCount : 0);
        } catch (Exception e) {
            counts.put("attendance", 0);
        }

        // 工资条待审批数量
        try {
            Integer salaryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_salary_month_status WHERE status = 1",
                Integer.class);
            counts.put("salary", salaryCount != null ? salaryCount : 0);
        } catch (Exception e) {
            counts.put("salary", 0);
        }
        
        return counts;
    }

    /**
     * 工资条待审批数量（前端 AJAX 刷新菜单徽章用）
     */
    @GetMapping("/salary/count")
    public int getSalaryPendingCount(HttpSession session) {
        if (session.getAttribute("currentUser") == null) return 0;
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
     * 返回工资条待审批和已驳回的项目月份信息（带审批角色）
     * 返回: { 
     *   pending: [{project: "预发项目（2026-01）", approverRoles: ["boss"]}],
     *   rejected: ["预发项目（2026-02）"]
     * }
     */
    @GetMapping("/salary/projects")
    public Map<String, Object> getSalaryPendingProjects(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        result.put("pending", new java.util.ArrayList<Map<String, Object>>());
        result.put("rejected", new java.util.ArrayList<String>());
            
        com.example.hello.entity.Employee currentUser = (com.example.hello.entity.Employee) session.getAttribute("currentUser");
        if (currentUser == null) return result;
        String currentRole = currentUser.getRoleCode();
            
        try {
            // 查询待审抒并获取审批人角色
            List<java.util.Map<String, Object>> pendingRows = jdbcTemplate.queryForList(
                "SELECT p.name AS proj_name, sms.`year_month` AS ym, r.role_code AS approver_role " +
                "FROM tb_salary_month_status sms " +
                "JOIN tb_project p ON sms.project_id = p.id " +
                "LEFT JOIN tb_employee e ON sms.approve_by = e.id " +
                "LEFT JOIN tb_role r ON e.role_id = r.id " +
                "WHERE sms.status = 1 ORDER BY p.name, sms.`year_month`");
                
            java.util.Map<String, java.util.List<String>> pendingRoleMap = new java.util.LinkedHashMap<>();
            for (java.util.Map<String, Object> row : pendingRows) {
                String proj = (String) row.get("proj_name");
                String ym = (String) row.get("ym");
                String role = (String) row.get("approver_role");
                String key = proj + "（" + ym + "）";
                if (!pendingRoleMap.containsKey(key)) {
                    pendingRoleMap.put(key, new java.util.ArrayList<>());
                }
                if (role != null && !pendingRoleMap.get(key).contains(role)) {
                    pendingRoleMap.get(key).add(role);
                }
            }
                
            List<Map<String, Object>> pendingList = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, java.util.List<String>> e : pendingRoleMap.entrySet()) {
                Map<String, Object> item = new HashMap<>();
                item.put("project", e.getKey());
                item.put("approverRoles", e.getValue());
                item.put("isMyApproval", e.getValue().contains(currentRole));
                pendingList.add(item);
            }
            result.put("pending", pendingList);
                
            // 查询已驳回
            List<java.util.Map<String, Object>> rejectedRows = jdbcTemplate.queryForList(
                "SELECT p.name AS proj_name, sms.`year_month` AS ym " +
                "FROM tb_salary_month_status sms " +
                "JOIN tb_project p ON sms.project_id = p.id " +
                "WHERE sms.status = 12 ORDER BY p.name, sms.`year_month`");
            java.util.Map<String, java.util.List<String>> rejectedMap = new java.util.LinkedHashMap<>();
            for (java.util.Map<String, Object> row : rejectedRows) {
                String proj = (String) row.get("proj_name");
                String ym = (String) row.get("ym");
                rejectedMap.computeIfAbsent(proj, k -> new java.util.ArrayList<>()).add(ym);
            }
            List<String> rejectedList = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, java.util.List<String>> e : rejectedMap.entrySet()) {
                rejectedList.add(e.getKey() + "（" + String.join("、", e.getValue()) + "）");
            }
            result.put("rejected", rejectedList);
        } catch (Exception e) {
            // 静默处理
        }
        return result;
    }

    /**
     * 返回考勤待审批和已驳回的项目月份信息（带审批角色）
     * 返回: { 
     *   pending: [{project: "预发项目（2026-01）", approverRoles: ["boss"]}],
     *   rejected: ["预发项目（2026-02）"]
     * }
     */
    @GetMapping("/attendance/projects")
    public Map<String, Object> getAttendancePendingProjects(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        result.put("pending", new java.util.ArrayList<Map<String, Object>>());
        result.put("rejected", new java.util.ArrayList<String>());
            
        com.example.hello.entity.Employee currentUser = (com.example.hello.entity.Employee) session.getAttribute("currentUser");
        if (currentUser == null) return result;
        String currentRole = currentUser.getRoleCode();
            
        try {
            // 查询待审批(1,2)并获取审批人角色
            List<java.util.Map<String, Object>> pendingRows = jdbcTemplate.queryForList(
                "SELECT p.name AS proj_name, ams.`year_month` AS ym, r.role_code AS approver_role " +
                "FROM tb_attendance_month_status ams " +
                "JOIN tb_project p ON ams.project_id = p.id " +
                "LEFT JOIN tb_employee e ON ams.approve_by = e.id " +
                "LEFT JOIN tb_role r ON e.role_id = r.id " +
                "WHERE ams.status IN (1, 2) ORDER BY p.name, ams.`year_month`");
                
            java.util.Map<String, java.util.List<String>> pendingRoleMap = new java.util.LinkedHashMap<>();
            for (java.util.Map<String, Object> row : pendingRows) {
                String proj = (String) row.get("proj_name");
                String ym = (String) row.get("ym");
                String role = (String) row.get("approver_role");
                String key = proj + "（" + ym + "）";
                if (!pendingRoleMap.containsKey(key)) {
                    pendingRoleMap.put(key, new java.util.ArrayList<>());
                }
                if (role != null && !pendingRoleMap.get(key).contains(role)) {
                    pendingRoleMap.get(key).add(role);
                }
            }
                
            List<Map<String, Object>> pendingList = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, java.util.List<String>> e : pendingRoleMap.entrySet()) {
                Map<String, Object> item = new HashMap<>();
                item.put("project", e.getKey());
                item.put("approverRoles", e.getValue());
                item.put("isMyApproval", e.getValue().contains(currentRole));
                pendingList.add(item);
            }
            result.put("pending", pendingList);
                
            // 查询已驳回(12)
            List<java.util.Map<String, Object>> rejectedRows = jdbcTemplate.queryForList(
                "SELECT p.name AS proj_name, ams.`year_month` AS ym " +
                "FROM tb_attendance_month_status ams " +
                "JOIN tb_project p ON ams.project_id = p.id " +
                "WHERE ams.status = 12 ORDER BY p.name, ams.`year_month`");
            java.util.Map<String, java.util.List<String>> rejectedMap = new java.util.LinkedHashMap<>();
            for (java.util.Map<String, Object> row : rejectedRows) {
                String proj = (String) row.get("proj_name");
                String ym = (String) row.get("ym");
                rejectedMap.computeIfAbsent(proj, k -> new java.util.ArrayList<>()).add(ym);
            }
            List<String> rejectedList = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, java.util.List<String>> e : rejectedMap.entrySet()) {
                rejectedList.add(e.getKey() + "（" + String.join("、", e.getValue()) + "）");
            }
            result.put("rejected", rejectedList);
        } catch (Exception e) {
            // 静默处理
        }
        return result;
    }

    /**
     * 返回考勤已审批但工资条未提交/被驳回的项目月份信息
     * 返回: { draft: ["预发项目（2026-01）"], rejected: ["预发项目（2026-02）"] }
     * 条件：考勤状态=5(已审批) 且 工资条状态=0(草稿)或12(已驳回)
     */
    @GetMapping("/salary/missing")
    public Map<String, List<String>> getSalaryMissingProjects(HttpSession session) {
        Map<String, List<String>> result = new HashMap<>();
        result.put("draft", new java.util.ArrayList<>());
        result.put("rejected", new java.util.ArrayList<>());
        if (session.getAttribute("currentUser") == null) return result;
        try {
            // 查询考勤已审批(status=5)但工资条为草稿(0)或已驳回(12)的记录
            List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT p.name AS proj_name, ams.`year_month` AS ym, sms.status AS salary_status " +
                "FROM tb_attendance_month_status ams " +
                "JOIN tb_salary_month_status sms ON ams.project_id = sms.project_id AND ams.`year_month` = sms.`year_month` " +
                "JOIN tb_project p ON ams.project_id = p.id " +
                "WHERE ams.status = 5 AND sms.status IN (0, 12) " +
                "ORDER BY p.name, ams.`year_month`");
            java.util.Map<String, java.util.List<String>> draftMap = new java.util.LinkedHashMap<>();
            java.util.Map<String, java.util.List<String>> rejectedMap = new java.util.LinkedHashMap<>();
            for (java.util.Map<String, Object> row : rows) {
                String proj = (String) row.get("proj_name");
                String ym = (String) row.get("ym");
                Number st = (Number) row.get("salary_status");
                if (st != null && st.intValue() == 12) {
                    rejectedMap.computeIfAbsent(proj, k -> new java.util.ArrayList<>()).add(ym);
                } else {
                    draftMap.computeIfAbsent(proj, k -> new java.util.ArrayList<>()).add(ym);
                }
            }
            List<String> draftList = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, java.util.List<String>> e : draftMap.entrySet()) {
                draftList.add(e.getKey() + "\uff08" + String.join("\u3001", e.getValue()) + "\uff09");
            }
            List<String> rejectedList = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, java.util.List<String>> e : rejectedMap.entrySet()) {
                rejectedList.add(e.getKey() + "\uff08" + String.join("\u3001", e.getValue()) + "\uff09");
            }
            result.put("draft", draftList);
            result.put("rejected", rejectedList);
        } catch (Exception e) {
            // 静默处理
        }
        return result;
    }

    /**
     * 返回考勤未生效的项目月份信息
     * 条件：该项目该月份已有员工考勤数据，但月度状态不是已审批(5)
     * 即：草稿(0)/待审批(1)/变更待审(2)/已驳回(12) 且 tb_attendance表中有对应记录
     */
    @GetMapping("/attendance/missing")
    public List<String> getAttendanceMissingProjects(HttpSession session) {
        if (session.getAttribute("currentUser") == null) return java.util.Collections.emptyList();
        try {
            // 场景1: tb_attendance 有数据但 tb_attendance_month_status 无记录（未提交审批，草稿态默认显示）
            // 场景2: tb_attendance 有数据且 tb_attendance_month_status 存在但 status != 5（待审批/驳回/变更待审）
            List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT p.name AS proj_name, a.ym AS ym " +
                "FROM (SELECT DISTINCT project_id, DATE_FORMAT(work_date, '%Y-%m') AS ym " +
                "      FROM tb_attendance WHERE project_id IS NOT NULL) a " +
                "JOIN tb_project p ON a.project_id = p.id " +
                "LEFT JOIN tb_attendance_month_status ams " +
                "      ON ams.project_id = a.project_id AND ams.`year_month` = a.ym " +
                "WHERE (ams.id IS NULL OR ams.status != 5) " +
                "ORDER BY p.name, a.ym");
            java.util.Map<String, java.util.List<String>> projMap = new java.util.LinkedHashMap<>();
            for (java.util.Map<String, Object> row : rows) {
                String proj = (String) row.get("proj_name");
                String ym = (String) row.get("ym");
                projMap.computeIfAbsent(proj, k -> new java.util.ArrayList<>()).add(ym);
            }
            List<String> result = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, java.util.List<String>> e : projMap.entrySet()) {
                result.add(e.getKey() + "\uff08" + String.join("\u3001", e.getValue()) + "\uff09");
            }
            System.out.println("[attendance/missing] result size=" + result.size() + " " + result);
            return result;
        } catch (Exception e) {
            System.err.println("[attendance/missing] ERROR: " + e.getMessage());
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }
}
