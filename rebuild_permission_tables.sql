-- ============================================
-- 重建权限系统表结构
-- ============================================

-- 删除旧表 (如果存在)
DROP TABLE IF EXISTS tb_role_menu;
DROP TABLE IF EXISTS tb_menu;
DROP TABLE IF EXISTS tb_role;

-- 创建角色表
CREATE TABLE tb_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色 ID',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    remark VARCHAR(200) COMMENT '备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 初始化角色数据
INSERT INTO tb_role (role_code, role_name, remark) VALUES 
('root', '超级管理员', '拥有所有权限'),
('boss', 'BOSS', '老板角色'),
('finance', '财务', '财务角色'),
('hr', 'HR', '人力资源角色'),
('employee', '员工', '普通员工');

-- 查看结果
SELECT * FROM tb_role;
DESC tb_role;
