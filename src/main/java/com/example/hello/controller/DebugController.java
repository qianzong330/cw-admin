package com.example.hello.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
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
}
