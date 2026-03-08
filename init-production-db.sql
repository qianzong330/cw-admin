-- 生产数据库初始化脚本
-- 在 Sealosh 数据库 hsc-online 的数据管理里执行

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
    id_card VARCHAR(50) COMMENT '身份证号',
    job_category_name VARCHAR(50) COMMENT '工种名称',
    salary_period VARCHAR(20) NOT NULL COMMENT '工资月份（格式：yyyy-MM）',
    salary_type TINYINT NOT NULL DEFAULT 1 COMMENT '薪资类型：1-日薪，2-月薪',
    base_salary DECIMAL(12,2) NOT NULL COMMENT '基础薪资（日薪或月薪金额）',
    attendance_days INT DEFAULT 0 COMMENT '出勤天数',
    base_amount DECIMAL(12,2) DEFAULT 0 COMMENT '基础工资小计（日薪*天数 或 月薪）',
    addition_amount DECIMAL(12,2) DEFAULT 0 COMMENT '费用+合计（应加项）',
    deduction_amount DECIMAL(12,2) DEFAULT 0 COMMENT '费用-合计（应减项）',
    payable_amount DECIMAL(12,2) DEFAULT 0 COMMENT '应付工资（base_amount + addition - deduction）',
    remark VARCHAR(500) COMMENT '备注',
    approve_remark VARCHAR(500) COMMENT '审批备注',
    created_by BIGINT COMMENT '创建人ID',
    approved_by BIGINT COMMENT '审批人ID',
    approved_time DATETIME COMMENT '审批时间',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_employee_period (employee_id, salary_period),
    INDEX idx_salary_period (salary_period),
    UNIQUE KEY uk_employee_period (employee_id, salary_period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工工资表';

-- 3. 工资费用项表
CREATE TABLE IF NOT EXISTS tb_salary_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    salary_slip_id BIGINT NOT NULL COMMENT '工资条ID',
    item_type TINYINT NOT NULL COMMENT '费用类型：1-费用+（应加），2-费用-（应减）',
    item_name VARCHAR(50) NOT NULL COMMENT '费用名称',
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

-- 5. 工时配置表
CREATE TABLE IF NOT EXISTS tb_work_hour_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    calc_type TINYINT NOT NULL DEFAULT 1 COMMENT '计算方式：1-日薪计算，2-月薪计算',
    monthly_work_days INT DEFAULT 22 COMMENT '每月工作天数，仅月薪计算时使用',
    morning_start_time VARCHAR(10) DEFAULT '08:00' COMMENT '上午开始时间',
    morning_end_time VARCHAR(10) DEFAULT '12:00' COMMENT '上午结束时间',
    afternoon_start_time VARCHAR(10) DEFAULT '13:00' COMMENT '下午开始时间',
    afternoon_end_time VARCHAR(10) DEFAULT '17:00' COMMENT '下午结束时间',
    daily_work_hours DECIMAL(4,1) DEFAULT 8.0 COMMENT '每日工时',
    overtime_start_time VARCHAR(10) COMMENT '加班开始时间',
    min_overtime_hours DECIMAL(3,1) DEFAULT 0.5 COMMENT '最小加班时长',
    weekday_overtime_rate DECIMAL(5,2) DEFAULT 1.5 COMMENT '工作日加班费率',
    weekday_overtime_hourly DECIMAL(10,2) COMMENT '工作日加班时薪',
    restday_overtime_rate DECIMAL(5,2) DEFAULT 2.0 COMMENT '休息日加班费率',
    restday_overtime_hourly DECIMAL(10,2) COMMENT '休息日加班时薪',
    holiday_overtime_rate DECIMAL(5,2) DEFAULT 3.0 COMMENT '节假日加班费率',
    holiday_overtime_hourly DECIMAL(10,2) COMMENT '节假日加班时薪',
    status TINYINT DEFAULT 0 COMMENT '状态：0-未生效，1-审批中，2-生效中',
    created_by_id BIGINT COMMENT '发起人ID',
    created_by_name VARCHAR(50) COMMENT '发起人姓名',
    approved_by_id BIGINT COMMENT '审批人ID',
    approved_by_name VARCHAR(50) COMMENT '审批人姓名',
    approved_time DATETIME COMMENT '审批时间',
    approve_remark VARCHAR(500) COMMENT '审批备注',
    remark VARCHAR(200) COMMENT '备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_calc_type (calc_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工时管理配置表';

-- 6. 考勤月份状态表
CREATE TABLE IF NOT EXISTS tb_attendance_month_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    year_month VARCHAR(20) NOT NULL COMMENT '年月（格式：yyyy-MM）',
    project_id BIGINT COMMENT '项目ID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-可编辑，1-待审批，2-已审批，3-已驳回',
    submit_by BIGINT COMMENT '提交人ID',
    submit_time DATETIME COMMENT '提交时间',
    approve_by BIGINT COMMENT '审批人ID',
    approve_time DATETIME COMMENT '审批时间',
    approve_remark VARCHAR(500) COMMENT '审批备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_year_month_project (year_month, project_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤月份状态表';

-- 7. 工资月份状态表
CREATE TABLE IF NOT EXISTS tb_salary_month_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    year_month VARCHAR(20) NOT NULL COMMENT '年月（格式：yyyy-MM）',
    project_id BIGINT COMMENT '项目ID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-草稿，1-待审批，2-已审批，3-已驳回，4-已发放，5-已锁定',
    submit_by BIGINT COMMENT '提交人ID',
    submit_time DATETIME COMMENT '提交时间',
    approve_by BIGINT COMMENT '审批人ID',
    approve_time DATETIME COMMENT '审批时间',
    approve_remark VARCHAR(500) COMMENT '审批备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_year_month_project (year_month, project_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资月份状态表';

-- 8. 员工表
CREATE TABLE IF NOT EXISTS tb_employee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    phone VARCHAR(20) COMMENT '手机号',
    id_card VARCHAR(50) COMMENT '身份证号',
    job_category_id BIGINT COMMENT '工种ID',
    job_category_name VARCHAR(50) COMMENT '工种名称',
    salary_type TINYINT DEFAULT 1 COMMENT '薪资类型：1-日薪，2-月薪',
    salary_amount DECIMAL(12,2) DEFAULT 0 COMMENT '薪资金额',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_job_category (job_category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';

-- 9. 项目表
CREATE TABLE IF NOT EXISTS tb_project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '项目名称',
    code VARCHAR(50) COMMENT '项目编号',
    description VARCHAR(500) COMMENT '项目描述',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- 10. 记账分类表
CREATE TABLE IF NOT EXISTS tb_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    code VARCHAR(50) COMMENT '分类编码',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记账分类表';

-- 11. 记账明细表
CREATE TABLE IF NOT EXISTS tb_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    category_id BIGINT COMMENT '分类ID',
    project_id BIGINT COMMENT '项目ID',
    amount DECIMAL(12,2) NOT NULL COMMENT '金额',
    type TINYINT NOT NULL COMMENT '类型：1-收入，2-支出',
    record_date DATE NOT NULL COMMENT '记账日期',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT COMMENT '创建人ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (category_id),
    INDEX idx_project (project_id),
    INDEX idx_record_date (record_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记账明细表';

-- 12. 菜单表
CREATE TABLE IF NOT EXISTS tb_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    menu_code VARCHAR(50) NOT NULL COMMENT '菜单编码',
    menu_name VARCHAR(50) NOT NULL COMMENT '菜单名称',
    menu_type TINYINT NOT NULL COMMENT '菜单类型：1-目录，2-菜单，3-按钮',
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID',
    menu_icon VARCHAR(50) COMMENT '菜单图标',
    menu_url VARCHAR(200) COMMENT '菜单URL',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_menu_code (menu_code),
    INDEX idx_parent (parent_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 13. 角色表
CREATE TABLE IF NOT EXISTS tb_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_role_code (role_code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 14. 角色菜单关联表
CREATE TABLE IF NOT EXISTS tb_role_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_menu (role_id, menu_id),
    INDEX idx_role (role_id),
    INDEX idx_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 15. 工种表
CREATE TABLE IF NOT EXISTS tb_job_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '工种名称',
    code VARCHAR(50) COMMENT '工种编码',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工种表';

-- 插入默认超级管理员
INSERT INTO tb_employee (id, name, phone, salary_type, salary_amount, status) VALUES 
(1, 'root', 'root', 1, 0, 1);

-- 插入默认角色
INSERT INTO tb_role (id, role_code, role_name, status) VALUES 
(1, 'root', '超级管理员', 1),
(2, 'boss', 'BOSS', 1),
(3, 'finance', '财务', 1);

-- 插入默认菜单（简化版，只插入关键菜单）
INSERT INTO tb_menu (id, menu_code, menu_name, menu_type, parent_id, menu_icon, menu_url, sort_order, status) VALUES 
(1, 'dashboard', '首页', 2, 0, 'bi-house', '/index', 1, 1),
(2, 'project', '工程管理', 1, 0, 'bi-building', '#', 2, 1),
(3, 'project:list', '项目管理', 2, 2, 'bi-list', '/project/list', 1, 1),
(4, 'workhour', '工时管理', 1, 0, 'bi-clock', '#', 3, 1),
(5, 'workhour:config', '工时配置', 2, 4, 'bi-gear', '/workhour/config', 1, 1),
(6, 'attendance', '考勤管理', 1, 0, 'bi-calendar', '#', 4, 1),
(7, 'attendance:list', '考勤记录', 2, 6, 'bi-list', '/attendance/list', 1, 1),
(8, 'salary', '工资条管理', 1, 0, 'bi-cash', '#', 5, 1),
(9, 'salary:list', '工资条列表', 2, 8, 'bi-list', '/salary/list', 1, 1),
(10, 'account', '记账管理', 1, 0, 'bi-book', '#', 6, 1),
(11, 'account:list', '记账列表', 2, 10, 'bi-list', '/account/list', 1, 1),
(12, 'system', '系统设置', 1, 0, 'bi-gear', '#', 7, 1),
(13, 'system:menu', '菜单管理', 2, 12, 'bi-list', '/menu/list', 1, 1),
(14, 'system:role', '角色管理', 2, 12, 'bi-people', '/role/list', 2, 1),
(15, 'system:employee', '员工管理', 2, 12, 'bi-person', '/employee/list', 3, 1);

-- 给 root 角色分配所有菜单权限
INSERT INTO tb_role_menu (role_id, menu_id) 
SELECT 1, id FROM tb_menu WHERE status = 1;

-- 给 boss 角色分配基本权限
INSERT INTO tb_role_menu (role_id, menu_id) VALUES 
(2, 1), (2, 3), (2, 5), (2, 7), (2, 9), (2, 11), (2, 14), (2, 15);
