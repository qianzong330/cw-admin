import java.sql.*;

public class CreateRootAdminUser {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        Statement stmt = conn.createStatement();
        
        System.out.println("=== 为 root 角色分配所有菜单权限 ===");
        
        // 获取 root 角色 ID
        ResultSet rs = stmt.executeQuery("SELECT id FROM tb_role WHERE role_code = 'root'");
        int rootRoleId = 0;
        if (rs.next()) {
            rootRoleId = rs.getInt("id");
            System.out.println("Root 角色 ID: " + rootRoleId);
        } else {
            System.out.println("未找到 root 角色！");
            rs.close();
            stmt.close();
            conn.close();
            return;
        }
        rs.close();
        
        // 获取所有菜单 ID
        rs = stmt.executeQuery("SELECT id, menu_code, menu_name FROM tb_menu ORDER BY id");
        java.util.List<String> menus = new java.util.ArrayList<>();
        while (rs.next()) {
            int menuId = rs.getInt("id");
            String menuCode = rs.getString("menu_code");
            String menuName = rs.getString("menu_name");
            menus.add(menuId + "," + menuCode + "," + menuName);
        }
        rs.close();
        
        int count = 0;
        for (String menu : menus) {
            String[] parts = menu.split(",");
            int menuId = Integer.parseInt(parts[0]);
            String menuCode = parts[1];
            String menuName = parts[2];
            
            // 插入角色菜单关联
            String sql = "INSERT INTO tb_role_menu (role_id, menu_id) VALUES (" + rootRoleId + ", " + menuId + ")";
            try {
                stmt.executeUpdate(sql);
                count++;
            } catch (SQLException e) {
                if (!e.getMessage().contains("Duplicate")) {
                    System.out.println("失败：" + menuCode + " - " + e.getMessage());
                }
            }
        }
        
        System.out.println("已分配 " + count + " 个菜单权限");
        
        System.out.println("=== 创建 admin 用户 ===");
        
        // 删除可能存在的旧 admin 用户
        try {
            stmt.executeUpdate("DELETE FROM tb_employee WHERE name = 'Admin'");
            System.out.println("✓ 已删除旧的 Admin 用户");
        } catch (SQLException e) {
            System.out.println("ℹ 无旧的 Admin 用户");
        }
        
        // 创建 admin 用户（密码是 BCrypt 加密的 123456）
        String passwordHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iB6fKJKvP7PLz5pEhFvGxOqpqIL.";
        String insertSql = """
            INSERT INTO tb_employee (name, role_id, phone, password, status, create_time, update_time)
            VALUES ('Admin', %d, '13800138000', '%s', 1, NOW(), NOW())
            """.formatted(rootRoleId, passwordHash);
        
        try {
            stmt.executeUpdate(insertSql);
            System.out.println("✓ Admin 用户创建成功！");
        } catch (SQLException e) {
            System.out.println("✗ Admin 用户创建失败：" + e.getMessage());
        }
        
        // 验证
        System.out.println("\n=== 验证结果 ===");
        rs = stmt.executeQuery("""
            SELECT e.id, e.name, e.phone, r.role_code, r.role_name
            FROM tb_employee e
            INNER JOIN tb_role r ON e.role_id = r.id
            WHERE e.name = 'Admin'
        """);
        
        if (rs.next()) {
            System.out.println("✓ 用户 ID: " + rs.getInt("id"));
            System.out.println("✓ 用户名：" + rs.getString("name"));
            System.out.println("✓ 手机号：" + rs.getString("phone"));
            System.out.println("✓ 角色编码：" + rs.getString("role_code"));
            System.out.println("✓ 角色名称：" + rs.getString("role_name"));
        } else {
            System.out.println("✗ 未找到 Admin 用户！");
        }
        
        rs = stmt.executeQuery("SELECT COUNT(*) as count FROM tb_role_menu WHERE role_id = " + rootRoleId);
        if (rs.next()) {
            System.out.println("✓ Root 角色菜单权限数：" + rs.getInt("count"));
        }
        
        rs.close();
        stmt.close();
        conn.close();
        
        System.out.println("\n===========================================");
        System.out.println("数据库初始化完成！");
        System.out.println("登录信息：");
        System.out.println("  用户名：admin");
        System.out.println("  密  码：123456");
        System.out.println("  角  色：超级管理员（root）");
        System.out.println("  权  限：所有菜单权限（" + count + "个）");
        System.out.println("===========================================");
    }
}
