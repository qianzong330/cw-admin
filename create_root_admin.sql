-- 创建 root 超级管理员角色
INSERT INTO tb_role (role_code, role_name, remark, created_time) 
VALUES ('root', '超级管理员', '系统最高权限角色，拥有所有功能权限', NOW());

-- 获取 root 角色 ID（假设是最后一个插入的）
SET @root_role_id = LAST_INSERT_ID();

-- 创建 admin 用户（BOSS 角色）
INSERT INTO tb_employee (name, role_id, phone, password, status, create_time, update_time)
VALUES ('Admin', @root_role_id, '13800138000', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iB6fKJKvP7PLz5pEhFvGxOqpqIL.', 1, NOW(), NOW());

-- 为 root 角色分配所有菜单权限
-- 获取所有菜单 ID 并插入到 tb_role_menu
INSERT INTO tb_role_menu (role_id, menu_id)
SELECT @root_role_id, id FROM tb_menu;
