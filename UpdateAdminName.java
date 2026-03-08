import java.sql.*;

public class UpdateAdminName {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        
        System.out.println("=== 更新用户名为 admin ===");
        
        // 将 Admin 改为 admin（统一小写）
        String updateSql = "UPDATE tb_employee SET name = 'admin' WHERE id = 2";
        PreparedStatement pstmt = conn.prepareStatement(updateSql);
        int rows = pstmt.executeUpdate();
        
        System.out.println("✓ 已更新 " + rows + " 条记录");
        
        // 验证更新结果
        ResultSet rs = pstmt.executeQuery("SELECT id, name FROM tb_employee WHERE id = 2");
        if (rs.next()) {
            System.out.println("✓ 用户 ID: " + rs.getInt("id"));
            System.out.println("✓ 用户名：" + rs.getString("name"));
        }
        
        rs.close();
        pstmt.close();
        conn.close();
        
        System.out.println("\n===========================================");
        System.out.println("现在可以使用 admin/123456 登录了！");
        System.out.println("===========================================");
    }
}
