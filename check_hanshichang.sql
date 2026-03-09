-- 检查韩世昌的用户信息和角色
SELECT e.id, e.name, e.phone, e.role_id, r.role_code, r.role_name
FROM tb_employee e
LEFT JOIN tb_role r ON e.role_id = r.id
WHERE e.name = '韩世昌' OR e.phone = '韩世昌';

-- 检查BOSS角色的权限
SELECT r.role_name, m.menu_code, m.menu_name, m.menu_type
FROM tb_role_menu rm
JOIN tb_role r ON rm.role_id = r.id
JOIN tb_menu m ON rm.menu_id = m.id
WHERE r.role_code = 'boss'
ORDER BY m.menu_type, m.menu_code;

-- 检查所有角色
SELECT id, role_code, role_name FROM tb_role;
