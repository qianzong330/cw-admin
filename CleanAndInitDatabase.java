import java.sql.*;
import java.nio.file.*;

public class CleanAndInitDatabase {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        String user = "root";
        String password = "4lsr2tq5";
        
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement stmt = conn.createStatement();
        
        System.out.println("=== 开始清空测试数据 ===");
        
        // 读取并执行清理 SQL
        String cleanSql = new String(Files.readAllBytes(Paths.get("clean_test_data.sql")));
        String[] cleanStatements = cleanSql.split(";");
        
        for (String sql : cleanStatements) {
            String trimmed = sql.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                try {
                    stmt.execute(trimmed);
                    System.out.println("✓ 执行：" + trimmed.substring(0, Math.min(50, trimmed.length())));
                } catch (Exception e) {
                    System.out.println("✗ 失败：" + e.getMessage());
                }
            }
        }
        
        System.out.println("\n=== 开始创建 root 管理员 ===");
        
        // 读取并执行初始化 SQL
        String initSql = new String(Files.readAllBytes(Paths.get("create_root_admin.sql")));
        String[] initStatements = initSql.split(";");
        
        for (String sql : initStatements) {
            String trimmed = sql.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                try {
                    stmt.execute(trimmed);
                    System.out.println("✓ 执行：" + trimmed.substring(0, Math.min(50, trimmed.length())));
                } catch (Exception e) {
                    System.out.println("✗ 失败：" + e.getMessage());
                }
            }
        }
        
        // 验证结果
        System.out.println("\n=== 验证结果 ===");
        
        ResultSet rs = stmt.executeQuery("SELECT role_code, role_name FROM tb_role WHERE role_code = 'root'");
        if (rs.next()) {
            System.out.println("✓ Root 角色已创建：" + rs.getString("role_code") + " - " + rs.getString("role_name"));
        } else {
            System.out.println("✗ Root 角色创建失败！");
        }
        rs.close();
        
        rs = stmt.executeQuery("SELECT e.name, r.role_code FROM tb_employee e INNER JOIN tb_role r ON e.role_id = r.id WHERE e.name = 'Admin'");
        if (rs.next()) {
            System.out.println("✓ Admin 用户已创建：" + rs.getString("name") + " - 角色：" + rs.getString("role_code"));
        } else {
            System.out.println("✗ Admin 用户创建失败！");
        }
        rs.close();
        
        rs = stmt.executeQuery("SELECT COUNT(*) as count FROM tb_role_menu rm INNER JOIN tb_role r ON rm.role_id = r.id WHERE r.role_code = 'root'");
        if (rs.next()) {
            int count = rs.getInt("count");
            System.out.println("✓ Root 角色已分配 " + count + " 个菜单权限");
        }
        rs.close();
        
        stmt.close();
        conn.close();
        
        System.out.println("\n=== 数据库初始化完成！===");
        System.out.println("登录信息：");
        System.out.println("用户名：admin");
        System.out.println("密码：123456");
        System.out.println("角色：超级管理员（root）");
    }
}
