import java.sql.*;

public class CheckRolePermission {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        Statement stmt = conn.createStatement();
        
        // 1. 检查是否有 role 相关的菜单
        System.out.println("=== 检查 role 相关菜单 ===");
        ResultSet rs = stmt.executeQuery("""
            SELECT id, menu_code, menu_name, menu_type, parent_id
            FROM tb_menu 
            WHERE menu_code LIKE '%role%'
            ORDER BY id
        """);
        
        boolean hasRoleMenu = false;
        while (rs.next()) {
            hasRoleMenu = true;
            int id = rs.getInt("id");
            String code = rs.getString("menu_code");
            String name = rs.getString("menu_name");
            int type = rs.getInt("menu_type");
            int parentId = rs.getInt("parent_id");
            
            String typeStr = type == 1 ? "[目录]" : (type == 2 ? "[菜单]" : "[按钮]");
            System.out.printf("ID:%d | Code:%-20s | Name:%-20s | Type:%-8s | Parent:%d%n", 
                id, code, name, typeStr, parentId);
        }
        
        if (!hasRoleMenu) {
            System.out.println("❌ 数据库中没有 role 相关的菜单记录！");
        }
        
        // 2. 检查 BOSS 角色的所有权限
        System.out.println("\n=== BOSS 角色的权限 ===");
        rs = stmt.executeQuery("""
            SELECT m.menu_code, m.menu_name, m.menu_type
            FROM tb_menu m
            INNER JOIN tb_role_menu rm ON m.id = rm.menu_id
            INNER JOIN tb_role r ON rm.role_id = r.id
            WHERE r.role_code = 'boss'
            ORDER BY m.sort_order
        """);
        
        boolean hasRolePermission = false;
        while (rs.next()) {
            String code = rs.getString("menu_code");
            String name = rs.getString("menu_name");
            int type = rs.getInt("menu_type");
            String typeStr = type == 1 ? "[目录]" : (type == 2 ? "[菜单]" : "[按钮]");
            
            if (code.contains("role")) {
                hasRolePermission = true;
                System.out.printf("✅ %-30s | %-30s | %s%n", code, name, typeStr);
            }
        }
        
        if (!hasRolePermission) {
            System.out.println("❌ BOSS 角色没有 role 相关的权限！");
        }
        
        // 3. 查看所有角色是否有任何一个有 role 权限
        System.out.println("\n=== 所有角色的 role 权限 ===");
        rs = stmt.executeQuery("""
            SELECT r.role_code, r.role_name, m.menu_code, m.menu_name
            FROM tb_role r
            INNER JOIN tb_role_menu rm ON r.id = rm.role_id
            INNER JOIN tb_menu m ON rm.menu_id = m.id
            WHERE m.menu_code LIKE '%role%'
            ORDER BY r.role_code
        """);
        
        if (!rs.isBeforeFirst()) {
            System.out.println("❌ 没有任何角色拥有 role 权限！");
        } else {
            while (rs.next()) {
                System.out.printf("角色：%s (%s) - 菜单：%s (%s)%n", 
                    rs.getString("role_code"), 
                    rs.getString("role_name"),
                    rs.getString("menu_code"),
                    rs.getString("menu_name"));
            }
        }
        
        rs.close();
        stmt.close();
        conn.close();
    }
}
