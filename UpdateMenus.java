import java.sql.*;
public class UpdateMenus {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8";
        Connection conn = DriverManager.getConnection(url, "root", "4lsr2tq5");
        Statement stmt = conn.createStatement();
        
        String[][] updates = {
            {"approval", "\u5BA1\u6279\u7BA1\u7406"},
            {"workhour", "\u5DE5\u65F6\u7BA1\u7406"},
            {"hr", "HR\u7BA1\u7406"},
            {"approval:pending", "\u5F85\u5BA1\u6279\u7BA1\u7406"},
            {"approval:workhour", "\u5DE5\u65F6\u914D\u7F6E\u5BA1\u6279"},
            {"approval:attendance", "\u8003\u52E4\u5BA1\u6279"},
            {"approval:salary", "\u5DE5\u8D44\u6761\u5BA1\u6279"},
            {"workhour:config", "\u5DE5\u65F6\u914D\u7F6E"},
            {"workhour:config:list", "\u5DE5\u65F6\u914D\u7F6E\u5217\u8868"},
            {"workhour:config:add", "\u65B0\u589E\u914D\u7F6E"},
            {"workhour:config:edit", "\u7F16\u8F91\u914D\u7F6E"},
            {"workhour:config:delete", "\u5220\u9664\u914D\u7F6E"},
            {"workhour:config:approve", "\u5BA1\u6279\u914D\u7F6E"},
            {"workhour:config:invalidate", "\u4F5C\u5E9F\u914D\u7F6E"}
        };
        
        for (String[] update : updates) {
            String sql = "UPDATE tb_menu SET menu_name = '" + update[1] + "' WHERE menu_code = '" + update[0] + "'";
            stmt.execute(sql);
            System.out.println("Updated: " + update[0]);
        }
        
        ResultSet rs = stmt.executeQuery("SELECT menu_code, menu_name FROM tb_menu WHERE menu_code IN ('approval', 'workhour', 'hr') ORDER BY id");
        System.out.println("\n=== Result ===");
        while (rs.next()) {
            System.out.println(rs.getString("menu_code") + " = " + rs.getString("menu_name"));
        }
        
        rs.close();
        stmt.close();
        conn.close();
    }
}
