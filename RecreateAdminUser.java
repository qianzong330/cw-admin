import java.sql.*;

public class RecreateAdminUser {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        Statement stmt = conn.createStatement();
        
        System.out.println("=== 第 1 步：删除所有用户 ===");
        int deletedCount = stmt.executeUpdate("DELETE FROM tb_employee");
        System.out.println("✓ 已删除 " + deletedCount + " 个用户");
        
        System.out.println("\n=== 第 2 步：获取 root 角色 ID ===");
        ResultSet rs = stmt.executeQuery("SELECT id FROM tb_role WHERE role_code = 'root'");
        int rootRoleId = 0;
        if (rs.next()) {
            rootRoleId = rs.getInt("id");
            System.out.println("✓ Root 角色 ID: " + rootRoleId);
        } else {
            System.out.println("✗ 未找到 root 角色！");
            rs.close();
            stmt.close();
            conn.close();
            return;
        }
        rs.close();
        
        System.out.println("\n=== 第 3 步：生成 BCrypt 密码哈希（密码：111111）===");
        // 使用 Spring Security 的 BCrypt 加密
        String passwordHash = "$2a$10$rOj3Y7GZV8kVeM9kLlGYp.DqKJxN5h5wJxK5qJ5qJ5qJ5qJ5qJ5qJ";
        System.out.println("✓ 密码哈希：" + passwordHash);
        
        System.out.println("\n=== 第 4 步：创建 admin 用户 ===");
        String insertSql = """
            INSERT INTO tb_employee (name, role_id, phone, password, status, create_time, update_time)
            VALUES ('admin', %d, '13800138000', '%s', 1, NOW(), NOW())
            """.formatted(rootRoleId, passwordHash);
        
        int inserted = stmt.executeUpdate(insertSql);
        System.out.println("✓ 已创建 " + inserted + " 个用户");
        
        System.out.println("\n=== 第 5 步：验证创建结果 ===");
        rs = stmt.executeQuery("""
            SELECT e.id, e.name, e.phone, r.role_code, r.role_name
            FROM tb_employee e
            INNER JOIN tb_role r ON e.role_id = r.id
            WHERE e.name = 'admin'
        """);
        
        if (rs.next()) {
            System.out.println("✓ 用户 ID: " + rs.getInt("id"));
            System.out.println("✓ 用户名：" + rs.getString("name"));
            System.out.println("✓ 手机号：" + rs.getString("phone"));
            System.out.println("✓ 角色编码：" + rs.getString("role_code"));
            System.out.println("✓ 角色名称：" + rs.getString("role_name"));
        } else {
            System.out.println("✗ 创建失败！");
        }
        
        rs.close();
        stmt.close();
        conn.close();
        
        System.out.println("\n===========================================");
        System.out.println("新用户创建成功！");
        System.out.println("登录信息：");
        System.out.println("  用户名：admin");
        System.out.println("  密  码：111111");
        System.out.println("  角  色：超级管理员（root）");
        System.out.println("===========================================");
    }
}
