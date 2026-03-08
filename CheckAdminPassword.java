import java.sql.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class CheckAdminPassword {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        Statement stmt = conn.createStatement();
        
        System.out.println("=== 检查 Admin 用户密码 ===");
        ResultSet rs = stmt.executeQuery("""
            SELECT e.id, e.name, e.password, r.role_code 
            FROM tb_employee e
            INNER JOIN tb_role r ON e.role_id = r.id
            WHERE e.name = 'Admin'
        """);
        
        if (rs.next()) {
            int id = rs.getInt("id");
            String name = rs.getString("name");
            String passwordHash = rs.getString("password");
            String roleCode = rs.getString("role_code");
            
            System.out.println("用户 ID: " + id);
            System.out.println("用户名：" + name);
            System.out.println("角色编码：" + roleCode);
            System.out.println("密码哈希：" + passwordHash);
            
            // 验证密码
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            boolean matches = encoder.matches("123456", passwordHash);
            System.out.println("\n密码验证结果：" + (matches ? "✓ 正确" : "✗ 错误"));
            
            if (!matches) {
                System.out.println("\n正在重新生成正确的密码哈希...");
                String newHash = encoder.encode("123456");
                System.out.println("新密码哈希：" + newHash);
                
                // 更新数据库
                String updateSql = "UPDATE tb_employee SET password = '" + newHash + "' WHERE id = " + id;
                int rows = stmt.executeUpdate(updateSql);
                System.out.println("已更新 " + rows + " 条记录");
                
                // 再次验证
                rs = stmt.executeQuery("SELECT password FROM tb_employee WHERE id = " + id);
                if (rs.next()) {
                    String updatedHash = rs.getString("password");
                    boolean verifyMatches = encoder.matches("123456", updatedHash);
                    System.out.println("验证新密码：" + (verifyMatches ? "✓ 成功" : "✗ 失败"));
                }
            }
        } else {
            System.out.println("未找到 Admin 用户！");
        }
        
        rs.close();
        stmt.close();
        conn.close();
    }
}
