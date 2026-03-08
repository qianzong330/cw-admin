-- 删除员工考勤表的状态和审批相关字段（单个考勤无需审批）
ALTER TABLE tb_attendance DROP COLUMN IF EXISTS status;
ALTER TABLE tb_attendance DROP COLUMN IF EXISTS approved_by;
ALTER TABLE tb_attendance DROP COLUMN IF EXISTS approved_time;
