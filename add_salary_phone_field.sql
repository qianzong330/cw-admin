-- 给工资条表添加手机号字段
ALTER TABLE tb_salary 
ADD COLUMN phone VARCHAR(20) NULL COMMENT '手机号' AFTER id_card;

-- 更新现有工资条数据，从员工表同步正确的身份证号和手机号
UPDATE tb_salary s
JOIN tb_employee e ON s.employee_id = e.id
SET s.id_card = e.id_card,
    s.phone = e.phone;
