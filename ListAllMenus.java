import java.sql.*;

public class ListAllMenus {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        Statement stmt = conn.createStatement();
        
        ResultSet rs = stmt.executeQuery("""
            SELECT id, menu_code, menu_name, menu_type, parent_id
            FROM tb_menu 
            ORDER BY id
        """);
        
        System.out.println("=== 所有菜单权限 ===");
        while (rs.next()) {
            int id = rs.getInt("id");
            String code = rs.getString("menu_code");
            String name = rs.getString("menu_name");
            int type = rs.getInt("menu_type");
            int parentId = rs.getInt("parent_id");
            
            String typeStr = type == 1 ? "[目录]" : (type == 2 ? "[菜单]" : "[按钮]");
            
            // 高亮显示角色相关
            String highlight = (code.contains("role") || name.contains("角色")) ? " <<<< 角色管理" : "";
            
            System.out.printf("ID:%3d | Code:%-30s | Name:%-30s | Type:%-8s | Parent:%d%s%n", 
                id, code, name, typeStr, parentId, highlight);
        }
        
        rs.close();
        stmt.close();
        conn.close();
    }
}
