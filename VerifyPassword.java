import java.sql.*;

public class VerifyPassword {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        Statement stmt = conn.createStatement();
        
        System.out.println("=== 检查 admin 用户的密码 ===");
        ResultSet rs = stmt.executeQuery("SELECT id, name, password FROM tb_employee WHERE name = 'admin'");
        
        if (rs.next()) {
            System.out.println("用户 ID: " + rs.getInt("id"));
            System.out.println("用户名：" + rs.getString("name"));
            System.out.println("密码哈希：" + rs.getString("password"));
            
            // 检查密码哈希格式
            String hash = rs.getString("password");
            if (hash.startsWith("$2a$10$")) {
                System.out.println("\n✓ 密码哈希格式正确（BCrypt）");
            } else {
                System.out.println("\n✗ 密码哈希格式错误！应该是 $2a$10$ 开头");
            }
        } else {
            System.out.println("未找到 admin 用户！");
        }
        
        rs.close();
        stmt.close();
        conn.close();
    }
}
