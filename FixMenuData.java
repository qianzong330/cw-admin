import java.sql.*;
import java.nio.file.*;

public class FixMenuData {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
        String user = "root";
        String password = "4lsr2tq5";
        
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement stmt = conn.createStatement();
        
        // Read SQL file
        String sql = new String(Files.readAllBytes(Paths.get("fix_menu_encoding.sql")));
        String[] statements = sql.split(";");
        
        System.out.println("=== Fixing Menu Data ===");
        int count = 0;
        for (String s : statements) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                try {
                    stmt.execute(trimmed);
                    count++;
                    System.out.println("Updated: " + trimmed.substring(0, Math.min(60, trimmed.length())));
                } catch (Exception e) {
                    System.out.println("Failed: " + e.getMessage());
                }
            }
        }
        
        System.out.println("\nSuccessfully updated " + count + " menus\n");
        
        // Verify
        System.out.println("=== Verification ===");
        ResultSet rs = stmt.executeQuery("""
            SELECT menu_code, menu_name 
            FROM tb_menu 
            WHERE menu_code IN ('approval', 'workhour', 'hr', 'approval:pending', 'workhour:config')
            ORDER BY id
        """);
        
        while (rs.next()) {
            String code = rs.getString("menu_code");
            String name = rs.getString("menu_name");
            System.out.println(code + " = " + name);
        }
        
        rs.close();
        stmt.close();
        conn.close();
    }
}
