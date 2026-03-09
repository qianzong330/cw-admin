-- 检查所有角色
SELECT * FROM tb_role ORDER BY id;

-- 检查BOSS角色是否存在
SELECT * FROM tb_role WHERE role_code = 'boss';

-- 检查所有角色的菜单权限数量
SELECT r.id, r.role_code, r.role_name, COUNT(rm.id) as menu_count
FROM tb_role r
LEFT JOIN tb_role_menu rm ON r.id = rm.role_id
GROUP BY r.id
ORDER BY r.id;
