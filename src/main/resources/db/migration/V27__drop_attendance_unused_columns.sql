-- V27: 删除 tb_attendance 表的无用字段（状态由月份状态表统一控制）
-- 员工考勤记录本身不需要状态、审批人、审批时间字段

-- 删除 status 字段
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = DATABASE() 
            AND table_name = 'tb_attendance' 
            AND column_name = 'status'
        ),
        'ALTER TABLE tb_attendance DROP COLUMN status',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 删除 approved_by 字段
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = DATABASE() 
            AND table_name = 'tb_attendance' 
            AND column_name = 'approved_by'
        ),
        'ALTER TABLE tb_attendance DROP COLUMN approved_by',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 删除 approved_time 字段
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = DATABASE() 
            AND table_name = 'tb_attendance' 
            AND column_name = 'approved_time'
        ),
        'ALTER TABLE tb_attendance DROP COLUMN approved_time',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
