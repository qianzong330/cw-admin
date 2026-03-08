-- 修复工时配置表中的状态值
-- 将旧状态映射到新状态：
-- 0 (未生效) -> 12 (未生效)
-- 2 (生效中) -> 5 (生效中)
-- 1 (审批中) -> 1 (审批中，保持不变)

UPDATE tb_work_hour_config SET status = 12 WHERE status = 0;
UPDATE tb_work_hour_config SET status = 5 WHERE status = 2;

-- 验证更新结果
SELECT id, calc_type, status, created_by_name, created_time
FROM tb_work_hour_config
ORDER BY id;
