import java.sql.*;

public class CheckMenus {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
        String user = "root";
        String password = "4lsr2tq5";
        
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement stmt = conn.createStatement();
        
        // Check if approval menus exist
        String checkSql = """
            SELECT id, menu_code, menu_name, menu_type 
            FROM tb_menu 
            WHERE menu_code LIKE 'approval%' OR menu_code LIKE 'workhour%' OR menu_code IN ('employee', 'category', 'project', 'hr')
            ORDER BY menu_code
        """;
        
        ResultSet rs = stmt.executeQuery(checkSql);
        System.out.println("Menus in database:");
        System.out.println("ID | Menu Code | Menu Name | Type");
        System.out.println("---|-----------|-----------|-----");
        
        while (rs.next()) {
            long id = rs.getLong("id");
            String code = rs.getString("menu_code");
            String name = rs.getString("menu_name");
            int type = rs.getInt("menu_type");
            String typeStr = type == 1 ? "[Directory]" : (type == 2 ? "[Menu]" : "[Button]");
            System.out.printf("%d | %-20s | %-25s | %s%n", id, code, name, typeStr);
        }
        
        rs.close();
        stmt.close();
        conn.close();
    }
}
