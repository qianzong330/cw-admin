import java.sql.*;

public class CheckHex {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
        String user = "root";
        String password = "4lsr2tq5";
        
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement stmt = conn.createStatement();
        
        ResultSet rs = stmt.executeQuery("""
            SELECT id, menu_code, menu_name, HEX(menu_name) as hex
            FROM tb_menu 
            WHERE menu_code IN ('approval', 'workhour', 'hr')
            ORDER BY id
        """);
        
        System.out.println("=== Raw HEX Data ===");
        while (rs.next()) {
            int id = rs.getInt("id");
            String code = rs.getString("menu_code");
            String hex = rs.getString("hex");
            System.out.printf("ID:%d | Code:%s | HEX:%s%n", id, code, hex);
        }
        
        rs.close();
        stmt.close();
        conn.close();
    }
}
