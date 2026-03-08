import java.sql.*;

public class RemoveParentId {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        Statement stmt = conn.createStatement();
        
        System.out.println("=== 删除 tb_category.parent_id 列 ===");
        
        try {
            stmt.executeUpdate("ALTER TABLE tb_category DROP COLUMN parent_id");
            System.out.println("✓ 成功删除 parent_id 列");
        } catch (SQLException e) {
            if (e.getMessage().contains("Unknown column")) {
                System.out.println("ℹ parent_id 列不存在，无需删除");
            } else {
                throw e;
            }
        }
        
        // 验证结果
        System.out.println("\n=== 验证表结构 ===");
        ResultSet rs = stmt.executeQuery("DESCRIBE tb_category");
        System.out.println("\ntb_category 表字段：");
        while (rs.next()) {
            System.out.println("  " + rs.getString("Field") + " - " + rs.getString("Type"));
        }
        
        rs.close();
        stmt.close();
        conn.close();
        
        System.out.println("\n===========================================");
        System.out.println("parent_id 列已删除！");
        System.out.println("===========================================");
    }
}
