import java.sql.*;

public class CheckRoleData {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
        String user = "root";
        String password = "4lsr2tq5";
        
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement stmt = conn.createStatement();
        
        ResultSet rs = stmt.executeQuery("SELECT id, role_code, role_name, remark FROM tb_role ORDER BY id");
        
        System.out.println("=== Role Data in Database ===");
        System.out.println("ID | Role Code | Role Name | Remark");
        System.out.println("---|-----------|-----------|-------");
        
        while (rs.next()) {
            int id = rs.getInt("id");
            String code = rs.getString("role_code");
            String name = rs.getString("role_name");
            String remark = rs.getString("remark");
            
            System.out.printf("%d | %-10s | %-15s | %s%n", id, code, name, remark);
        }
        
        rs.close();
        stmt.close();
        conn.close();
    }
}
