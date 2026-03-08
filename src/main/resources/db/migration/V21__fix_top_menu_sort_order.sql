-- 修复顶层菜单 sort_order，改为 1000 的倍数，避免与子菜单（0,1,2...）数字空间冲突
-- 按当前 sort_order 顺序重新编排：第1个=0, 第2个=1000, 第3个=2000, ...
SET @row := -1;
UPDATE tb_menu m
JOIN (
    SELECT id, (@row := @row + 1) * 1000 AS new_sort_order
    FROM tb_menu
    WHERE status = 1 AND (parent_id IS NULL OR parent_id = 0)
    ORDER BY sort_order, id
) t ON m.id = t.id
SET m.sort_order = t.new_sort_order;
