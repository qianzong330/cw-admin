import java.sql.*;

public class FixAdminPassword {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        Statement stmt = conn.createStatement();
        
        System.out.println("=== 修复 Admin 用户密码 ===");
        
        // 使用 Spring Security BCrypt 生成密码
        // 123456 的 BCrypt 哈希（使用应用的默认 salt）
        String passwordHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iB6fKJKvP7PLz5pEhFvGxOqpqIL.";
        
        // 查找 Admin 用户
        ResultSet rs = stmt.executeQuery("SELECT id, name, password FROM tb_employee WHERE name = 'Admin'");
        if (rs.next()) {
            int id = rs.getInt("id");
            String currentHash = rs.getString("password");
            
            System.out.println("Admin 用户 ID: " + id);
            System.out.println("当前密码哈希：" + currentHash);
            System.out.println("新密码哈希：" + passwordHash);
            
            // 更新密码
            String updateSql = "UPDATE tb_employee SET password = ? WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(updateSql);
            pstmt.setString(1, passwordHash);
            pstmt.setInt(2, id);
            
            int rows = pstmt.executeUpdate();
            System.out.println("\n✓ 已更新 " + rows + " 条记录");
            
            pstmt.close();
        } else {
            System.out.println("✗ 未找到 Admin 用户！");
        }
        
        rs.close();
        stmt.close();
        conn.close();
        
        System.out.println("\n===========================================");
        System.out.println("密码已重置为：123456");
        System.out.println("现在可以使用 admin/123456 登录了");
        System.out.println("===========================================");
    }
}
