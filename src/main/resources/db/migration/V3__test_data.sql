-- 测试数据
-- 注意：执行前请确保已有基础数据（角色、岗位等）

-- 1. 插入测试项目
INSERT INTO tb_project (name, create_time, update_time) VALUES
('阿里巴巴项目', NOW(), NOW()),
('腾讯项目', NOW(), NOW()),
('字节跳动项目', NOW(), NOW()),
('美团项目', NOW(), NOW()),
('京东项目', NOW(), NOW()),
('拼多多项目', NOW(), NOW()),
('百度项目', NOW(), NOW()),
('快手项目', NOW(), NOW());

-- 2. 插入测试费用分类（一级分类）
INSERT INTO tb_category (name, parent_id, create_time, update_time) VALUES
('办公费用', 0, NOW(), NOW()),
('差旅费用', 0, NOW(), NOW()),
('人力成本', 0, NOW(), NOW()),
('市场推广', 0, NOW(), NOW()),
('技术服务', 0, NOW(), NOW()),
('设备采购', 0, NOW(), NOW()),
('咨询费用', 0, NOW(), NOW()),
('租赁费用', 0, NOW(), NOW()),
('项目收入', 0, NOW(), NOW()),
('其他收入', 0, NOW(), NOW());

-- 3. 插入测试费用分类（二级分类）
INSERT INTO tb_category (name, parent_id, create_time, update_time) VALUES
('办公用品', 1, NOW(), NOW()),
('水电费', 1, NOW(), NOW()),
('交通费', 2, NOW(), NOW()),
('住宿费', 2, NOW(), NOW()),
('工资', 3, NOW(), NOW()),
('社保', 3, NOW(), NOW()),
('广告投放', 4, NOW(), NOW()),
('活动费用', 4, NOW(), NOW()),
('软件授权', 5, NOW(), NOW()),
('云服务', 5, NOW(), NOW()),
('电脑设备', 6, NOW(), NOW()),
('办公家具', 6, NOW(), NOW()),
('法律咨询', 7, NOW(), NOW()),
('财务咨询', 7, NOW(), NOW()),
('房租', 8, NOW(), NOW()),
('物业费', 8, NOW(), NOW()),
('软件开发', 9, NOW(), NOW()),
('技术服务', 9, NOW(), NOW()),
('利息收入', 10, NOW(), NOW()),
('投资收益', 10, NOW(), NOW());

-- 4. 插入测试员工（假设role_id=1是BOSS，role_id=2是财务，role_id=3是普通员工）
-- 注意：请根据实际角色ID调整
INSERT INTO tb_employee (name, role_id, phone, job_category_id, salary_amount, salary_type, password, finance_contact_id, status, create_time, update_time) VALUES
('张总', 1, '13800138001', 1, 50000.00, 1, '123456', NULL, 1, NOW(), NOW()),
('李财务', 2, '13800138002', 2, 15000.00, 1, '123456', 1, 1, NOW(), NOW()),
('王会计', 2, '13800138003', 2, 12000.00, 1, '123456', 1, 1, NOW(), NOW()),
('赵销售', 3, '13800138004', 3, 8000.00, 2, '123456', 2, 1, NOW(), NOW()),
('钱技术', 3, '13800138005', 4, 18000.00, 1, '123456', 2, 1, NOW(), NOW()),
('孙运营', 3, '13800138006', 5, 10000.00, 1, '123456', 2, 1, NOW(), NOW()),
('周产品', 3, '13800138007', 6, 20000.00, 1, '123456', 3, 1, NOW(), NOW()),
('吴设计', 3, '13800138008', 7, 12000.00, 1, '123456', 3, 1, NOW(), NOW());

