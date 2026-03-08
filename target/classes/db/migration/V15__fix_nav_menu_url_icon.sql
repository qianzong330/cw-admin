-- V15: 补全所有导航菜单的 menu_url 和 menu_icon 字段

-- 记账管理
UPDATE tb_menu SET menu_url = '/account/list',   menu_icon = 'bi-journal-text'
WHERE menu_code = 'account';

-- 考勤管理
UPDATE tb_menu SET menu_url = '/attendance/list', menu_icon = 'bi-calendar-check'
WHERE menu_code = 'attendance';

-- 工资条管理
UPDATE tb_menu SET menu_url = '/salary/list',     menu_icon = 'bi-cash-stack'
WHERE menu_code = 'salary';

-- 工时管理（menu_code 为 workhour:config）
UPDATE tb_menu SET menu_url = '/workhour/config', menu_icon = 'bi-clock'
WHERE menu_code = 'workhour:config';

-- 角色管理
UPDATE tb_menu SET menu_url = '/role/list',       menu_icon = 'bi-person-badge'
WHERE menu_code = 'role';

-- 员工管理
UPDATE tb_menu SET menu_url = '/employee/list',   menu_icon = 'bi-people'
WHERE menu_code = 'employee';

-- 费用分类（原分类管理）
UPDATE tb_menu SET menu_url = '/category/list',   menu_icon = 'bi-tags'
WHERE menu_code = 'category';

-- 工种管理（jobcategory:list 是 type=2 菜单页）
UPDATE tb_menu SET menu_url = '/jobcategory/list', menu_icon = 'bi-hammer'
WHERE menu_code = 'jobcategory:list';

-- 项目管理
UPDATE tb_menu SET menu_url = '/project/list',    menu_icon = 'bi-folder'
WHERE menu_code = 'project';

-- 确保所有侧边栏导航菜单的 parent_id 设为 0（顶级，直接展示）
UPDATE tb_menu SET parent_id = 0
WHERE menu_code IN ('account','attendance','salary','workhour:config','role','employee','category','jobcategory:list','project');
