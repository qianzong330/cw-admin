# 删除 tb_category.parent_id 字段完成清单

## 数据库修改

✅ **已执行**
1. 从 `tb_category` 表中删除 `parent_id` 列
   - SQL: `ALTER TABLE tb_category DROP COLUMN parent_id`
   - 创建迁移脚本：`V9__remove_category_parent_id.sql`

## 后端代码修改

✅ **实体类**
- `Category.java` - 已确认无 `parentId` 字段

✅ **Mapper 接口**
- `CategoryMapper.java` - 已确认无相关方法

✅ **Mapper XML**
- `CategoryMapper.xml` - 已确认无 `parent_id` 映射

✅ **Controller**
- `AccountController.java` - 将 `level1Categories` 改为 `categories`（2 处）

## 前端代码修改

✅ **模板文件**
- `account/list.html`:
  - 删除二级分类选择框（原 `categoryLevel2Id`）
  - 将一级分类选择框改名为 `categoryId`
  - 删除加载二级分类的 AJAX 调用
  - 删除 `categoryLevel1Id` 的 change 事件监听
  - 更新弹窗重置逻辑

## 验证结果

✅ 编译成功：`mvn clean compile -DskipTests` → BUILD SUCCESS

✅ 数据库表结构验证：
```
tb_category 表字段：
  - id (bigint)
  - name (varchar(50))
  - level (tinyint)
  - create_time (datetime)
  - update_time (datetime)
```

## 保留的 parent_id（无需删除）

以下 parent_id 属于菜单权限系统，与 category 无关，**予以保留**：
- `tb_menu` 表的 `parent_id` 字段
- `MenuMapper.xml` 中的映射
- `role/form.html` 中的菜单树形结构展示逻辑

## 清理的临时文件

可删除以下临时测试文件：
- `CheckCategoryTable.java`
- `RemoveParentId.java`

---

**状态**: ✅ 全部完成
**时间**: 2026-03-04
