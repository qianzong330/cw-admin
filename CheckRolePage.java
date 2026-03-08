import java.sql.*;

public class CheckRolePage {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
        String user = "root";
        String password = "4lsr2tq5";
        
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement stmt = conn.createStatement();
        
        // Get role info
        System.out.println("=== Role ID 3 Info ===");
        ResultSet rs = stmt.executeQuery("SELECT id, role_code, role_name FROM tb_role WHERE id = 3");
        if (rs.next()) {
            int id = rs.getInt("id");
            String code = rs.getString("role_code");
            String name = rs.getString("role_name");
            System.out.printf("ID: %d, Code: %s, Name: %s%n", id, code, name);
        }
        
        // Get menus for finance role
        System.out.println("\n=== Menus assigned to Finance role ===");
        String sql = """
            SELECT m.menu_code, m.menu_name, m.menu_type
            FROM tb_menu m
            INNER JOIN tb_role_menu rm ON m.id = rm.menu_id
            WHERE rm.role_id = 3
            ORDER BY m.sort_order
        """;
        rs = stmt.executeQuery(sql);
        while (rs.next()) {
            String code = rs.getString("menu_code");
            String name = rs.getString("menu_name");
            int type = rs.getInt("menu_type");
            String typeStr = type == 1 ? "[Directory]" : (type == 2 ? "[Menu]" : "[Button]");
            System.out.printf("%-25s | %-25s | %s%n", code, name, typeStr);
        }
        
        rs.close();
        stmt.close();
        conn.close();
    }
}
