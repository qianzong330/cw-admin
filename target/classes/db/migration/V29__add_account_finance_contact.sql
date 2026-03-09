-- V29: 添加帐条财务对接人字段
-- 用于存储该帐条的审批人（财务对接人或BOSS）

ALTER TABLE tb_account ADD COLUMN IF NOT EXISTS finance_contact_id BIGINT COMMENT '财务对接人ID（审批人）';
