-- 创建项目管理员关联表（支持多选管理员）
CREATE TABLE IF NOT EXISTS tb_project_admin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    employee_id BIGINT NOT NULL COMMENT '管理员员工ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_employee (project_id, employee_id),
    FOREIGN KEY (project_id) REFERENCES tb_project(id) ON DELETE CASCADE,
    FOREIGN KEY (employee_id) REFERENCES tb_employee(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目管理员关联表';
