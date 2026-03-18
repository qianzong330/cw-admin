-- 创建员工项目关联历史表（记录已移除的员工）
CREATE TABLE IF NOT EXISTS tb_employee_project_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    employee_name VARCHAR(50) COMMENT '员工姓名（冗余存储，防止员工被删除后无法显示）',
    job_category_name VARCHAR(50) COMMENT '工种名称（冗余存储）',
    join_time DATETIME COMMENT '加入项目时间',
    leave_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '离开项目时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    INDEX idx_project_id (project_id),
    INDEX idx_employee_id (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工项目关联历史表（记录已移除的员工）';
