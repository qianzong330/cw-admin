import java.sql.*;
import java.nio.file.*;

public class ExecuteSql {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
        String user = "root";
        String password = "4lsr2tq5";
        
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement stmt = conn.createStatement();
        
        // Read V9 migration script
        String sql = new String(Files.readAllBytes(Paths.get("src/main/resources/db/migration/V9__add_finance_role_permissions.sql")));
        
        System.out.println("Executing V9 migration script...\n");
        
        // Split by semicolon and execute each statement
        String[] statements = sql.split(";");
        int successCount = 0;
        int skipCount = 0;
        
        for (String statement : statements) {
            String trimmed = statement.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                skipCount++;
                continue;
            }
            // Skip standalone SELECT queries (verification queries)
            if (trimmed.startsWith("SELECT") && !trimmed.contains("INSERT") && !trimmed.contains("UPDATE") && !trimmed.contains("SET")) {
                skipCount++;
                continue;
            }
            
            try {
                stmt.addBatch(trimmed);
                successCount++;
            } catch (Exception e) {
                System.out.println("Error adding statement: " + e.getMessage());
            }
        }
        
        System.out.println("Added " + successCount + " statements to batch (skipped " + skipCount + ")");
        System.out.println("Executing batch...");
        
        // Execute the batch
        try {
            int[] results = stmt.executeBatch();
            System.out.println("Batch executed successfully! " + results.length + " statements executed.");
        } catch (Exception e) {
            System.out.println("Error executing batch: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Verify finance role permissions
        System.out.println("\nVerifying finance role permissions:");
        String verifySql = """
            SELECT r.role_code, r.role_name, m.menu_code, m.menu_name, m.menu_type
            FROM tb_role r 
            LEFT JOIN tb_role_menu rm ON r.id = rm.role_id 
            LEFT JOIN tb_menu m ON rm.menu_id = m.id 
            WHERE r.role_code = 'finance' 
            ORDER BY m.sort_order
        """;
        
        ResultSet rs = stmt.executeQuery(verifySql);
        System.out.println("\nRole Code | Role Name | Menu Code | Menu Name | Type");
        System.out.println("----------|-----------|--------------------------|--------------------|--------");
        
        int count = 0;
        while (rs.next()) {
            String roleCode = rs.getString("role_code");
            String roleName = rs.getString("role_name");
            String menuCode = rs.getString("menu_code");
            String menuName = rs.getString("menu_name");
            int menuType = rs.getInt("menu_type");
            
            String typeStr = menuType == 1 ? "[Directory]" : (menuType == 2 ? "[Menu]" : "[Button]");
            System.out.printf("%-10s| %-11s| %-26s| %-20s| %s%n", 
                roleCode, roleName, menuCode, menuName, typeStr);
            count++;
        }
        
        System.out.println("\nTotal: " + count + " permissions");
        
        rs.close();
        stmt.close();
        conn.close();
    }
}
