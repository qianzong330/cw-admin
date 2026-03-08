package com.example.hello.service;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PasswordEncryptionService implements CommandLineRunner {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== 检查并加密明文密码 ===");
        
        // 查询所有明文密码的用户（密码不是以 $argon2 开头）
        String sql = "SELECT id, name, password FROM tb_employee WHERE password NOT LIKE ?";
        String argon2Prefix = "$argon2";
        
        jdbcTemplate.query(sql, rs -> {
            Long id = rs.getLong("id");
            String name = rs.getString("name");
            String plainPassword = rs.getString("password");
            
            System.out.println("发现明文密码用户：" + name);
            
            // 使用 Argon2 加密密码
            Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id, 32, 64);
            char[] passwordChars = plainPassword.toCharArray();
            String hash = argon2.hash(10, 65536, 4, passwordChars);
            argon2.wipeArray(passwordChars);
            
            // 更新数据库
            String updateSql = "UPDATE tb_employee SET password = ? WHERE id = ?";
            int rows = jdbcTemplate.update(updateSql, hash, id);
            
            System.out.println("✓ 已加密用户 " + name + " 的密码，更新 " + rows + " 条记录");
        }, "%" + argon2Prefix + "%");
        
        System.out.println("=== 密码加密完成 ===\n");
    }
}
