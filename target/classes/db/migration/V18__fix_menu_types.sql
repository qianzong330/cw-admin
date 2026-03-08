-- V18: 修复菜单类型和数据，支持层级展示

-- 修复 home：改为 type=2（直接链接菜单），首页固定显示不走动态渲染
UPDATE tb_menu SET menu_type = 2 WHERE menu_code = 'home';

-- 修复 employee：type 应为 2（直接链接菜单），不是目录
UPDATE tb_menu SET menu_type = 2 WHERE menu_code = 'employee';

-- 确保 jobcategory 有正确的 url 和 icon
UPDATE tb_menu SET url = '/jobcategory/list', icon = 'bi-hammer' WHERE menu_code = 'jobcategory';

-- 确保 basic_config 图标正确
UPDATE tb_menu SET icon = 'bi-gear-wide-connected' WHERE menu_code = 'basic_config';
