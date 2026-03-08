-- 清空所有测试数据（保留基础配置表）
SET FOREIGN_KEY_CHECKS = 0;

-- 清空业务数据
TRUNCATE TABLE tb_account;
TRUNCATE TABLE tb_account_detail;
TRUNCATE TABLE tb_attendance;
TRUNCATE TABLE tb_employee;
TRUNCATE TABLE tb_role_menu;
TRUNCATE TABLE tb_salary_slip;
TRUNCATE TABLE tb_salary_item;
TRUNCATE TABLE tb_work_hour_config;

-- 清空基础数据（除了系统和菜单表）
TRUNCATE TABLE tb_category;
TRUNCATE TABLE tb_project;
TRUNCATE TABLE tb_job_category;

-- 清空角色相关数据
TRUNCATE TABLE tb_role;

SET FOREIGN_KEY_CHECKS = 1;
