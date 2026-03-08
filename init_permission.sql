-- ============================================
-- 菜单权限系统初始化脚本
-- 执行方式：直接在 MySQL 客户端或管理工具中执行
-- ============================================

-- 1. 创建角色表
CREATE TABLE IF NOT EXISTS tb_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色 ID',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    remark VARCHAR(200) COMMENT '备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 2. 创建菜单表
CREATE TABLE IF NOT EXISTS tb_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '菜单 ID',
    menu_code VARCHAR(100) NOT NULL UNIQUE COMMENT '菜单编码',
    menu_name VARCHAR(100) NOT NULL COMMENT '菜单名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单 ID',
    menu_type TINYINT DEFAULT 1 COMMENT '菜单类型：1-目录，2-菜单，3-按钮',
    menu_url VARCHAR(200) COMMENT '菜单路径',
    menu_icon VARCHAR(50) COMMENT '菜单图标',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    remark VARCHAR(200) COMMENT '备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_parent (parent_id),
    INDEX idx_type (menu_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 3. 创建角色菜单关联表
CREATE TABLE IF NOT EXISTS tb_role_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID',
    role_id BIGINT NOT NULL COMMENT '角色 ID',
    menu_id BIGINT NOT NULL COMMENT '菜单 ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_menu (role_id, menu_id),
    INDEX idx_role (role_id),
    INDEX idx_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 4. 初始化角色数据
INSERT INTO tb_role (role_code, role_name, remark) VALUES 
('root', '超级管理员', '拥有所有权限'),
('boss', 'BOSS', '老板角色'),
('finance', '财务', '财务角色'),
('hr', 'HR', '人力资源角色'),
('employee', '员工', '普通员工')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- 5. 初始化一级菜单（目录）
INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status) VALUES
('system', '系统管理', 0, 1, NULL, 'bi-gear', 100, 1),
('approval', '审批管理', 0, 1, NULL, 'bi-check-circle', 1, 1),
('attendance', '考勤管理', 0, 1, NULL, 'bi-calendar-check', 2, 1),
('salary', '工资管理', 0, 1, NULL, 'bi-cash', 3, 1),
('workhour', '工时管理', 0, 1, NULL, 'bi-clock', 4, 1),
('hr', 'HR 管理', 0, 1, NULL, 'bi-people', 5, 1),
('account', '账户管理', 0, 1, NULL, 'bi-wallet', 6, 1),
('project', '项目管理', 0, 1, NULL, 'bi-folder', 7, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- 6. 初始化审批管理二级菜单
INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status)
SELECT 'approval:pending', '待审批管理', m.id, 2, '/approval/pending', 'bi-inbox', 1, 1
FROM tb_menu m WHERE m.menu_code = 'approval' AND m.parent_id = 0
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status)
SELECT 'approval:workhour', '工时配置审批', m.id, 2, '/workhour/pending', 'bi-clock-history', 2, 1
FROM tb_menu m WHERE m.menu_code = 'approval' AND m.parent_id = 0
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status)
SELECT 'approval:attendance', '考勤审批', m.id, 2, '/attendance/pending', 'bi-calendar-check', 3, 1
FROM tb_menu m WHERE m.menu_code = 'approval' AND m.parent_id = 0
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status)
SELECT 'approval:salary', '工资条审批', m.id, 2, '/salary/pending', 'bi-cash-stack', 4, 1
FROM tb_menu m WHERE m.menu_code = 'approval' AND m.parent_id = 0
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- 7. 初始化工时管理菜单
INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status)
SELECT 'workhour:config', '工时配置', m.id, 2, '/workhour/config', 'bi-sliders', 1, 1
FROM tb_menu m WHERE m.menu_code = 'workhour' AND m.parent_id = 0
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- 8. 初始化小时配置按钮权限
INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status)
SELECT 'workhour:config:add', '新增配置', m.id, 3, NULL, 'bi-plus', 1, 1
FROM tb_menu m WHERE m.menu_code = 'workhour:config'
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status)
SELECT 'workhour:config:edit', '编辑配置', m.id, 3, NULL, 'bi-pencil', 2, 1
FROM tb_menu m WHERE m.menu_code = 'workhour:config'
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status)
SELECT 'workhour:config:delete', '删除配置', m.id, 3, NULL, 'bi-trash', 3, 1
FROM tb_menu m WHERE m.menu_code = 'workhour:config'
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status)
SELECT 'workhour:config:approve', '审批配置', m.id, 3, NULL, 'bi-check-lg', 4, 1
FROM tb_menu m WHERE m.menu_code = 'workhour:config'
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status)
SELECT 'workhour:config:invalidate', '作废配置', m.id, 3, NULL, 'bi-slash-circle', 5, 1
FROM tb_menu m WHERE m.menu_code = 'workhour:config'
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- 9. 初始化 root 角色的菜单权限（拥有所有权限）
INSERT INTO tb_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM tb_role r, tb_menu m 
WHERE r.role_code = 'root'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- ============================================
-- 验证查询
-- ============================================

-- 查看所有角色
SELECT * FROM tb_role;

-- 查看所有菜单
SELECT * FROM tb_menu ORDER BY sort_order;

-- 查看 root 角色的菜单权限
SELECT r.role_code, m.menu_code, m.menu_name, m.menu_type
FROM tb_role r 
JOIN tb_role_menu rm ON r.id = rm.role_id 
JOIN tb_menu m ON rm.menu_id = m.id 
WHERE r.role_code = 'root'
ORDER BY m.sort_order;
