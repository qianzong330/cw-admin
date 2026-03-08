import java.sql.*;

public class InsertMissingMenus {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
        String user = "root";
        String password = "4lsr2tq5";
        
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement stmt = conn.createStatement();
        
        System.out.println("Inserting missing menus...\n");
        
        // Insert top-level directories
        String[] directories = {
            "('approval', '审批管理', 0, 1, NULL, 'bi-check-circle', 1, 1)",
            "('workhour', '工时管理', 0, 1, NULL, 'bi-clock', 4, 1)",
            "('hr', 'HR 管理', 0, 1, NULL, 'bi-people', 5, 1)"
        };
        
        for (String dir : directories) {
            try {
                String sql = "INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status) VALUES " + dir + 
                             " ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name)";
                stmt.execute(sql);
                System.out.println("Inserted/Updated: " + dir.split(",")[0].replace("('", ""));
            } catch (Exception e) {
                System.out.println("Failed: " + e.getMessage());
            }
        }
        
        // Insert approval sub-menus
        String[] approvalMenus = {
            "('approval:pending', '待审批管理', (SELECT id FROM (SELECT id FROM tb_menu WHERE menu_code='approval') AS tmp), 2, '/approval/pending', 'bi-inbox', 1, 1)",
            "('approval:workhour', '工时配置审批', (SELECT id FROM (SELECT id FROM tb_menu WHERE menu_code='approval') AS tmp), 2, '/workhour/pending', 'bi-clock-history', 2, 1)",
            "('approval:attendance', '考勤审批', (SELECT id FROM (SELECT id FROM tb_menu WHERE menu_code='approval') AS tmp), 2, '/attendance/pending', 'bi-calendar-check', 3, 1)",
            "('approval:salary', '工资条审批', (SELECT id FROM (SELECT id FROM tb_menu WHERE menu_code='approval') AS tmp), 2, '/salary/pending', 'bi-cash-stack', 4, 1)"
        };
        
        System.out.println("\nInserting approval sub-menus...");
        for (String menu : approvalMenus) {
            try {
                String sql = "INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status) VALUES " + menu + 
                             " ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name)";
                stmt.execute(sql);
                System.out.println("Inserted/Updated: " + menu.split(",")[0].replace("('", ""));
            } catch (Exception e) {
                System.out.println("Failed: " + e.getMessage());
            }
        }
        
        System.out.println("\nDone! Now verifying...");
        
        // Verify
        ResultSet rs = stmt.executeQuery("""
            SELECT id, menu_code, menu_name, menu_type 
            FROM tb_menu 
            WHERE menu_code LIKE 'approval%' OR menu_code IN ('workhour', 'hr')
            ORDER BY menu_code
        """);
        
        System.out.println("\nMenus in database:");
        System.out.println("ID | Menu Code | Menu Name | Type");
        System.out.println("---|-----------|-----------|-----");
        
        while (rs.next()) {
            long id = rs.getLong("id");
            String code = rs.getString("menu_code");
            String name = rs.getString("menu_name");
            int type = rs.getInt("menu_type");
            String typeStr = type == 1 ? "[Directory]" : (type == 2 ? "[Menu]" : "[Button]");
            System.out.printf("%d | %-25s | %-25s | %s%n", id, code, name, typeStr);
        }
        
        rs.close();
        stmt.close();
        conn.close();
    }
}
