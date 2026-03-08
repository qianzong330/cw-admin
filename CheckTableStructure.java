import java.sql.*;

public class CheckTableStructure {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
        String user = "root";
        String password = "4lsr2tq5";
        
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement stmt = conn.createStatement();
        
        ResultSet rs = stmt.executeQuery("DESCRIBE tb_menu");
        
        System.out.println("tb_menu table structure:");
        System.out.println("Field | Type | Null | Key | Default | Extra");
        System.out.println("------|------|------|-----|---------|-----");
        
        while (rs.next()) {
            String field = rs.getString("Field");
            String type = rs.getString("Type");
            String nullVal = rs.getString("Null");
            String key = rs.getString("Key");
            String def = rs.getString("Default");
            String extra = rs.getString("Extra");
            System.out.printf("%-20s | %-15s | %-4s | %-3s | %-7s | %s%n", field, type, nullVal, key, def, extra);
        }
        
        rs.close();
        stmt.close();
        conn.close();
    }
}
