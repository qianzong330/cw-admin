-- 考勤修改记录表
-- 用于记录考勤审批通过后二次修改的变更内容，供审批人查看
CREATE TABLE IF NOT EXISTS tb_attendance_change_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    attendance_id BIGINT NOT NULL COMMENT '考勤记录ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    work_date DATE NOT NULL COMMENT '工作日期',
    `year_month` VARCHAR(7) NOT NULL COMMENT '所属年月(yyyy-MM)',
    
    -- 变更前数据
    old_attendance_type TINYINT DEFAULT NULL COMMENT '变更前出勤类型',
    old_overtime_type TINYINT DEFAULT NULL COMMENT '变更前加班类型',
    old_work_hours DECIMAL(4,1) DEFAULT NULL COMMENT '变更前工作时长',
    old_remark VARCHAR(200) DEFAULT NULL COMMENT '变更前备注',
    
    -- 变更后数据
    new_attendance_type TINYINT DEFAULT NULL COMMENT '变更后出勤类型',
    new_overtime_type TINYINT DEFAULT NULL COMMENT '变更后加班类型',
    new_work_hours DECIMAL(4,1) DEFAULT NULL COMMENT '变更后工作时长',
    new_remark VARCHAR(200) DEFAULT NULL COMMENT '变更后备注',
    
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
    
    INDEX idx_attendance_id (attendance_id),
    INDEX idx_project_month (project_id, `year_month`),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤修改记录表';
