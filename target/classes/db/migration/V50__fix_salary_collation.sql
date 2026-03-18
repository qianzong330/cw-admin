-- V50: 统一 tb_salary 和 tb_salary_month_status 的字符集排序规则
-- 原因：两张表 collation 不一致（utf8mb4_unicode_ci vs utf8mb4_0900_ai_ci），
--       导致 JOIN 时报 "Illegal mix of collations" 错误

-- 统一 tb_salary 表的字符集
ALTER TABLE tb_salary CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 统一 tb_salary_month_status 表的字符集
ALTER TABLE tb_salary_month_status CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
