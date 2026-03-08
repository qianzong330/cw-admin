import java.sql.*;

public class CheckRoleTable {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        
        DatabaseMetaData meta = conn.getMetaData();
        ResultSet rs = meta.getColumns(null, null, "tb_role", null);
        
        System.out.println("=== tb_role 表结构 ===");
        while (rs.next()) {
            String columnName = rs.getString("COLUMN_NAME");
            String columnType = rs.getString("TYPE_NAME");
            int size = rs.getInt("COLUMN_SIZE");
            String isNullable = rs.getString("IS_NULLABLE");
            
            System.out.printf("%-20s | %-15s | Size: %-3d | %s%n", 
                columnName, columnType, size, isNullable);
        }
        
        rs.close();
        conn.close();
    }
}
