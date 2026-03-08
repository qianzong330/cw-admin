import java.sql.*;

public class TestLogin {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        Statement stmt = conn.createStatement();
        
        System.out.println("=== 测试用户名查询（区分大小写）===");
        
        // 测试小写 admin
        ResultSet rs = stmt.executeQuery("SELECT id, name FROM tb_employee WHERE name = 'admin'");
        System.out.print("查询 'admin': ");
        if (rs.next()) {
            System.out.println("找到用户 ID=" + rs.getInt("id") + ", name=" + rs.getString("name"));
        } else {
            System.out.println("未找到用户");
        }
        rs.close();
        
        // 测试大写 Admin
        rs = stmt.executeQuery("SELECT id, name FROM tb_employee WHERE name = 'Admin'");
        System.out.print("查询 'Admin': ");
        if (rs.next()) {
            System.out.println("找到用户 ID=" + rs.getInt("id") + ", name=" + rs.getString("name"));
        } else {
            System.out.println("未找到用户");
        }
        rs.close();
        
        stmt.close();
        conn.close();
    }
}