-- 5. 插入测试帐条数据（收入 type=1，支出 type=2，状态：1=审批中，5=生效，12=驳回）
-- 阿里巴巴项目收入
INSERT INTO tb_account (project_id, creator_id, job_category_id, role_id, type, category_level1_id, category_level2_id, invoice_no, status, company_name, approval_progress_id, amount, remark, create_time, update_time) VALUES
(1, 4, 3, 3, 1, 9, 17, 'INV2024001', 5, '阿里巴巴', NULL, 500000.00, '软件开发收入', DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),
(1, 4, 3, 3, 1, 9, 18, 'INV2024002', 5, '阿里巴巴', NULL, 200000.00, '技术服务收入', DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(1, 4, 3, 3, 1, 10, 19, 'INV2024003', 5, '阿里巴巴', NULL, 50000.00, '利息收入', DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),

-- 腾讯项目收入
(2, 5, 4, 3, 1, 9, 17, 'INV2024004', 5, '腾讯科技', NULL, 800000.00, '软件开发收入', DATE_SUB(NOW(), INTERVAL 15 DAY), NOW()),
(2, 5, 4, 3, 1, 9, 18, 'INV2024005', 5, '腾讯科技', NULL, 300000.00, '技术服务收入', DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),

-- 字节跳动项目收入
(3, 6, 5, 3, 1, 9, 17, 'INV2024006', 5, '字节跳动', NULL, 600000.00, '软件开发收入', DATE_SUB(NOW(), INTERVAL 20 DAY), NOW()),
(3, 6, 5, 3, 1, 10, 20, 'INV2024007', 5, '字节跳动', NULL, 100000.00, '投资收益', DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),

-- 美团项目收入
(4, 4, 3, 3, 1, 9, 17, 'INV2024008', 5, '美团', NULL, 450000.00, '软件开发收入', DATE_SUB(NOW(), INTERVAL 25 DAY), NOW()),

-- 京东项目收入
(5, 5, 4, 3, 1, 9, 18, 'INV2024009', 5, '京东', NULL, 350000.00, '技术服务收入', DATE_SUB(NOW(), INTERVAL 18 DAY), NOW()),

-- 拼多多项目收入
(6, 6, 5, 3, 1, 9, 17, 'INV2024010', 5, '拼多多', NULL, 550000.00, '软件开发收入', DATE_SUB(NOW(), INTERVAL 22 DAY), NOW()),

-- 百度项目收入
(7, 7, 6, 3, 1, 9, 17, 'INV2024011', 5, '百度', NULL, 400000.00, '软件开发收入', DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),

-- 快手项目收入
(8, 8, 7, 3, 1, 9, 18, 'INV2024012', 5, '快手', NULL, 280000.00, '技术服务收入', DATE_SUB(NOW(), INTERVAL 28 DAY), NOW()),

-- 办公费用支出
(1, 4, 3, 3, 2, 1, 1, 'EXP2024001', 5, '办公用品公司', NULL, 5000.00, '购买办公用品', DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(1, 4, 3, 3, 2, 1, 2, 'EXP2024002', 5, '电力公司', NULL, 3000.00, '电费', DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),
(2, 5, 4, 3, 2, 1, 1, 'EXP2024003', 5, '办公用品公司', NULL, 8000.00, '购买办公用品', DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),
(3, 6, 5, 3, 2, 1, 2, 'EXP2024004', 5, '电力公司', NULL, 4500.00, '电费', DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),

-- 差旅费用支出
(1, 4, 3, 3, 2, 2, 3, 'EXP2024005', 5, '滴滴出行', NULL, 2500.00, '出差交通费', DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(1, 4, 3, 3, 2, 2, 4, 'EXP2024006', 5, '如家酒店', NULL, 3000.00, '出差住宿费', DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(2, 5, 4, 3, 2, 2, 3, 'EXP2024007', 5, '高铁', NULL, 1500.00, '出差交通费', DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(4, 4, 3, 3, 2, 2, 4, 'EXP2024008', 5, '希尔顿酒店', NULL, 5000.00, '出差住宿费', DATE_SUB(NOW(), INTERVAL 15 DAY), NOW()),

-- 人力成本支出
(1, 2, 2, 2, 2, 3, 5, 'EXP2024009', 5, '公司', NULL, 150000.00, '员工工资', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(1, 2, 2, 2, 2, 3, 6, 'EXP2024010', 5, '社保局', NULL, 30000.00, '社保缴纳', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(2, 2, 2, 2, 2, 3, 5, 'EXP2024011', 5, '公司', NULL, 200000.00, '员工工资', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),

-- 市场推广支出
(1, 4, 3, 3, 2, 4, 7, 'EXP2024012', 5, '百度推广', NULL, 50000.00, '广告投放', DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),
(1, 4, 3, 3, 2, 4, 8, 'EXP2024013', 5, '活动策划公司', NULL, 30000.00, '市场活动', DATE_SUB(NOW(), INTERVAL 4 DAY), NOW()),
(2, 5, 4, 3, 2, 4, 7, 'EXP2024014', 5, '腾讯广告', NULL, 80000.00, '广告投放', DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
(3, 6, 5, 3, 2, 4, 8, 'EXP2024015', 5, '活动策划公司', NULL, 45000.00, '市场活动', DATE_SUB(NOW(), INTERVAL 11 DAY), NOW()),

-- 技术服务支出
(1, 4, 3, 3, 2, 5, 9, 'EXP2024016', 5, '微软', NULL, 20000.00, '软件授权费', DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),
(1, 4, 3, 3, 2, 5, 10, 'EXP2024017', 5, '阿里云', NULL, 15000.00, '云服务费用', DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
(2, 5, 4, 3, 2, 5, 10, 'EXP2024018', 5, '腾讯云', NULL, 25000.00, '云服务费用', DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(3, 6, 5, 3, 2, 5, 9, 'EXP2024019', 5, 'Oracle', NULL, 35000.00, '数据库授权', DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),

-- 设备采购支出
(1, 4, 3, 3, 2, 6, 11, 'EXP2024020', 5, '联想', NULL, 60000.00, '采购电脑', DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
(2, 5, 4, 3, 2, 6, 12, 'EXP2024021', 5, '宜家', NULL, 25000.00, '办公家具', DATE_SUB(NOW(), INTERVAL 11 DAY), NOW()),

-- 咨询费用支出
(1, 2, 2, 2, 2, 7, 13, 'EXP2024022', 5, '律师事务所', NULL, 20000.00, '法律咨询', DATE_SUB(NOW(), INTERVAL 14 DAY), NOW()),
(1, 2, 2, 2, 2, 7, 14, 'EXP2024023', 5, '会计师事务所', NULL, 15000.00, '财务咨询', DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),

-- 租赁费用支出
(1, 2, 2, 2, 2, 8, 15, 'EXP2024024', 5, '房东', NULL, 50000.00, '办公室房租', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(1, 2, 2, 2, 2, 8, 16, 'EXP2024025', 5, '物业', NULL, 5000.00, '物业费', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),

-- 审批中的帐条（待审批）
(1, 4, 3, 3, 2, 1, 1, 'EXP2024026', 1, '办公用品公司', NULL, 3000.00, '待审批办公用品', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(2, 5, 4, 3, 2, 2, 3, 'EXP2024027', 1, '滴滴', NULL, 800.00, '待审批交通费', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),

-- 驳回的帐条
(1, 4, 3, 3, 2, 4, 7, 'EXP2024028', 12, '广告公司', NULL, 100000.00, '被驳回的广告费', DATE_SUB(NOW(), INTERVAL 5 DAY), NOW());

-- 6. 插入帐条操作明细
INSERT INTO tb_account_detail (project_id, account_id, operator_name, operator_id, action_type, remark, operate_time) VALUES
(1, 1, '赵销售', 4, 'CREATE', '创建帐条', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(1, 1, '李财务', 2, 'APPROVE', '审批通过', DATE_SUB(NOW(), INTERVAL 9 DAY)),
(1, 2, '赵销售', 4, 'CREATE', '创建帐条', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1, 2, '李财务', 2, 'APPROVE', '审批通过', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(2, 4, '钱技术', 5, 'CREATE', '创建帐条', DATE_SUB(NOW(), INTERVAL 15 DAY)),
(2, 4, '李财务', 2, 'APPROVE', '审批通过', DATE_SUB(NOW(), INTERVAL 14 DAY));
