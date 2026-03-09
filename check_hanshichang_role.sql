-- 检查韩世昌的用户信息和角色
SELECT e.id, e.name, e.phone, e.role_id, r.role_code, r.role_name
FROM tb_employee e
LEFT JOIN tb_role r ON e.role_id = r.id
WHERE e.name = '韩世昌';

-- 检查所有员工的角色分配
SELECT e.id, e.name, e.role_id, r.role_code, r.role_name
FROM tb_employee e
LEFT JOIN tb_role r ON e.role_id = r.id
ORDER BY e.id;
