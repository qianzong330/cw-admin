# 工资条年度汇总功能开发总结

## 功能概述
在工资条管理页面新增"年度汇总"按钮，点击后可统计并展示当前项目下指定年度的员工工资条汇总信息。

## 修改内容

### 1. 后端修改

#### 1.1 Mapper 层 (SalarySlipMapper.java)
- 新增方法：`sumYearTotal(@Param("projectId") Long projectId, @Param("year") String year)`
- 功能：统计某项目某年度的汇总工资条数据（仅统计已审批通过的月份）

#### 1.2 MyBatis XML (mapper/SalarySlipMapper.xml)
- 新增 SQL 查询：`sumYearTotal`
- 查询逻辑：
  - 关联 `tb_salary` 和 `tb_salary_month_status` 表
  - 筛选条件：项目 ID、年度、状态为已审批通过（status=5）
  - 汇总字段：
    - `totalBaseAmount`: 考勤工资总和
    - `totalAdditionAmount`: 费用加项总和
    - `totalDeductionAmount`: 费用减项总和
    - `totalPayableAmount`: 应付工资总和
    - `approvedMonths`: 已审批通过的月份数

#### 1.3 Service 层 (SalarySlipService.java)
- 新增方法：`getYearTotal(Long projectId, String year)`
- 功能：调用 Mapper 获取年度汇总数据

#### 1.4 Controller 层 (SalarySlipController.java)
- 新增接口：`GET /salary/api/year-total`
- 参数：
  - `projectId`: 项目 ID
  - `year`: 年度（如："2026"）
- 返回：JSON 格式，包含 success 标志和汇总数据

### 2. 前端修改

#### 2.1 页面按钮 (templates/salary/list.html)
- 在顶部工具栏添加"年度汇总"按钮
- 位置：工资条状态标签左侧
- 样式：蓝色信息按钮，带图表图标

#### 2.2 弹窗组件
- 新增年度汇总弹窗 (`yearTotalModal`)
- 包含内容：
  - 年度选择器（最近 5 年）
  - 提示信息：仅统计已审批通过（状态为锁定）的月份数据
  - 汇总表格：
    - 已统计月份数
    - 考勤工资总和
    - 费用加项总和
    - 费用减项总和
    - 应付工资总和
  - 空数据提示

#### 2.3 JavaScript 函数
- `showYearTotal()`: 显示年度汇总弹窗，初始化年份选项
- `loadYearTotal()`: 加载并渲染年度汇总数据
- 数据格式化：使用现有的 `fmt()` 函数格式化金额显示

## 关键特性

### 数据统计规则
1. **仅统计已审批通过的月份**：只有工资条月份状态为"已锁定"（status=5）的数据才会被统计
2. **按项目隔离**：只统计当前选中项目的数据
3. **按年度筛选**：根据选择的年度筛选对应年份的数据

### UI 交互
1. **年度选择**：默认显示当前年份，提供最近 5 年可选
2. **实时刷新**：切换年度时自动重新加载汇总数据
3. **空数据处理**：无数据时显示友好的空状态提示
4. **颜色区分**：
   - 考勤工资：蓝色
   - 费用加项：绿色
   - 费用减项：红色
   - 应付工资：主色调高亮

## API 接口文档

### 获取年度汇总数据
**请求**
```
GET /salary/api/year-total?projectId={projectId}&year={year}
```

**响应示例**
```json
{
  "success": true,
  "data": {
    "totalBaseAmount": 100000.00,
    "totalAdditionAmount": 5000.00,
    "totalDeductionAmount": 2000.00,
    "totalPayableAmount": 103000.00,
    "approvedMonths": 3
  }
}
```

## 测试要点

### 功能测试
1. ✅ 点击"年度汇总"按钮能正常打开弹窗
2. ✅ 年度选择器显示最近 5 年选项
3. ✅ 切换年度时自动刷新汇总数据
4. ✅ 汇总数据计算准确（仅统计已审批通过的月份）
5. ✅ 无数据时显示空状态提示

### 数据验证
1. ✅ 考勤工资总和 = 所有已审批月份的 base_amount 之和
2. ✅ 费用加项总和 = 所有已审批月份的 addition_amount 之和
3. ✅ 费用减项总和 = 所有已审批月份的 deduction_amount 之和
4. ✅ 应付工资总和 = 所有已审批月份的 payable_amount 之和
5. ✅ 已统计月份数 = 已审批通过的月份数量

## 文件清单

### 修改的文件
1. `/src/main/java/com/example/hello/mapper/SalarySlipMapper.java`
2. `/src/main/resources/mapper/SalarySlipMapper.xml`
3. `/src/main/java/com/example/hello/service/SalarySlipService.java`
4. `/src/main/java/com/example/hello/controller/SalarySlipController.java`
5. `/src/main/resources/templates/salary/list.html`

### Bug 修复
- **问题**：点击年度汇总按钮报 SQL 异常
- **原因**：MyBatis XML 中 `resultType` 使用了 `java.util.HashMap`，应该使用 `java.util.Map`
- **修复**：将 `resultType="java.util.HashMap"` 改为 `resultType="java.util.Map"`
- **文件**：`/src/main/resources/mapper/SalarySlipMapper.xml` 第 128 行

## 使用说明

1. 进入工资条管理页面
2. 选择要查看的项目
3. 点击右上角"年度汇总"按钮
4. 在弹窗中选择要查看的年度
5. 查看该年度已审批通过的工资条汇总数据

## 注意事项

1. 汇总数据仅包含状态为"已锁定"（已审批通过）的月份
2. 草稿状态或驳回状态的月份不会被统计
3. 数据按项目隔离，只显示当前项目的汇总
4. 金额保留两位小数显示

## 下一步建议

1. 可考虑增加导出 Excel 功能
2. 可考虑增加月度趋势图表
3. 可考虑增加员工个人年度汇总查询
