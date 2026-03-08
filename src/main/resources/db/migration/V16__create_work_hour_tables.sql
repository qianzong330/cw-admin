-- V16: 创建工时配置表（修复考勤保存报错）

-- 工时配置表
CREATE TABLE IF NOT EXISTS tb_work_hour_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    calc_type TINYINT NOT NULL DEFAULT 2 COMMENT '计算方式：1-日薪计算，2-月薪计算',
    monthly_work_days INT DEFAULT 22 COMMENT '每月工作天数（仅月薪计算使用）',
    morning_start_time TIME DEFAULT '08:00:00' COMMENT '上午开始时间',
    morning_end_time TIME DEFAULT '12:00:00' COMMENT '上午结束时间',
    afternoon_start_time TIME DEFAULT '13:00:00' COMMENT '下午开始时间',
    afternoon_end_time TIME DEFAULT '17:00:00' COMMENT '下午结束时间',
    daily_work_hours DECIMAL(4,1) DEFAULT 8.0 COMMENT '每日标准工作时长（小时，自动计算）',
    overtime_start_time TIME COMMENT '加班开始时间',
    min_overtime_hours DECIMAL(3,1) DEFAULT 0.5 COMMENT '最小加班时长（小时）',
    weekday_overtime_rate DECIMAL(3,1) DEFAULT 1.5 COMMENT '工作日加班费率',
    weekday_overtime_hourly DECIMAL(10,2) COMMENT '工作日加班时薪',
    restday_overtime_rate DECIMAL(3,1) DEFAULT 2.0 COMMENT '休息日加班费率',
    restday_overtime_hourly DECIMAL(10,2) COMMENT '休息日加班时薪',
    holiday_overtime_rate DECIMAL(3,1) DEFAULT 3.0 COMMENT '法定节假日加班费率',
    holiday_overtime_hourly DECIMAL(10,2) COMMENT '节假日加班时薪',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认配置：0-否，1-是',
    status TINYINT DEFAULT 12 COMMENT '状态：1-审批中，5-生效中，12-未生效',
    created_by_id BIGINT COMMENT '发起人ID',
    created_by_name VARCHAR(50) COMMENT '发起人姓名',
    approved_by_id BIGINT COMMENT '审批人ID',
    approved_by_name VARCHAR(50) COMMENT '审批人姓名',
    approved_time DATETIME COMMENT '审批时间',
    approve_remark VARCHAR(500) COMMENT '审批备注',
    remark VARCHAR(200) COMMENT '备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工时配置表';

-- 插入一条默认工时配置（供所有员工使用）
INSERT IGNORE INTO tb_work_hour_config (
    id, calc_type, monthly_work_days,
    morning_start_time, morning_end_time, afternoon_start_time, afternoon_end_time,
    daily_work_hours, min_overtime_hours,
    weekday_overtime_rate, restday_overtime_rate, holiday_overtime_rate,
    is_default, status
) VALUES (
    1, 2, 22,
    '08:00:00', '12:00:00', '13:00:00', '17:00:00',
    8.0, 0.5,
    1.5, 2.0, 3.0,
    1, 5
);
