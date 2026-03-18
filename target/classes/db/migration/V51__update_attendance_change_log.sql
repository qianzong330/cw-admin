-- 修改考勤修改记录表，支持新增和删除操作
ALTER TABLE tb_attendance_change_log
ADD COLUMN change_type VARCHAR(20) NOT NULL DEFAULT 'EDIT' COMMENT '变更类型：ADD-新增, EDIT-编辑, DELETE-删除' AFTER `year_month`;

-- 修改索引，支持按变更类型查询
CREATE INDEX idx_change_type ON tb_attendance_change_log(change_type);
