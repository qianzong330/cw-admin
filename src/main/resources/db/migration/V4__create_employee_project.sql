-- 员工项目关联表
CREATE TABLE IF NOT EXISTS tb_employee_project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_employee_project (employee_id, project_id),
    INDEX idx_employee (employee_id),
    INDEX idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工项目关联表';
