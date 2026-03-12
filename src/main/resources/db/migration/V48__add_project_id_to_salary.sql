-- 为工资条表添加 project_id 字段，实现按项目隔离考勤数据
ALTER TABLE tb_salary ADD COLUMN IF NOT EXISTS project_id BIGINT DEFAULT NULL COMMENT '项目ID';

-- 为现有工资条数据补充 project_id（通过 tb_employee_project 关联）
-- 若员工只属于一个项目则直接关联；属于多个项目则暂设为 NULL（不影响新数据）
UPDATE tb_salary s
SET project_id = (
    SELECT ep.project_id
    FROM tb_employee_project ep
    WHERE ep.employee_id = s.employee_id
    LIMIT 1
)
WHERE s.project_id IS NULL;
