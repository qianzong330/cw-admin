#!/bin/bash

# 测试财务角色权限的脚本

echo "======================================"
echo "测试财务角色菜单权限"
echo "======================================"
echo ""

# 使用 JDBC 查询数据库
java -cp "/home/devbox/project/target/classes:$(find /home/devbox/.m2/repository -name '*.jar' | tr '\n' ':')" \
  org.springframework.jdbc.core.JdbcTemplate << 'EOF'
DataSource dataSource = new org.springframework.jdbc.datasource.DriverManagerDataSource(
    "jdbc:mysql://accounting-mysql.ns-z4bn2xr7.svc:3306/accounting?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai",
    "root",
    "4lsr2tq5"
);
JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

println "=== 财务角色 (finance) 的菜单权限 ==="
def sql = """
SELECT r.role_code, r.role_name, m.menu_code, m.menu_name, m.menu_type
FROM tb_role r 
LEFT JOIN tb_role_menu rm ON r.id = rm.role_id 
LEFT JOIN tb_menu m ON rm.menu_id = m.id 
WHERE r.role_code = 'finance' 
ORDER BY m.sort_order
"""
def results = jdbcTemplate.queryForList(sql)
results.each { row ->
    def type = row.get("menu_type")
    def typeStr = type == 1 ? "[目录]" : (type == 2 ? "[菜单]" : "[按钮]")
    println "${row.get('role_code')} | ${row.get('role_name')} | ${row.get('menu_code')} | ${row.get('menu_name')} | ${typeStr}"
}
println ""
println "总计：${results.size()} 个权限"
EOF

echo ""
echo "======================================"
echo "测试完成"
echo "======================================"
