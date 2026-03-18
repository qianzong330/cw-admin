-- 测试年度汇总查询 SQL
-- 用于验证 sumYearTotal 查询是否正确

-- 参数替换示例：projectId = 1, year = '2026'
SELECT 
    COALESCE(SUM(s.base_amount), 0) AS totalBaseAmount,
    COALESCE(SUM(s.addition_amount), 0) AS totalAdditionAmount,
    COALESCE(SUM(s.deduction_amount), 0) AS totalDeductionAmount,
    COALESCE(SUM(s.payable_amount), 0) AS totalPayableAmount,
    COUNT(DISTINCT s.salary_period) AS approvedMonths
FROM tb_salary s
INNER JOIN tb_salary_month_status sms 
    ON s.project_id = sms.project_id 
    AND s.salary_period = sms.year_month
WHERE s.project_id = 1
  AND s.salary_period LIKE CONCAT('2026', '%')
  AND sms.status = 5;

-- 说明：
-- 1. 此查询关联了 tb_salary 和 tb_salary_month_status 表
-- 2. 只统计 status = 5 (已审批通过/已锁定) 的月份
-- 3. 使用 LIKE 进行年度模糊匹配（格式：yyyy-MM）
-- 4. 所有金额字段使用 COALESCE 处理 NULL 值，确保返回 0 而不是 NULL
