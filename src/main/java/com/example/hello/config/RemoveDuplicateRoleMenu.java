package com.example.hello.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 删除重复的权限分配菜单（基础配置下的），只保留系统设置下的角色管理
 * 注意：此类现在已禁用，因为角色管理菜单已由 BasicConfigMenuInitializer 管理
 */
@Component
public class RemoveDuplicateRoleMenu implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        // 此类已禁用，不再删除任何 role 菜单
        // 角色管理菜单由 BasicConfigMenuInitializer 统一管理
        System.out.println("=== RemoveDuplicateRoleMenu 已禁用，跳过执行 ===");
    }
}
