import java.sql.*;

public class VerifyApprovalRemoved {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        Statement stmt = conn.createStatement();
        
        System.out.println("=== 验证审批管理菜单删除结果 ===\n");
        
        // 查询是否还有审批相关的菜单
        ResultSet rs = stmt.executeQuery("""
            SELECT menu_code, menu_name, menu_type 
            FROM tb_menu 
            WHERE menu_code LIKE 'approval%' 
            ORDER BY menu_code
        """);
        
        int count = 0;
        System.out.println("剩余的审批相关菜单：");
        while (rs.next()) {
            count++;
            System.out.println("  - " + rs.getString("menu_code") + ": " + rs.getString("menu_name"));
        }
        rs.close();
        
        if (count == 0) {
            System.out.println("  ✓ 无剩余，已全部删除！");
        }
        
        System.out.println("\n=== 当前所有一级菜单 ===");
        rs = stmt.executeQuery("""
            SELECT menu_code, menu_name, sort_order 
            FROM tb_menu 
            WHERE menu_type = 1 AND parent_id = 0 
            ORDER BY sort_order
        """);
        
        while (rs.next()) {
            System.out.println("  " + rs.getInt("sort_order") + ". " + 
                             rs.getString("menu_code") + " - " + 
                             rs.getString("menu_name"));
        }
        rs.close();
        
        stmt.close();
        conn.close();
        
        System.out.println("\n===========================================");
        System.out.println("✓ 审批管理菜单删除验证完成！");
        System.out.println("===========================================");
    }
}
