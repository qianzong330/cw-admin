INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, url, icon, sort_order, status) VALUES
('approval', '审批管理', 0, 1, NULL, 'bi-check-circle', 1, 1),
('workhour', '工时管理', 0, 1, NULL, 'bi-clock', 4, 1),
('hr', 'HR 管理', 0, 1, NULL, 'bi-people', 5, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, url, icon, sort_order, status) VALUES
('approval:pending', '待审批管理', (SELECT id FROM (SELECT id FROM tb_menu WHERE menu_code='approval') AS tmp), 2, '/approval/pending', 'bi-inbox', 1, 1),
('approval:workhour', '工时配置审批', (SELECT id FROM (SELECT id FROM tb_menu WHERE menu_code='approval') AS tmp), 2, '/workhour/pending', 'bi-clock-history', 2, 1),
('approval:attendance', '考勤审批', (SELECT id FROM (SELECT id FROM tb_menu WHERE menu_code='approval') AS tmp), 2, '/attendance/pending', 'bi-calendar-check', 3, 1),
('approval:salary', '工资条审批', (SELECT id FROM (SELECT id FROM tb_menu WHERE menu_code='approval') AS tmp), 2, '/salary/pending', 'bi-cash-stack', 4, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);
