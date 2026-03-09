-- V30: 添加帐条多级审批流程字段
-- 支持：员工→财务→BOSS 多级审批

-- 审批阶段：1-待财务审批，2-待BOSS审批，5-生效，12-驳回
ALTER TABLE tb_account ADD COLUMN IF NOT EXISTS approval_stage TINYINT DEFAULT 1 COMMENT '审批阶段：1-待财务审批，2-待BOSS审批';

-- 记录已审批的财务人员ID（逗号分隔）
ALTER TABLE tb_account ADD COLUMN IF NOT EXISTS approved_by_finance VARCHAR(500) COMMENT '已审批的财务人员ID列表（逗号分隔）';

-- 最终审批人ID（BOSS）
ALTER TABLE tb_account ADD COLUMN IF NOT EXISTS final_approver_id BIGINT COMMENT '最终审批人ID（BOSS）';
