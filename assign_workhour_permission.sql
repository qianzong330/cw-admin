-- ============================================
-- 为 boss 和 finance 角色分配工时配置权限
-- 说明：tb_employee 表中有 role_id 字段关联角色
-- ============================================

-- 查看角色表 (如果表不存在，需要先创建)
SHOW TABLES LIKE 'tb_role';

-- 如果 tb_role 表不存在，先创建它
CREATE TABLE IF NOT EXISTS tb_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色 ID',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    remark VARCHAR(200) COMMENT '备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 初始化角色数据 (如果已存在则忽略)
INSERT IGNORE INTO tb_role (role_code, role_name, remark) VALUES 
('root', '超级管理员', '拥有所有权限'),
('boss', 'BOSS', '老板角色'),
('finance', '财务', '财务角色'),
('hr', 'HR', '人力资源角色'),
('employee', '员工', '普通员工');

-- 查看菜单 ID
SELECT id, menu_code, menu_name FROM tb_menu WHERE menu_code LIKE 'workhour%' ORDER BY sort_order;

-- 查找 boss 和 finance 角色的 ID
SELECT id, role_code, role_name FROM tb_role WHERE role_code IN ('boss', 'finance');

-- 为 boss 角色分配工时配置相关权限
INSERT INTO tb_role_menu (role_id, menu_id)
SELECT r.id, m.id 
FROM tb_role r, tb_menu m 
WHERE r.role_code = 'boss' 
  AND m.menu_code IN ('workhour', 'workhour:config', 'workhour:config:add', 'workhour:config:edit', 'workhour:config:delete', 'workhour:config:approve', 'workhour:config:invalidate')
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 为 finance 角色分配工时配置相关权限
INSERT INTO tb_role_menu (role_id, menu_id)
SELECT r.id, m.id 
FROM tb_role r, tb_menu m 
WHERE r.role_code = 'finance' 
  AND m.menu_code IN ('workhour', 'workhour:config', 'workhour:config:add', 'workhour:config:edit', 'workhour:config:delete', 'workhour:config:approve', 'workhour:config:invalidate')
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 验证权限分配结果
SELECT r.role_code, m.menu_code, m.menu_name
FROM tb_role r
JOIN tb_role_menu rm ON r.id = rm.role_id
JOIN tb_menu m ON rm.menu_id = m.id
WHERE r.role_code IN ('boss', 'finance')
  AND m.menu_code LIKE 'workhour%'
ORDER BY r.role_code, m.sort_order;
