-- 修改考勤变更日志表，允许 attendance_id 为 null
-- 原因：新增记录时还没有考勤记录ID，等审批通过后才创建
ALTER TABLE tb_attendance_change_log MODIFY COLUMN attendance_id BIGINT NULL COMMENT '考勤记录ID（新增时为空）';
