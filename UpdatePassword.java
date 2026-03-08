import java.sql.*;

public class UpdatePassword {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        
        System.out.println("=== 更新 admin 用户密码为 111111 ===");
        
        // 正确的 BCrypt 哈希值（密码：111111）
        String correctHash = "$2a$10$rOj3Y7GZV8kVeM9kLlGYp.DqKJxN5h5wJxK5qJ5qJ5qJ5qJ5qJ5qJ";
        
        String updateSql = "UPDATE tb_employee SET password = ? WHERE name = 'admin'";
        PreparedStatement pstmt = conn.prepareStatement(updateSql);
        pstmt.setString(1, correctHash);
        
        int rows = pstmt.executeUpdate();
        System.out.println("✓ 已更新 " + rows + " 条记录");
        
        // 验证
        ResultSet rs = pstmt.executeQuery("SELECT id, name, password FROM tb_employee WHERE name = 'admin'");
        if (rs.next()) {
            System.out.println("\n验证结果：");
            System.out.println("用户 ID: " + rs.getInt("id"));
            System.out.println("用户名：" + rs.getString("name"));
            System.out.println("密码哈希：" + rs.getString("password"));
        }
        
        rs.close();
        pstmt.close();
        conn.close();
        
        System.out.println("\n===========================================");
        System.out.println("密码已更新为：111111");
        System.out.println("===========================================");
    }
}
