import java.sql.*;

public class CheckMenuData {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        Statement stmt = conn.createStatement();
        
        System.out.println("=== 当前所有菜单数据 ===");
        ResultSet rs = stmt.executeQuery("""
            SELECT id, menu_code, menu_name, menu_type, parent_id, url, icon, sort_order
            FROM tb_menu ORDER BY sort_order, parent_id, id
        """);
        
        System.out.printf("%-4s %-30s %-20s %-6s %-8s %-30s%n", "ID", "menu_code", "menu_name", "type", "parent", "url");
        System.out.println("-".repeat(100));
        while (rs.next()) {
            System.out.printf("%-4d %-30s %-20s %-6d %-8s %-30s%n",
                rs.getLong("id"),
                rs.getString("menu_code"),
                rs.getString("menu_name"),
                rs.getInt("menu_type"),
                rs.getString("parent_id"),
                rs.getString("url") != null ? rs.getString("url") : "");
        }
        rs.close();
        stmt.close();
        conn.close();
    }
}
