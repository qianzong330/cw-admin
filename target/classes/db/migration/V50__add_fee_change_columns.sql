-- 工资条变更记录表添加费用项变更相关字段
ALTER TABLE tb_salary_slip_change_log
ADD COLUMN change_type VARCHAR(20) DEFAULT NULL COMMENT '变更类型：ADD-新增, EDIT-修改, DELETE-删除, BATCH-批量',
ADD COLUMN fee_items_change_detail TEXT DEFAULT NULL COMMENT '费用项变更详情（JSON格式）',
ADD COLUMN old_fee_items_json TEXT DEFAULT NULL COMMENT '变更前费用项JSON（完整备份）',
ADD COLUMN new_fee_items_json TEXT DEFAULT NULL COMMENT '变更后费用项JSON（完整备份）',
ADD COLUMN is_fee_change TINYINT(1) DEFAULT 0 COMMENT '是否为费用项变更：0-否, 1-是';

-- 添加索引优化查询
CREATE INDEX idx_is_fee_change ON tb_salary_slip_change_log(is_fee_change);
CREATE INDEX idx_change_type ON tb_salary_slip_change_log(change_type);
