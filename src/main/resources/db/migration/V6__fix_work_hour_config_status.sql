-- 修复工时配置表中 status 为 NULL 的数据
-- 将 NULL 值更新为 0 (未生效状态)
UPDATE tb_work_hour_config
SET status = 0
WHERE status IS NULL;

-- 验证更新结果
SELECT id, calc_type, status, created_by_name, created_time
FROM tb_work_hour_config
ORDER BY id;
