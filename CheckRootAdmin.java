import java.sql.*;

public class CheckRootAdmin {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        Statement stmt = conn.createStatement();
        
        System.out.println("=== 检查 root 角色 ===");
        ResultSet rs = stmt.executeQuery("SELECT id, role_code, role_name FROM tb_role WHERE role_code = 'root'");
        if (rs.next()) {
            int roleId = rs.getInt("id");
            System.out.println("Root 角色 ID: " + roleId);
            System.out.println("角色编码：" + rs.getString("role_code"));
            System.out.println("角色名称：" + rs.getString("role_name"));
            
            // 检查菜单权限
            System.out.println("\n=== Root 角色的菜单权限 ===");
            rs = stmt.executeQuery("""
                SELECT COUNT(*) as count FROM tb_role_menu WHERE role_id = """ + roleId);
            if (rs.next()) {
                System.out.println("菜单权限数量：" + rs.getInt("count"));
            }
            
            // 检查是否有员工使用这个角色
            System.out.println("\n=== 使用该角色的员工 ===");
            rs = stmt.executeQuery("SELECT id, name FROM tb_employee WHERE role_id = " + roleId);
            boolean hasEmployee = false;
            while (rs.next()) {
                hasEmployee = true;
                System.out.println("员工 ID: " + rs.getInt("id") + ", 姓名：" + rs.getString("name"));
            }
            if (!hasEmployee) {
                System.out.println("暂无员工使用该角色");
            }
        } else {
            System.out.println("未找到 root 角色！");
        }
        
        System.out.println("\n=== 检查所有菜单 ===");
        rs = stmt.executeQuery("SELECT COUNT(*) as count FROM tb_menu");
        if (rs.next()) {
            System.out.println("系统菜单总数：" + rs.getInt("count"));
        }
        
        rs.close();
        stmt.close();
        conn.close();
    }
}
