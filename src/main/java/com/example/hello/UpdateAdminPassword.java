package com.example.hello;

import java.sql.*;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

public class UpdateAdminPassword {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        
        System.out.println("=== 使用 Argon2 更新 admin 密码 ===");
        
        // 创建 Argon2 实例
        Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id, 32, 64);
        
        // 生成密码哈希（密码：111111）
        char[] password = "111111".toCharArray();
        String hash = argon2.hash(10, 65536, 4, password);
        
        System.out.println("✓ Argon2 哈希：" + hash);
        
        // 清除敏感数据
        argon2.wipeArray(password);
        
        // 更新数据库
        String updateSql = "UPDATE tb_employee SET password = ? WHERE name = 'admin'";
        PreparedStatement pstmt = conn.prepareStatement(updateSql);
        pstmt.setString(1, hash);
        
        int rows = pstmt.executeUpdate();
        System.out.println("✓ 已更新 " + rows + " 条记录");
        
        // 验证
        ResultSet rs = pstmt.executeQuery("SELECT id, name, password FROM tb_employee WHERE name = 'admin'");
        if (rs.next()) {
            System.out.println("\n验证结果：");
            System.out.println("用户 ID: " + rs.getInt("id"));
            System.out.println("用户名：" + rs.getString("name"));
            
            // 验证密码是否正确
            boolean verified = argon2.verify(rs.getString("password"), "111111".toCharArray());
            System.out.println("密码验证：" + (verified ? "✓ 成功" : "✗ 失败"));
        }
        
        rs.close();
        pstmt.close();
        conn.close();
        
        System.out.println("\n===========================================");
        System.out.println("Argon2 密码加密完成！");
        System.out.println("登录信息：");
        System.out.println("  用户名：admin");
        System.out.println("  密  码：111111");
        System.out.println("  加密方式：Argon2id");
        System.out.println("===========================================");
    }
}
