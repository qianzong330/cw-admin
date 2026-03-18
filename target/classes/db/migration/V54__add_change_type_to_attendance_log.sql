-- 添加 change_type 列到考勤修改记录表（如果不存在）
ALTER TABLE tb_attendance_change_log
ADD COLUMN IF NOT EXISTS change_type VARCHAR(20) NOT NULL DEFAULT 'EDIT' COMMENT '变更类型：ADD-新增, EDIT-编辑, DELETE-删除' AFTER `year_month`;

-- 添加索引（如果不存在）
CREATE INDEX IF NOT EXISTS idx_change_type ON tb_attendance_change_log(change_type);
