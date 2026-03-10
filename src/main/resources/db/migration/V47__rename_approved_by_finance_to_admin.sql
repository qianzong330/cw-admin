-- 将approved_by_finance字段重命名为approved_by_admin
-- 首先检查字段是否存在
SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns 
    WHERE table_schema = DATABASE() AND table_name = 'tb_account' AND column_name = 'approved_by_finance');

-- 如果旧字段存在，则重命名
SET @sql = IF(@column_exists > 0, 
    'ALTER TABLE tb_account CHANGE COLUMN approved_by_finance approved_by_admin VARCHAR(500) COMMENT ''已审批的管理员ID列表''', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 更新审批阶段注释
ALTER TABLE tb_account MODIFY COLUMN approval_stage TINYINT DEFAULT 1 COMMENT '审批阶段：1-待管理员审批，2-待BOSS审批';
