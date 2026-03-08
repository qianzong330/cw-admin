import java.sql.*;

public class CheckMenuEncoding {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
        String user = "root";
        String password = "4lsr2tq5";
        
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement stmt = conn.createStatement();
        
        // Get all menus with hex representation
        System.out.println("=== All Menus (with HEX encoding) ===");
        ResultSet rs = stmt.executeQuery("""
            SELECT id, menu_code, menu_name, HEX(menu_name) as name_hex, menu_type
            FROM tb_menu 
            ORDER BY sort_order
        """);
        
        while (rs.next()) {
            int id = rs.getInt("id");
            String code = rs.getString("menu_code");
            String nameHex = rs.getString("name_hex");
            int type = rs.getInt("menu_type");
            
            // Convert HEX to readable Chinese
            String nameChinese = hexToString(nameHex);
            String typeStr = type == 1 ? "[Dir]" : (type == 2 ? "[Menu]" : "[Btn]");
            
            System.out.printf("ID:%d | %-25s | %-25s | %s%n", 
                id, code, nameChinese, typeStr);
        }
        
        rs.close();
        stmt.close();
        conn.close();
    }
    
    // Helper method to convert HEX string to Chinese
    private static String hexToString(String hex) {
        if (hex == null || hex.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            String hexPart = hex.substring(i, i + 2);
            int intValue = Integer.parseInt(hexPart, 16);
            sb.append((char) intValue);
        }
        return sb.toString();
    }
}
