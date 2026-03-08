import java.sql.*;

public class RemoveApprovalMenu {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        Statement stmt = conn.createStatement();
        
        System.out.println("=== 删除审批管理菜单 ===");
        
        // 先删除 tb_role_menu 中关联的审批菜单权限
        System.out.println("第 1 步：删除角色权限关联...");
        int deletedPermissions = stmt.executeUpdate("""
            DELETE FROM tb_role_menu 
            WHERE menu_id IN (
                SELECT id FROM (
                    SELECT id FROM tb_menu WHERE menu_code LIKE 'approval%'
                ) AS tmp
            )
        """);
        System.out.println("✓ 已删除 " + deletedPermissions + " 条角色权限关联");
        
        // 删除审批相关的二级菜单和按钮
        System.out.println("\n第 2 步：删除审批子菜单...");
        int deletedSubMenus = stmt.executeUpdate("""
            DELETE FROM tb_menu 
            WHERE menu_code LIKE 'approval:%'
        """);
        System.out.println("✓ 已删除 " + deletedSubMenus + " 个审批子菜单");
        
        // 删除审批管理一级菜单
        System.out.println("\n第 3 步：删除审批管理一级菜单...");
        int deletedMainMenu = stmt.executeUpdate("""
            DELETE FROM tb_menu 
            WHERE menu_code = 'approval'
        """);
        System.out.println("✓ 已删除 " + deletedMainMenu + " 个一级菜单");
        
        // 验证结果
        System.out.println("\n=== 验证删除结果 ===");
        ResultSet rs = stmt.executeQuery("""
            SELECT COUNT(*) as count FROM tb_menu WHERE menu_code LIKE 'approval%'
        """);
        if (rs.next()) {
            int count = rs.getInt("count");
            System.out.println("剩余审批相关菜单数：" + count);
            if (count == 0) {
                System.out.println("✓ 审批管理菜单已全部删除！");
            }
        }
        rs.close();
        
        stmt.close();
        conn.close();
        
        System.out.println("\n===========================================");
        System.out.println("审批管理菜单删除完成！");
        System.out.println("请重启应用查看效果");
        System.out.println("===========================================");
    }
}
