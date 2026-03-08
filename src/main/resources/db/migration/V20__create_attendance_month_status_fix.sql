-- V20: 创建月度考勤表状态管理表（修复版）

-- 月度考勤表状态表
CREATE TABLE IF NOT EXISTS tb_attendance_month_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    `year_month` VARCHAR(7) NOT NULL COMMENT '年月，格式：yyyy-MM',
    project_id BIGINT COMMENT '项目ID，null表示全部项目',
    status INT DEFAULT 0 COMMENT '状态：0-草稿，1-待审批，2-变更待审，5-已审批锁定，12-已驳回',
    submit_by BIGINT COMMENT '提交人ID',
    submit_time DATETIME COMMENT '提交时间',
    submit_remark VARCHAR(500) COMMENT '提交备注',
    approve_by BIGINT COMMENT '审批人ID',
    approve_time DATETIME COMMENT '审批时间',
    approve_remark VARCHAR(500) COMMENT '审批备注',
    change_apply_by BIGINT COMMENT '变更申请人ID',
    change_apply_time DATETIME COMMENT '变更申请时间',
    change_apply_remark VARCHAR(500) COMMENT '变更申请备注（需说明修改内容）',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_year_month_project (`year_month`, project_id)
) COMMENT='月度考勤表状态管理';
