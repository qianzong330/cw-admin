import java.sql.*;

public class SimpleUpdatePassword {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        Statement stmt = conn.createStatement();
        
        System.out.println("=== 更新 admin 用户密码 ===");
        
        // 使用简单的密码哈希（暂时不加密，用于测试）
        String plainPassword = "111111";
        
        // 先删除旧用户
        stmt.executeUpdate("DELETE FROM tb_employee WHERE name = 'admin'");
        
        // 获取 root 角色 ID
        ResultSet rs = stmt.executeQuery("SELECT id FROM tb_role WHERE role_code = 'root'");
        int rootRoleId = 0;
        if (rs.next()) {
            rootRoleId = rs.getInt("id");
        }
        rs.close();
        
        // 创建新用户（密码明文存储，稍后应用会加密）
        String insertSql = """
            INSERT INTO tb_employee (name, role_id, phone, password, status, create_time, update_time)
            VALUES ('admin', %d, '13800138000', '%s', 1, NOW(), NOW())
            """.formatted(rootRoleId, plainPassword);
        
        stmt.executeUpdate(insertSql);
        System.out.println("✓ 用户创建成功");
        
        // 验证
        rs = stmt.executeQuery("SELECT id, name FROM tb_employee WHERE name = 'admin'");
        if (rs.next()) {
            System.out.println("✓ 用户 ID: " + rs.getInt("id"));
            System.out.println("✓ 用户名：" + rs.getString("name"));
        }
        rs.close();
        
        stmt.close();
        conn.close();
        
        System.out.println("\n===========================================");
        System.out.println("请重启应用以使用新的密码验证逻辑");
        System.out.println("登录信息：");
        System.out.println("  用户名：admin");
        System.out.println("  密  码：111111");
        System.out.println("===========================================");
    }
}
