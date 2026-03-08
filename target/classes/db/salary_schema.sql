-- 员工考勤管理模块表结构

-- 1. 考勤记录表
CREATE TABLE IF NOT EXISTS tb_attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    project_id BIGINT COMMENT '项目ID',
    work_date DATE NOT NULL COMMENT '工作日期',
    attendance_type TINYINT NOT NULL DEFAULT 1 COMMENT '出勤类型：1-正常出勤，2-加班，3-请假，4-缺勤',
    overtime_type TINYINT COMMENT '加班类型：1-工作日加班，2-休息日加班，3-法定假期加班',
    work_hours DECIMAL(4,1) DEFAULT 8.0 COMMENT '工作时长（小时）',
    remark VARCHAR(200) COMMENT '备注',
    status TINYINT COMMENT '状态：1-待审批，2-已审批，3-已驳回，null-可编辑',
    approved_by BIGINT COMMENT '审批人ID',
    approved_time DATETIME COMMENT '审批时间',
    created_by BIGINT COMMENT '创建人ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_employee_date (employee_id, work_date),
    INDEX idx_work_date (work_date),
    INDEX idx_project (project_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工考勤记录表';

-- 2. 工资表
CREATE TABLE IF NOT EXISTS tb_salary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    salary_period VARCHAR(20) NOT NULL COMMENT '工资月份（格式：yyyy-MM）',
    salary_type TINYINT NOT NULL DEFAULT 1 COMMENT '薪资类型：1-日薪，2-月薪',
    base_salary DECIMAL(12,2) NOT NULL COMMENT '基础薪资（日薪或月薪金额）',
    attendance_days INT DEFAULT 0 COMMENT '出勤天数',
    base_amount DECIMAL(12,2) DEFAULT 0 COMMENT '基础工资小计（日薪*天数 或 月薪）',
    addition_amount DECIMAL(12,2) DEFAULT 0 COMMENT '费用+合计（应加项）',
    deduction_amount DECIMAL(12,2) DEFAULT 0 COMMENT '费用-合计（应减项）',
    payable_amount DECIMAL(12,2) DEFAULT 0 COMMENT '应付工资（base_amount + addition - deduction）',
    status TINYINT DEFAULT 1 COMMENT '状态：1-草稿，2-待审批，3-已确认，4-已发放，5-已作废',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT COMMENT '创建人ID',
    approved_by BIGINT COMMENT '审批人ID',
    approved_time DATETIME COMMENT '审批时间',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_employee_period (employee_id, salary_period),
    INDEX idx_salary_period (salary_period),
    INDEX idx_status (status),
    UNIQUE KEY uk_employee_period (employee_id, salary_period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工工资表';

-- 3. 工资费用项表（费用+ 和 费用-）
CREATE TABLE IF NOT EXISTS tb_salary_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    salary_slip_id BIGINT NOT NULL COMMENT '工资条ID',
    item_type TINYINT NOT NULL COMMENT '费用类型：1-费用+（应加），2-费用-（应减）',
    item_name VARCHAR(50) NOT NULL COMMENT '费用名称，如：路费、微信转账',
    amount DECIMAL(12,2) NOT NULL COMMENT '金额',
    remark VARCHAR(200) COMMENT '备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_salary_slip (salary_slip_id),
    INDEX idx_item_type (item_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资费用项表';

-- 4. 员工项目关联表
CREATE TABLE IF NOT EXISTS tb_employee_project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_employee_project (employee_id, project_id),
    INDEX idx_employee (employee_id),
    INDEX idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工项目关联表';

-- 5. 工时管理配置表
CREATE TABLE IF NOT EXISTS tb_work_hour_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    config_name VARCHAR(50) NOT NULL COMMENT '配置名称',
    daily_work_hours DECIMAL(4,1) NOT NULL DEFAULT 8.0 COMMENT '每日标准工作时长（小时）',
    work_start_time TIME NOT NULL DEFAULT '09:00:00' COMMENT '工作开始时间',
    work_end_time TIME NOT NULL DEFAULT '18:00:00' COMMENT '工作结束时间',
    overtime_start_time TIME COMMENT '加班开始时间（为空则按工作结束时间）',
    min_overtime_hours DECIMAL(3,1) DEFAULT 0.5 COMMENT '最小加班时长（小时）',
    -- 工作日加班费率
    weekday_overtime_rate DECIMAL(5,2) DEFAULT 1.5 COMMENT '工作日加班费率（倍）',
    weekday_overtime_hourly DECIMAL(10,2) COMMENT '工作日加班时薪（元/小时，为空则按基本工资计算）',
    -- 休息日加班费率
    restday_overtime_rate DECIMAL(5,2) DEFAULT 2.0 COMMENT '休息日加班费率（倍）',
    restday_overtime_hourly DECIMAL(10,2) COMMENT '休息日加班时薪（元/小时）',
    -- 节假日加班费率
    holiday_overtime_rate DECIMAL(5,2) DEFAULT 3.0 COMMENT '节假日加班费率（倍）',
    holiday_overtime_hourly DECIMAL(10,2) COMMENT '节假日加班时薪（元/小时）',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认配置：0-否，1-是',
    remark VARCHAR(200) COMMENT '备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工时管理配置表';

-- 5. 工时配置表
CREATE TABLE IF NOT EXISTS tb_work_hour_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    config_name VARCHAR(50) NOT NULL COMMENT '配置名称',
    daily_work_hours DECIMAL(4,1) DEFAULT 8.0 COMMENT '每日标准工作时长（小时）',
    work_start_time TIME COMMENT '工作开始时间',
    work_end_time TIME COMMENT '工作结束时间',
    overtime_start_threshold DECIMAL(4,1) DEFAULT 0.0 COMMENT '加班起算阈值（小时）',
    min_overtime_hours DECIMAL(3,1) DEFAULT 0.5 COMMENT '最小加班时长（小时）',
    weekday_overtime_rate DECIMAL(3,1) DEFAULT 1.5 COMMENT '工作日加班费率',
    restday_overtime_rate DECIMAL(3,1) DEFAULT 2.0 COMMENT '休息日加班费率',
    holiday_overtime_rate DECIMAL(3,1) DEFAULT 3.0 COMMENT '法定节假日加班费率',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认配置：0-否，1-是',
    remark VARCHAR(200) COMMENT '备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工时配置表';


