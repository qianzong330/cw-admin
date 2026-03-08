SET @finance_role_id = (SELECT id FROM tb_role WHERE role_code = 'finance');

INSERT INTO tb_role_menu (role_id, menu_id)
SELECT @finance_role_id, id FROM tb_menu WHERE menu_code = 'approval'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO tb_role_menu (role_id, menu_id)
SELECT @finance_role_id, id FROM tb_menu WHERE menu_code = 'workhour'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO tb_role_menu (role_id, menu_id)
SELECT @finance_role_id, id FROM tb_menu WHERE menu_code = 'hr'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO tb_role_menu (role_id, menu_id)
SELECT @finance_role_id, id FROM tb_menu WHERE menu_code IN (
    'approval:pending',
    'approval:workhour',
    'approval:attendance',
    'approval:salary'
)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO tb_role_menu (role_id, menu_id)
SELECT @finance_role_id, id FROM tb_menu WHERE menu_code = 'workhour:config'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO tb_role_menu (role_id, menu_id)
SELECT @finance_role_id, id FROM tb_menu WHERE menu_code IN (
    'workhour:config:add',
    'workhour:config:edit',
    'workhour:config:delete',
    'workhour:config:approve',
    'workhour:config:invalidate'
)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO tb_role_menu (role_id, menu_id)
SELECT @finance_role_id, id FROM tb_menu WHERE menu_code = 'employee'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO tb_role_menu (role_id, menu_id)
SELECT @finance_role_id, id FROM tb_menu WHERE menu_code = 'category'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO tb_role_menu (role_id, menu_id)
SELECT @finance_role_id, id FROM tb_menu WHERE menu_code = 'project'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
