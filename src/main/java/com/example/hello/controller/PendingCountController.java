package com.example.hello.controller;

import com.example.hello.service.WorkHourConfigService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
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
        
        counts.put("account", 0);
        counts.put("attendance", 0);
        counts.put("salary", 0);
        
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
}
