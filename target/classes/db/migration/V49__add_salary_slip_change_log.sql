-- 工资条修改记录表
-- 用于记录工资条二次修改的变更内容，供审批人查看
CREATE TABLE IF NOT EXISTS tb_salary_slip_change_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    salary_slip_id BIGINT NOT NULL COMMENT '工资条ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    salary_period VARCHAR(7) NOT NULL COMMENT '工资月份(yyyy-MM)',
    
    -- 变更前数据
    old_attendance_days DECIMAL(10,2) DEFAULT NULL COMMENT '变更前出勤天数',
    old_base_salary DECIMAL(10,2) DEFAULT NULL COMMENT '变更前基础薪资',
    old_base_amount DECIMAL(10,2) DEFAULT NULL COMMENT '变更前汇总工资',
    old_addition_amount DECIMAL(10,2) DEFAULT NULL COMMENT '变更前费用加项',
    old_deduction_amount DECIMAL(10,2) DEFAULT NULL COMMENT '变更前费用减项',
    old_payable_amount DECIMAL(10,2) DEFAULT NULL COMMENT '变更前应付工资',
    
    -- 变更后数据
    new_attendance_days DECIMAL(10,2) DEFAULT NULL COMMENT '变更后出勤天数',
    new_base_salary DECIMAL(10,2) DEFAULT NULL COMMENT '变更后基础薪资',
    new_base_amount DECIMAL(10,2) DEFAULT NULL COMMENT '变更后汇总工资',
    new_addition_amount DECIMAL(10,2) DEFAULT NULL COMMENT '变更后费用加项',
    new_deduction_amount DECIMAL(10,2) DEFAULT NULL COMMENT '变更后费用减项',
    new_payable_amount DECIMAL(10,2) DEFAULT NULL COMMENT '变更后应付工资',
    
    -- 变更原因
    change_reason VARCHAR(500) DEFAULT NULL COMMENT '变更原因说明',
    
    -- 审批状态
    status INT DEFAULT 0 COMMENT '状态：0-待审批，1-已通过，2-已驳回',
    
    -- 操作人信息
    created_by BIGINT DEFAULT NULL COMMENT '变更人ID',
    created_by_name VARCHAR(50) DEFAULT NULL COMMENT '变更人姓名',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    
    -- 审批信息
    approved_by BIGINT DEFAULT NULL COMMENT '审批人ID',
    approved_by_name VARCHAR(50) DEFAULT NULL COMMENT '审批人姓名',
    approved_time DATETIME DEFAULT NULL COMMENT '审批时间',
    approve_remark VARCHAR(500) DEFAULT NULL COMMENT '审批备注',
    
    INDEX idx_salary_slip_id (salary_slip_id),
    INDEX idx_project_period (project_id, salary_period),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资条修改记录表';
