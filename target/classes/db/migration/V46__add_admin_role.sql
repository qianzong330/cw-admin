-- 创建admin角色（项目管理员角色）
INSERT INTO tb_role (role_code, role_name, remark)
VALUES ('admin', '项目管理员', '项目管理员角色')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);
