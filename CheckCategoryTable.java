import java.sql.*;

public class CheckCategoryTable {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        
        System.out.println("=== 检查 tb_category 表结构 ===");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("DESCRIBE tb_category");
        
        System.out.println("\ntb_category 表字段：");
        while (rs.next()) {
            System.out.println("  " + rs.getString("Field") + " - " + rs.getString("Type"));
        }
        
        rs.close();
        stmt.close();
        conn.close();
    }
}
