-- V26: 创建工资条月份状态表，删除 tb_salary 的 status 字段
-- 工资条本身无状态，由月份状态表统一控制

-- 1. 创建工资条月份状态表
CREATE TABLE IF NOT EXISTS tb_salary_month_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    `year_month` VARCHAR(7) NOT NULL COMMENT '年月，格式：yyyy-MM',
    project_id BIGINT COMMENT '项目ID，null表示全部项目',
    status INT DEFAULT 0 COMMENT '状态：0-草稿，1-待审批，5-已审批锁定，12-已驳回',
    submit_by BIGINT COMMENT '提交人ID',
    submit_time DATETIME COMMENT '提交时间',
    submit_remark VARCHAR(500) COMMENT '提交备注',
    approve_by BIGINT COMMENT '审批人ID',
    approve_time DATETIME COMMENT '审批时间',
    approve_remark VARCHAR(500) COMMENT '审批备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_year_month_project (`year_month`, project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资条月份状态管理';

-- 2. 删除 tb_salary 的 status 字段（如果存在）
-- MySQL 8.0+ 支持 DROP COLUMN IF EXISTS
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = DATABASE() 
            AND table_name = 'tb_salary' 
            AND column_name = 'status'
        ),
        'ALTER TABLE tb_salary DROP COLUMN status',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
