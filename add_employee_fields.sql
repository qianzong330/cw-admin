-- 为员工表添加身份证号和籍贯地址字段
ALTER TABLE tb_employee 
ADD COLUMN id_card VARCHAR(18) NULL COMMENT '身份证号' AFTER phone,
ADD COLUMN native_address VARCHAR(100) NULL COMMENT '籍贯地址' AFTER id_card;

-- 创建索引（可选，用于按籍贯搜索）
-- ALTER TABLE tb_employee ADD INDEX idx_native_address (native_address);
