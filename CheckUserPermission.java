import java.sql.*;

public class CheckUserPermission {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        Statement stmt = conn.createStatement();
        
        // 1. 查找韩世昌的用户信息
        System.out.println("=== 韩世昌用户信息 ===");
        ResultSet rs = stmt.executeQuery("SELECT id, name, role_id FROM tb_employee WHERE name = '韩世昌'");
        
        if (rs.next()) {
            int empId = rs.getInt("id");
            String name = rs.getString("name");
            Integer roleId = rs.getObject("role_id", Integer.class);
            
            System.out.println("员工 ID: " + empId);
            System.out.println("用户名：" + name);
            System.out.println("角色 ID: " + (roleId != null ? roleId : "null"));
            
            // 2. 查询角色信息
            if (roleId != null) {
                System.out.println("\n=== 角色信息 ===");
                rs = stmt.executeQuery("SELECT id, role_code, role_name FROM tb_role WHERE id = " + roleId);
                if (rs.next()) {
                    System.out.println("角色编码：" + rs.getString("role_code"));
                    System.out.println("角色名称：" + rs.getString("role_name"));
                    
                    // 3. 查询该角色的所有菜单权限
                    System.out.println("\n=== 菜单权限列表 ===");
                    rs = stmt.executeQuery("""
                        SELECT m.menu_code, m.menu_name, m.menu_type
                        FROM tb_menu m
                        INNER JOIN tb_role_menu rm ON m.id = rm.menu_id
                        WHERE rm.role_id = """ + roleId + """
                        ORDER BY m.sort_order
                    """);
                    
                    while (rs.next()) {
                        String type = rs.getInt("menu_type") == 1 ? "[目录]" : 
                                     (rs.getInt("menu_type") == 2 ? "[菜单]" : "[按钮]");
                        System.out.printf("%-30s | %-30s | %s%n", 
                            rs.getString("menu_code"), 
                            rs.getString("menu_name"),
                            type);
                    }
                    
                    // 4. 特别检查是否有 approval 权限
                    System.out.println("\n=== 审批管理相关权限 ===");
                    rs = stmt.executeQuery("""
                        SELECT m.menu_code, m.menu_name
                        FROM tb_menu m
                        INNER JOIN tb_role_menu rm ON m.id = rm.menu_id
                        WHERE rm.role_id = """ + roleId + """
                        AND m.menu_code LIKE '%approval%'
                    """);
                    
                    if (!rs.isBeforeFirst()) {
                        System.out.println("无任何审批管理相关权限 ✓");
                    } else {
                        while (rs.next()) {
                            System.out.println("❌ 仍有权限：" + rs.getString("menu_code") + " - " + rs.getString("menu_name"));
                        }
                    }
                }
            }
        } else {
            System.out.println("未找到用户：韩世昌");
        }
        
        rs.close();
        stmt.close();
        conn.close();
    }
}
