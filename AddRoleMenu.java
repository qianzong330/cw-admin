import java.sql.*;

public class AddRoleMenu {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        Statement stmt = conn.createStatement();
        
        System.out.println("=== 添加角色管理菜单 ===");
        
        // 添加角色管理菜单
        String sql = """
            INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, url, icon, sort_order, status) VALUES
            ('role', '角色管理', (SELECT id FROM (SELECT id FROM tb_menu WHERE menu_code='system') AS tmp), 2, '/role/list', 'bi-shield-lock', 1, 1)
            ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name)
            """;
        
        stmt.executeUpdate(sql);
        System.out.println("✓ 已添加角色管理菜单");
        
        // 为 root 角色添加权限
        String grantSql = """
            INSERT INTO tb_role_menu (role_id, menu_id)
            SELECT r.id, m.id FROM tb_role r, tb_menu m 
            WHERE r.role_code = 'root' AND m.menu_code = 'role'
            ON DUPLICATE KEY UPDATE role_id = VALUES(role_id)
            """;
        
        stmt.executeUpdate(grantSql);
        System.out.println("✓ 已为 root 角色分配角色管理权限");
        
        // 验证结果
        ResultSet rs = stmt.executeQuery("""
            SELECT m.menu_code, m.menu_name, m.url, m.icon
            FROM tb_menu m
            WHERE m.menu_code = 'role'
        """);
        
        if (rs.next()) {
            System.out.println("\n菜单信息：");
            System.out.println("  编码：" + rs.getString("menu_code"));
            System.out.println("  名称：" + rs.getString("menu_name"));
            System.out.println("  路径：" + rs.getString("url"));
            System.out.println("  图标：" + rs.getString("icon"));
        }
        
        rs.close();
        stmt.close();
        conn.close();
        
        System.out.println("\n===========================================");
        System.out.println("角色管理菜单已添加！");
        System.out.println("请重启应用后查看效果");
        System.out.println("===========================================");
    }
}
