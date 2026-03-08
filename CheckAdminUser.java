import java.sql.*;

public class CheckAdminUser {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        Statement stmt = conn.createStatement();
        
        System.out.println("=== 检查 Admin 用户信息 ===");
        ResultSet rs = stmt.executeQuery("""
            SELECT e.id, e.name, e.password, e.status, r.role_code, r.role_name
            FROM tb_employee e
            LEFT JOIN tb_role r ON e.role_id = r.id
            WHERE e.name = 'Admin'
        """);
        
        if (rs.next()) {
            System.out.println("用户 ID: " + rs.getInt("id"));
            System.out.println("用户名：" + rs.getString("name"));
            System.out.println("角色编码：" + rs.getString("role_code"));
            System.out.println("角色名称：" + rs.getString("role_name"));
            System.out.println("用户状态：" + rs.getInt("status"));
            System.out.println("密码哈希：" + rs.getString("password"));
            
            // 测试密码验证
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = 
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            boolean matches = encoder.matches("123456", rs.getString("password"));
            System.out.println("\n密码验证结果：" + (matches ? "✓ 正确" : "✗ 错误"));
        } else {
            System.out.println("未找到 Admin 用户！");
        }
        
        rs.close();
        stmt.close();
        conn.close();
    }
}
