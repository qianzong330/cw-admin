-- 修改 tb_salary 唯一约束：允许同一员工在不同项目各有一条同期工资条
-- 旧约束：uk_employee_period (employee_id, salary_period)
-- 新约束：uk_employee_period_project (employee_id, salary_period, project_id)

-- 先删除旧唯一约束
ALTER TABLE tb_salary DROP INDEX uk_employee_period;

-- 新增包含 project_id 的唯一约束
ALTER TABLE tb_salary ADD UNIQUE KEY uk_employee_period_project (employee_id, salary_period, project_id);

-- 清理因旧约束导致的重复数据（保留 project_id 不为 NULL 的记录，删除重复的 NULL 记录）
DELETE s1 FROM tb_salary s1
INNER JOIN tb_salary s2
    ON s1.employee_id = s2.employee_id
    AND s1.salary_period = s2.salary_period
    AND s1.project_id IS NULL
    AND s2.project_id IS NOT NULL
    AND s1.id > s2.id;
