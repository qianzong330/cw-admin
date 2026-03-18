-- 创建员工项目流动记录表（记录员工加入和离开项目的完整历史）
CREATE TABLE IF NOT EXISTS tb_employee_project_flow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    employee_name VARCHAR(50) COMMENT '员工姓名（冗余存储）',
    job_category_name VARCHAR(50) COMMENT '工种名称（冗余存储）',
    operation_type TINYINT NOT NULL COMMENT '操作类型：1-加入，2-移除',
    operation_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(50) COMMENT '操作人姓名',
    remark VARCHAR(200) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    INDEX idx_project_id (project_id),
    INDEX idx_employee_id (employee_id),
    INDEX idx_operation_time (operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工项目流动记录表';
