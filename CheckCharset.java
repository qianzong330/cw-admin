import java.sql.*;

public class CheckCharset {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
        String user = "root";
        String password = "4lsr2tq5";
        
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement stmt = conn.createStatement();
        
        // Check database charset
        ResultSet rs = stmt.executeQuery("SHOW CREATE DATABASE accounting");
        System.out.println("=== Database Charset ===");
        if (rs.next()) {
            System.out.println(rs.getString(2));
        }
        
        // Check table charset
        System.out.println("\n=== Table Charset ===");
        rs = stmt.executeQuery("SHOW TABLE STATUS LIKE 'tb_role'");
        if (rs.next()) {
            System.out.println("Table Collation: " + rs.getString("Collation"));
        }
        
        // Check actual data with hex
        System.out.println("\n=== Raw Data (Hex) ===");
        rs = stmt.executeQuery("SELECT id, role_code, HEX(role_name) as name_hex FROM tb_role ORDER BY id");
        while (rs.next()) {
            int id = rs.getInt("id");
            String code = rs.getString("role_code");
            String nameHex = rs.getString("name_hex");
            System.out.printf("ID: %d, Code: %s, Name(Hex): %s%n", id, code, nameHex);
        }
        
        rs.close();
        stmt.close();
        conn.close();
    }
}
