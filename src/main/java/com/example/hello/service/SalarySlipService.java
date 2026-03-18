package com.example.hello.service;

import com.example.hello.entity.Employee;
import com.example.hello.entity.ProjectEmployeeSalary;
import com.example.hello.entity.SalaryItem;
import com.example.hello.entity.SalaryMonthStatus;
import com.example.hello.entity.SalarySlip;
import com.example.hello.entity.SalarySlipChangeLog;
import com.example.hello.mapper.SalaryMonthStatusMapper;
import com.example.hello.mapper.EmployeeMapper;
import com.example.hello.mapper.SalaryItemMapper;
import com.example.hello.mapper.SalarySlipMapper;
import com.example.hello.mapper.SalarySlipChangeLogMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * 工资条服务
 */
@Service
public class SalarySlipService {

    private static final Logger log = LoggerFactory.getLogger(SalarySlipService.class);

    @Autowired private SalarySlipMapper salarySlipMapper;
    @Autowired private SalaryItemMapper  salaryItemMapper;
    @Autowired private AttendanceService attendanceService;
    @Autowired private EmployeeMapper    employeeMapper;
    @Autowired private SalaryMonthStatusMapper salaryMonthStatusMapper;
    @Autowired private ProjectEmployeeSalaryService projectEmployeeSalaryService;
    @Autowired private SalarySlipChangeLogMapper changeLogMapper;

    // ===== 工资条 CRUD =====

    /**
     * 考勤审批通过后，批量为项目内所有员工自动创建或更新工资条
     * 规则：
     * 1. 员工没有工资条时，创建工资条
     * 2. 员工有工资条时，比对考勤天数，不一致则更新，一致则跳过
     */
    @Transactional
    public void batchCreateForProject(Long projectId, String salaryPeriod) {
        List<Employee> employees = employeeMapper.findByProjectId(projectId);
        log.info("[SalaryService] batchCreateForProject: projectId={}, period={}, employees={}", projectId, salaryPeriod, employees.size());
        
        YearMonth ym = YearMonth.parse(salaryPeriod);
        
        for (Employee emp : employees) {
            // 计算当前考勤表中的出勤天数
            BigDecimal currentAttendanceDays = attendanceService.calcAttendanceDaysWithConfig(
                    emp.getId(), ym, null, projectId);
            
            SalarySlip existing = salarySlipMapper.selectByEmployeeAndPeriod(emp.getId(), salaryPeriod, projectId);
            
            if (existing != null) {
                // 已存在工资条，比对考勤天数
                BigDecimal existingDays = existing.getAttendanceDays() != null ? existing.getAttendanceDays() : BigDecimal.ZERO;
                
                // 获取员工当前薪资（支持调薪）
                Integer currentSalaryType = projectEmployeeSalaryService.getSalaryTypeForPeriod(projectId, emp, ym);
                BigDecimal currentSalary = projectEmployeeSalaryService.calculateSalaryForPeriod(projectId, emp, ym);
                
                // 检查考勤天数或薪资是否有变化
                boolean daysChanged = currentAttendanceDays.compareTo(existingDays) != 0;
                boolean salaryChanged = currentSalary.compareTo(existing.getBaseSalary() != null ? existing.getBaseSalary() : BigDecimal.ZERO) != 0;
                
                if (!daysChanged && !salaryChanged) {
                    // 考勤天数和薪资都一致，无需更新
                    log.info("[SalaryService] Skip update for employee {} {}, attendance days and salary unchanged", 
                            emp.getId(), emp.getName());
                    continue;
                }
                
                // 考勤天数或薪资有变化，更新工资条
                log.info("[SalaryService] Update slip for employee {} {}, daysChanged={}, salaryChanged: {} -> {}", 
                        emp.getId(), emp.getName(), daysChanged, existing.getBaseSalary(), currentSalary);
                
                if (existing.getProjectId() == null) {
                    existing.setProjectId(projectId);
                }
                
                // 同步更新员工信息（身份证号、手机号、工种）
                existing.setIdCard(emp.getIdCard());
                existing.setPhone(emp.getPhone());
                existing.setJobCategoryName(emp.getJobCategoryName());
                
                // 更新薪资类型和薪资
                existing.setSalaryType(currentSalaryType);
                existing.setBaseSalary(currentSalary);
                
                // 重新计算出勤天数和考勤工资（recalcBaseAmount 会设置 attendanceDays）
                recalcBaseAmount(existing);
                existing.setAdditionAmount(existing.getAdditionAmount() != null ? existing.getAdditionAmount() : BigDecimal.ZERO);
                existing.setDeductionAmount(existing.getDeductionAmount() != null ? existing.getDeductionAmount() : BigDecimal.ZERO);
                existing.calculatePayable();
                salarySlipMapper.update(existing);
                continue;
            }

            // 没有工资条，创建新工资条
            log.info("[SalaryService] Creating slip for employee {} {}, attendanceDays={}", 
                    emp.getId(), emp.getName(), currentAttendanceDays);
            
            SalarySlip slip = new SalarySlip();
            slip.setEmployeeId(emp.getId());
            slip.setProjectId(projectId);
            slip.setSalaryPeriod(salaryPeriod);
            slip.setIdCard(emp.getIdCard());
            slip.setPhone(emp.getPhone());
            slip.setJobCategoryName(emp.getJobCategoryName());
            
            // 使用项目薪资历史（支持调薪）
            Integer salaryType = projectEmployeeSalaryService.getSalaryTypeForPeriod(projectId, emp, ym);
            BigDecimal salaryAmount = projectEmployeeSalaryService.calculateSalaryForPeriod(projectId, emp, ym);
            
            slip.setSalaryType(salaryType);
            slip.setBaseSalary(salaryAmount);
            slip.setAdditionAmount(BigDecimal.ZERO);
            slip.setDeductionAmount(BigDecimal.ZERO);
            recalcBaseAmount(slip);
            slip.calculatePayable();
            salarySlipMapper.insert(slip);
        }
    }

    /**
     * 创建工资条：自动从员工表带入身份证、工种、薪资类型、薪资
     */
    @Transactional
    public void createSalarySlip(SalarySlip slip) {
        // 同项目同月份不允许重复
        SalarySlip existing = salarySlipMapper.selectByEmployeeAndPeriod(
                slip.getEmployeeId(), slip.getSalaryPeriod(), slip.getProjectId());
        if (existing != null) {
            throw new RuntimeException("该员工在 " + slip.getSalaryPeriod() + " 已有工资条");
        }

        // 从员工表自动带入信息
        Employee emp = employeeMapper.findById(slip.getEmployeeId());
        if (emp != null) {
            if (slip.getIdCard() == null)          slip.setIdCard(emp.getIdCard());
            if (slip.getPhone() == null)           slip.setPhone(emp.getPhone());
            if (slip.getJobCategoryName() == null) slip.setJobCategoryName(emp.getJobCategoryName());
            
            // 使用项目薪资历史（支持调薪）
            if (slip.getSalaryType() == null || slip.getBaseSalary() == null) {
                YearMonth ym = YearMonth.parse(slip.getSalaryPeriod());
                if (slip.getSalaryType() == null) {
                    slip.setSalaryType(projectEmployeeSalaryService.getSalaryTypeForPeriod(
                        slip.getProjectId(), emp, ym));
                }
                if (slip.getBaseSalary() == null) {
                    slip.setBaseSalary(projectEmployeeSalaryService.calculateSalaryForPeriod(
                        slip.getProjectId(), emp, ym));
                }
            }
        }

        // 计算出勤天数与汇总工资
        recalcBaseAmount(slip);

        // 初始化费用项
        slip.setAdditionAmount(BigDecimal.ZERO);
        slip.setDeductionAmount(BigDecimal.ZERO);
        slip.calculatePayable();

        salarySlipMapper.insert(slip);
    }

    /**
     * 按项目和工资期删除工资条（用于考勤驳回时清理）
     */
    @Transactional
    public void deleteByProjectAndPeriod(Long projectId, String salaryPeriod) {
        int count = salarySlipMapper.deleteByProjectAndPeriod(projectId, salaryPeriod);
        log.info("[SalaryService] 删除工资条: projectId={}, period={}, 删除数量={}", projectId, salaryPeriod, count);
    }

    /**
     * 更新工资条基础信息（工资条本身无状态，由月份状态表控制是否可编辑）
     */
    @Transactional
    public void updateSalarySlip(SalarySlip slip) {
        SalarySlip existing = salarySlipMapper.selectById(slip.getId());
        if (existing == null) throw new RuntimeException("工资条不存在");

        // 重新计算出勤天数与汇总工资
        recalcBaseAmount(slip);

        // 保留原费用项金额
        slip.setAdditionAmount(existing.getAdditionAmount());
        slip.setDeductionAmount(existing.getDeductionAmount());
        slip.calculatePayable();

        salarySlipMapper.update(slip);
    }

    /**
     * 删除工资条
     */
    @Transactional
    public void deleteSalarySlip(Long id) {
        SalarySlip slip = salarySlipMapper.selectById(id);
        if (slip == null) throw new RuntimeException("工资条不存在");
        salaryItemMapper.deleteBySalarySlipId(id);
        salarySlipMapper.deleteById(id);
    }

    /**
     * 根据员工ID和薪资期间查询工资条（含费用项）
     */
    public SalarySlip getSalarySlipByEmployeeAndPeriod(Long employeeId, String salaryPeriod) {
        SalarySlip slip = salarySlipMapper.selectByEmployeeAndPeriod(employeeId, salaryPeriod, null);
        if (slip != null) {
            slip.setItems(salaryItemMapper.selectBySalarySlipId(slip.getId()));
        }
        return slip;
    }

    /**
     * 根据ID查询工资条（含费用项）
     */
    public SalarySlip getSalarySlipById(Long id) {
        SalarySlip slip = salarySlipMapper.selectById(id);
        if (slip != null) {
            slip.setItems(salaryItemMapper.selectBySalarySlipId(id));
        }
        return slip;
    }

    /**
     * 查询工资条列表
     */
    public List<SalarySlip> listSalarySlips(Long employeeId, String salaryPeriod,
                                             Integer status, Long projectId) {
        List<SalarySlip> slips = salarySlipMapper.selectList(employeeId, salaryPeriod, projectId);
        
        // 为每个工资条加载费用项明细
        for (SalarySlip slip : slips) {
            if (slip.getId() != null) {
                List<SalaryItem> items = salaryItemMapper.selectBySalarySlipId(slip.getId());
                slip.setItems(items);
                log.debug("[SalaryService] 加载工资条费用项: slipId={}, items={}", slip.getId(), items.size());
                
                // 根据实际费用项重新计算 additionAmount 和 deductionAmount
                BigDecimal additionSum = BigDecimal.ZERO;
                BigDecimal deductionSum = BigDecimal.ZERO;
                for (SalaryItem item : items) {
                    if (item.getItemType() != null && item.getAmount() != null) {
                        if (item.getItemType() == 1) {
                            additionSum = additionSum.add(item.getAmount());
                        } else if (item.getItemType() == 2) {
                            deductionSum = deductionSum.add(item.getAmount());
                        }
                    }
                }
                
                // 如果计算值与数据库值不一致，更新数据库
                boolean needUpdateAmount = false;
                if (additionSum.compareTo(slip.getAdditionAmount() != null ? slip.getAdditionAmount() : BigDecimal.ZERO) != 0) {
                    log.info("[SalaryService] 修正工资条费用加项: slipId={}, {} -> {}", 
                            slip.getId(), slip.getAdditionAmount(), additionSum);
                    slip.setAdditionAmount(additionSum);
                    needUpdateAmount = true;
                }
                if (deductionSum.compareTo(slip.getDeductionAmount() != null ? slip.getDeductionAmount() : BigDecimal.ZERO) != 0) {
                    log.info("[SalaryService] 修正工资条费用减项: slipId={}, {} -> {}", 
                            slip.getId(), slip.getDeductionAmount(), deductionSum);
                    slip.setDeductionAmount(deductionSum);
                    needUpdateAmount = true;
                }
                
                // 重新计算应付工资
                if (needUpdateAmount) {
                    BigDecimal baseAmount = slip.getBaseAmount() != null ? slip.getBaseAmount() : BigDecimal.ZERO;
                    BigDecimal newPayable = baseAmount.add(additionSum);
                    slip.setPayableAmount(newPayable);
                    salarySlipMapper.update(slip);
                    log.info("[SalaryService] 工资条费用修正完成: slipId={}, addition={}, deduction={}, payable={}", 
                            slip.getId(), additionSum, deductionSum, newPayable);
                }
            }
        }
        
        // 同步员工薪资并重新计算应付工资
        for (SalarySlip slip : slips) {
            boolean needUpdate = false;
            
            // 查询项目员工关联表中的当前生效薪资
            YearMonth ym = YearMonth.parse(salaryPeriod);
            ProjectEmployeeSalary projectSalary = projectEmployeeSalaryService.getEffectiveSalary(
                    projectId, slip.getEmployeeId(), ym);
            
            if (projectSalary != null) {
                // 检查薪资类型是否一致
                if (projectSalary.getSalaryType() != null && 
                    !projectSalary.getSalaryType().equals(slip.getSalaryType())) {
                    log.info("[SalaryService] 同步项目员工薪资类型: slipId={}, employeeId={}, {} -> {}", 
                            slip.getId(), slip.getEmployeeId(), slip.getSalaryType(), projectSalary.getSalaryType());
                    slip.setSalaryType(projectSalary.getSalaryType());
                    needUpdate = true;
                }
                
                // 检查薪资金额是否一致
                if (projectSalary.getSalaryAmount() != null && 
                    (slip.getBaseSalary() == null || 
                     projectSalary.getSalaryAmount().compareTo(slip.getBaseSalary()) != 0)) {
                    log.info("[SalaryService] 同步项目员工薪资金额: slipId={}, employeeId={}, {} -> {}", 
                            slip.getId(), slip.getEmployeeId(), slip.getBaseSalary(), projectSalary.getSalaryAmount());
                    slip.setBaseSalary(projectSalary.getSalaryAmount());
                    needUpdate = true;
                }
                
                // 如果薪资有变化，重新计算基础工资和应付工资
                if (needUpdate) {
                    // 重新计算基础工资
                    BigDecimal baseAmount = calculateBaseAmount(
                            slip.getBaseSalary(), slip.getSalaryType(), slip.getAttendanceDays());
                    slip.setBaseAmount(baseAmount);
                    
                    // 重新计算应付工资
                    BigDecimal correctPayable = baseAmount.add(
                            slip.getAdditionAmount() != null ? slip.getAdditionAmount() : BigDecimal.ZERO);
                    slip.setPayableAmount(correctPayable);
                    
                    // 更新数据库
                    salarySlipMapper.update(slip);
                    log.info("[SalaryService] 工资条薪资已同步并重新计算: slipId={}, baseAmount={}, payable={}", 
                            slip.getId(), baseAmount, correctPayable);
                }
            }
            
            // 强制重新计算应付工资并修复数据（原有的逻辑保留）
            BigDecimal base = slip.getBaseAmount() != null ? slip.getBaseAmount() : BigDecimal.ZERO;
            BigDecimal add = slip.getAdditionAmount() != null ? slip.getAdditionAmount() : BigDecimal.ZERO;
            BigDecimal correctPayable = base.add(add);
            
            if (slip.getPayableAmount() == null || correctPayable.compareTo(slip.getPayableAmount()) != 0) {
                log.info("[SalaryService] 修复工资条应付工资: slipId={}, {} -> {}", 
                        slip.getId(), slip.getPayableAmount(), correctPayable);
                slip.setPayableAmount(correctPayable);
                salarySlipMapper.update(slip);
            }
        }
        
        return slips;
    }
    
    /**
     * 计算基础工资
     */
    private BigDecimal calculateBaseAmount(BigDecimal baseSalary, Integer salaryType, BigDecimal attendanceDays) {
        if (baseSalary == null) return BigDecimal.ZERO;
        if (attendanceDays == null) attendanceDays = BigDecimal.ZERO;
        
        if (salaryType != null && salaryType == 2) {
            // 月薪 = 固定金额
            return baseSalary;
        } else {
            // 日薪 = 日薪 * 出勤天数
            return baseSalary.multiply(attendanceDays);
        }
    }

    /**
     * 检查工资条所属月份是否已锁定（审批通过 status=5），按项目维度隔离
     */
    private void checkNotLocked(Long salarySlipId) {
        SalarySlip slip = salarySlipMapper.selectById(salarySlipId);
        if (slip == null) throw new RuntimeException("工资条不存在");
        int approvedCount = salaryMonthStatusMapper.countApprovedByYearMonth(slip.getSalaryPeriod(), slip.getProjectId());
        if (approvedCount > 0) {
            throw new RuntimeException("工资条已审批锁定，不允许修改费用明细");
        }
    }

    /**
     * 更新费用项金额
     */
    @Transactional
    public void updateSalaryItemAmount(Long itemId, BigDecimal newAmount, String modifier) {
        SalaryItem item = salaryItemMapper.selectById(itemId);
        if (item == null) throw new RuntimeException("费用项不存在");
        checkNotLocked(item.getSalarySlipId());
        item.setAmount(newAmount);
        item.setModifier(modifier);
        salaryItemMapper.update(item);
        recalculateSalarySlip(item.getSalarySlipId());
    }

    /**
     * 添加费用项（工资条本身无状态，由月份状态表控制是否可编辑）
     */
    @Transactional
    public void addSalaryItem(SalaryItem item) {
        SalarySlip slip = salarySlipMapper.selectById(item.getSalarySlipId());
        if (slip == null) throw new RuntimeException("工资条不存在");
        checkNotLocked(item.getSalarySlipId());
        salaryItemMapper.insert(item);
        recalculateSalarySlip(item.getSalarySlipId());
    }

    /**
     * 删除费用项
     */
    @Transactional
    public void deleteSalaryItem(Long itemId) {
        SalaryItem existing = salaryItemMapper.selectById(itemId);
        if (existing == null) throw new RuntimeException("费用项不存在");
        checkNotLocked(existing.getSalarySlipId());
        salaryItemMapper.deleteById(itemId);
        recalculateSalarySlip(existing.getSalarySlipId());
    }

    // ===== 审批流程 =====

    // 工资条本身无状态，由月份状态表控制，以下方法已废弃
    // submitForApproval 和 approveSalarySlip 逻辑已移至 SalarySlipController 操作月份状态表

    // ===== 私有辅助 =====

    /**
     * 根据薪资类型计算出勤天数和汇总工资
     */
    private void recalcBaseAmount(SalarySlip slip) {
        if (slip.getSalaryType() != null && slip.getSalaryType() == 1) {
            // 日薪：按工时配置计算出勤天数，同时将中间计算明细填充到 slip
            YearMonth ym = YearMonth.parse(slip.getSalaryPeriod());
            BigDecimal daysDecimal = attendanceService.calcAttendanceDaysWithConfig(slip.getEmployeeId(), ym, slip);
            // 保留1位小数
            daysDecimal = daysDecimal.setScale(1, java.math.RoundingMode.HALF_UP);
            slip.setAttendanceDays(daysDecimal);
            BigDecimal base = slip.getBaseSalary() != null ? slip.getBaseSalary() : BigDecimal.ZERO;
            // 汇总工资保留2位小数
            slip.setBaseAmount(base.multiply(daysDecimal).setScale(2, java.math.RoundingMode.HALF_UP));
        } else {
            // 月薪：汇总工资 = 薪资
            slip.setAttendanceDays(BigDecimal.ZERO);
            slip.setBaseAmount(slip.getBaseSalary() != null ? slip.getBaseSalary() : BigDecimal.ZERO);
        }
    }

    /**
     * 重新汇总费用项金额并更新工资条
     */
    private void recalculateSalarySlip(Long salarySlipId) {
        BigDecimal addition  = salaryItemMapper.sumAdditionBySalarySlipId(salarySlipId);
        BigDecimal deduction = salaryItemMapper.sumDeductionBySalarySlipId(salarySlipId);
        SalarySlip slip = salarySlipMapper.selectById(salarySlipId);
        BigDecimal base = slip.getBaseAmount() != null ? slip.getBaseAmount() : BigDecimal.ZERO;
        // 应付工资 = 考勤工资 + 费用加项（不包含费用减项）
        BigDecimal payable = base.add(addition);
        salarySlipMapper.updateAmounts(salarySlipId, addition, deduction, payable);
    }

    /**
     * 统计某项目某年度的汇总工资条数据（仅统计已审批通过的月份）
     */
    public java.util.Map<String, Object> getYearTotal(Long projectId, String year) {
        return salarySlipMapper.sumYearTotal(projectId, year);
    }

    /**
     * 按员工聚合获取年度工资条汇总（仅已审批通过的月份）
     */
    public java.util.List<java.util.Map<String, Object>> getYearByEmployee(Long projectId, String year) {
        return salarySlipMapper.sumYearByEmployee(projectId, year);
    }

    /**
     * 查询某员工在某项目某年度已审批通过的工资条列表（含费用项）
     */
    public java.util.List<SalarySlip> getYearSlipsByEmployee(Long projectId, Long employeeId, String year) {
        java.util.List<SalarySlip> slips = salarySlipMapper.selectYearSlipsByEmployee(projectId, employeeId, year);
        for (SalarySlip slip : slips) {
            slip.setItems(salaryItemMapper.selectBySalarySlipId(slip.getId()));
            // 日薪员工：填充工时瞬态字段，供前端展示计算说明
            if (slip.getSalaryType() != null && slip.getSalaryType() == 1
                    && slip.getEmployeeId() != null && slip.getSalaryPeriod() != null) {
                try {
                    java.time.YearMonth ym = java.time.YearMonth.parse(slip.getSalaryPeriod());
                    attendanceService.calcAttendanceDaysWithConfig(slip.getEmployeeId(), ym, slip);
                } catch (Exception e) {
                    log.warn("[getYearSlipsByEmployee] fill transient fields failed for slip {}: {}", slip.getId(), e.getMessage());
                }
            }
        }
        return slips;
    }
    
    // ===== 费用项变更相关方法 =====
    
    /**
     * 审批通过费用项变更
     * 应用费用项变更到工资条，然后删除变更记录
     */
    @Transactional
    public void approveFeeChangeLog(Long changeLogId, Long approverId, String approverName, String remark) {
        SalarySlipChangeLog changeLog = changeLogMapper.selectById(changeLogId);
        if (changeLog == null) throw new RuntimeException("修改记录不存在");
        
        // 应用费用项变更
        applyFeeItemsChange(changeLog);
        
        // 更新工资条费用汇总
        SalarySlip slip = salarySlipMapper.selectById(changeLog.getSalarySlipId());
        if (slip == null) throw new RuntimeException("工资条不存在");
        
        slip.setAdditionAmount(changeLog.getNewAdditionAmount());
        slip.setDeductionAmount(changeLog.getNewDeductionAmount());
        slip.setPayableAmount(changeLog.getNewPayableAmount());
        
        salarySlipMapper.update(slip);
        
        // 删除变更记录（审批通过后不再需要保留）
        changeLogMapper.deleteById(changeLogId);
        
        log.info("[SalaryService] 审批通过费用项变更: changeId={}, slipId={}, 审批人={}", 
                changeLogId, changeLog.getSalarySlipId(), approverName);
    }
    
    /**
     * 应用费用项变更到数据库
     */
    public void applyFeeItemsChange(SalarySlipChangeLog changeLog) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> changes = mapper.readValue(changeLog.getFeeItemsChangeDetail(), 
                    new TypeReference<List<Map<String, Object>>>() {});
            
            for (Map<String, Object> change : changes) {
                String changeType = (String) change.get("changeType");
                
                switch (changeType) {
                    case "ADD":
                        // 新增费用项
                        SalaryItem newItem = new SalaryItem();
                        newItem.setSalarySlipId(changeLog.getSalarySlipId());
                        newItem.setItemName((String) change.get("itemName"));
                        // ADD操作使用 newAmount 字段（与日志记录时一致）
                        Object amountObj = change.get("newAmount");
                        if (amountObj == null) {
                            log.warn("[SalaryService] ADD操作缺少newAmount字段，跳过: {}", change);
                            break;
                        }
                        newItem.setAmount(new BigDecimal(amountObj.toString()));
                        newItem.setItemType((Integer) change.get("itemType"));
                        salaryItemMapper.insert(newItem);
                        break;
                        
                    case "EDIT":
                        // 修改费用项金额
                        Object itemIdObj = change.get("itemId");
                        Object newAmountObj = change.get("newAmount");
                        if (itemIdObj == null || newAmountObj == null) {
                            log.warn("[SalaryService] EDIT操作缺少必要字段，跳过: {}", change);
                            break;
                        }
                        Long itemId = Long.valueOf(itemIdObj.toString());
                        BigDecimal newAmount = new BigDecimal(newAmountObj.toString());
                        salaryItemMapper.updateAmount(itemId, newAmount);
                        break;
                        
                    case "DELETE":
                        // 删除费用项
                        Object delItemIdObj = change.get("itemId");
                        if (delItemIdObj == null) {
                            log.warn("[SalaryService] DELETE操作缺少itemId字段，跳过: {}", change);
                            break;
                        }
                        Long delItemId = Long.valueOf(delItemIdObj.toString());
                        salaryItemMapper.deleteById(delItemId);
                        break;
                }
            }
            
            log.info("[SalaryService] 应用费用项变更完成: changeId={}, 变更数={}", 
                    changeLog.getId(), changes.size());
        } catch (Exception e) {
            log.error("[SalaryService] 应用费用项变更失败", e);
            throw new RuntimeException("应用费用项变更失败: " + e.getMessage());
        }
    }
    
    /**
     * 驳回费用项变更
     * 直接删除变更记录
     */
    @Transactional
    public void rejectFeeChangeLog(Long changeLogId, Long approverId, String approverName, String remark) {
        SalarySlipChangeLog changeLog = changeLogMapper.selectById(changeLogId);
        if (changeLog == null) throw new RuntimeException("修改记录不存在");
        
        // 删除变更记录（驳回后不再需要保留）
        changeLogMapper.deleteById(changeLogId);
        
        log.info("[SalaryService] 驳回费用项变更: changeId={}, slipId={}, 审批人={}", 
                changeLogId, changeLog.getSalarySlipId(), approverName);
    }
    
    /**
     * 批量审批费用项变更（按项目和月份）
     * 应用变更后删除变更记录
     */
    @Transactional
    public void batchApproveFeeChanges(Long projectId, String salaryPeriod, Long approverId, String approverName, String remark) {
        // 查询所有变更记录
        List<SalarySlipChangeLog> pendingLogs = changeLogMapper.selectByProjectAndPeriod(projectId, salaryPeriod);
        
        for (SalarySlipChangeLog changeLog : pendingLogs) {
            // 应用费用项变更
            applyFeeItemsChange(changeLog);
            
            // 更新工资条费用汇总
            SalarySlip slip = salarySlipMapper.selectById(changeLog.getSalarySlipId());
            if (slip != null) {
                slip.setAdditionAmount(changeLog.getNewAdditionAmount());
                slip.setDeductionAmount(changeLog.getNewDeductionAmount());
                slip.setPayableAmount(changeLog.getNewPayableAmount());
                salarySlipMapper.update(slip);
            }
            
            // 删除变更记录
            changeLogMapper.deleteById(changeLog.getId());
        }
        
        log.info("[SalaryService] 批量审批费用项变更: projectId={}, period={}, 变更数量={}, 审批人={}", 
                projectId, salaryPeriod, pendingLogs.size(), approverName);
    }
    
    /**
     * 批量驳回费用项变更（按项目和月份）
     * 直接删除变更记录
     */
    @Transactional
    public void batchRejectFeeChanges(Long projectId, String salaryPeriod, Long approverId, String approverName, String remark) {
        // 删除该项目的所有变更记录
        int deletedCount = changeLogMapper.deleteByProjectAndPeriod(projectId, salaryPeriod);
        
        log.info("[SalaryService] 批量驳回费用项变更: projectId={}, period={}, 删除日志数={}, 审批人={}", 
                projectId, salaryPeriod, deletedCount, approverName);
    }
    
    /**
     * 检查工资条是否需要重新计算（考勤数据有差异）
     */
    public boolean needRecalc(Long salarySlipId) {
        SalarySlip slip = salarySlipMapper.selectById(salarySlipId);
        if (slip == null) return false;
        
        // 根据最新考勤计算出勤天数
        YearMonth ym = YearMonth.parse(slip.getSalaryPeriod());
        BigDecimal currentDays = attendanceService.calcAttendanceDaysWithConfig(
                slip.getEmployeeId(), ym, null);
        
        // 对比现有出勤天数
        BigDecimal slipDays = slip.getAttendanceDays() != null ? slip.getAttendanceDays() : BigDecimal.ZERO;
        
        return currentDays.compareTo(slipDays) != 0;
    }
    
    /**
     * 获取考勤与工资条的差异详情
     */
    public java.util.Map<String, Object> getAttendanceDiff(Long salarySlipId) {
        java.util.Map<String, Object> diff = new java.util.HashMap<>();
        
        SalarySlip slip = salarySlipMapper.selectById(salarySlipId);
        if (slip == null) {
            diff.put("hasDiff", false);
            return diff;
        }
        
        // 获取最新考勤数据
        YearMonth ym = YearMonth.parse(slip.getSalaryPeriod());
        BigDecimal currentDays = attendanceService.calcAttendanceDaysWithConfig(
                slip.getEmployeeId(), ym, null);
        
        BigDecimal slipDays = slip.getAttendanceDays() != null ? slip.getAttendanceDays() : BigDecimal.ZERO;
        
        diff.put("hasDiff", currentDays.compareTo(slipDays) != 0);
        diff.put("salarySlipId", salarySlipId);
        diff.put("employeeId", slip.getEmployeeId());
        diff.put("salaryPeriod", slip.getSalaryPeriod());
        diff.put("currentAttendanceDays", currentDays);
        diff.put("slipAttendanceDays", slipDays);
        diff.put("diffDays", currentDays.subtract(slipDays));
        
        return diff;
    }
    
    /**
     * 查询工资条的待审批修改记录
     */
    public SalarySlipChangeLog getPendingChangeLog(Long salarySlipId) {
        return changeLogMapper.selectBySalarySlipId(salarySlipId);
    }
    
    /**
     * 查询项目和月份的所有修改记录
     */
    public List<SalarySlipChangeLog> getChangeLogsByProjectAndPeriod(Long projectId, String salaryPeriod) {
        return changeLogMapper.selectByProjectAndPeriod(projectId, salaryPeriod);
    }
    
    /**
     * 检查是否存在待审批的修改记录
     */
    public boolean hasPendingChanges(Long projectId, String salaryPeriod) {
        return changeLogMapper.countByProjectAndPeriod(projectId, salaryPeriod) > 0;
    }
    
    // ===== 费用项变更相关方法 =====
    
    /**
     * 检查工资条是否有待审批的费用项变更
     */
    public boolean hasPendingFeeChange(Long salarySlipId) {
        return changeLogMapper.countBySalarySlipId(salarySlipId) > 0;
    }
    
    /**
     * 记录费用项变更（实时抵消计算）
     * 抵消规则：
     * 1. 新增后删除 = 抵消，不记录
     * 2. 编辑后改回原值 = 抵消，删除原记录
     * 3. 新增后修改 = 保留ADD（改后金额）
     * 4. 删除既有费用项 = 记录DELETE
     * 5. 编辑既有费用项 = 对比原值，不同则记录EDIT
     * 
     * @param salarySlipId 工资条ID
     * @param itemId 费用项ID
     * @param itemName 费用项名称
     * @param itemType 费用项类型（1=加项，2=减项）
     * @param operation 操作类型：ADD/EDIT/DELETE
     * @param oldAmount 原金额
     * @param newAmount 新金额
     * @param changeReason 变更原因
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @return 是否成功记录（false表示被抵消）
     */
    @Transactional
    public boolean recordFeeItemChange(Long salarySlipId, Long itemId, String itemName, Integer itemType,
                                        String operation, BigDecimal oldAmount, BigDecimal newAmount,
                                        String changeReason, Long operatorId, String operatorName) {
        SalarySlip slip = salarySlipMapper.selectById(salarySlipId);
        if (slip == null) throw new RuntimeException("工资条不存在");
        
        // 查询该费用项的最新变更记录
        SalarySlipChangeLog existingLog = changeLogMapper.selectLatestBySlipAndItem(salarySlipId, itemId);
        
        // 解析现有变更
        java.util.Map<String, Object> existingChange = null;
        if (existingLog != null && existingLog.getFeeItemsChangeDetail() != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                java.util.List<java.util.Map<String, Object>> changes = mapper.readValue(
                        existingLog.getFeeItemsChangeDetail(), 
                        new TypeReference<java.util.List<java.util.Map<String, Object>>>() {});
                if (!changes.isEmpty()) {
                    existingChange = changes.get(0);
                }
            } catch (Exception e) {
                log.error("解析现有变更记录失败", e);
            }
        }
        
        String existingOp = existingChange != null ? (String) existingChange.get("changeType") : null;
        
        // 抵消计算
        if ("ADD".equals(operation)) {
            // 新增费用项
            if ("DELETE".equals(existingOp)) {
                // 之前有DELETE，现在ADD = 变为EDIT（但这种情况不应该出现，DELETE是删除既有项）
                // 直接记录ADD
            }
            // 记录ADD
            return saveFeeItemChange(slip, itemId, itemName, itemType, "ADD", 
                    BigDecimal.ZERO, newAmount, changeReason, operatorId, operatorName);
            
        } else if ("DELETE".equals(operation)) {
            // 删除费用项
            if ("ADD".equals(existingOp)) {
                // 之前有ADD，现在DELETE = 抵消，删除原记录
                changeLogMapper.deleteById(existingLog.getId());
                SalarySlipService.log.info("[SalaryService] 抵消：新增后删除，删除记录 logId={}", existingLog.getId());
                return false; // 被抵消，不记录
            }
            // 记录DELETE
            return saveFeeItemChange(slip, itemId, itemName, itemType, "DELETE", 
                    oldAmount, BigDecimal.ZERO, changeReason, operatorId, operatorName);
            
        } else if ("EDIT".equals(operation)) {
            // 编辑费用项
            if (oldAmount.compareTo(newAmount) == 0) {
                // 金额未变，不记录
                SalarySlipService.log.info("[SalaryService] 抵消：编辑后金额未变，不记录");
                return false;
            }
            
            if ("ADD".equals(existingOp)) {
                // 之前有ADD，现在EDIT = 修改ADD的金额
                // 删除原记录，重新记录ADD（新金额）
                changeLogMapper.deleteById(existingLog.getId());
                BigDecimal originalAddAmount = new BigDecimal(existingChange.get("newAmount").toString());
                return saveFeeItemChange(slip, itemId, itemName, itemType, "ADD", 
                        BigDecimal.ZERO, newAmount, changeReason, operatorId, operatorName);
            }
            
            // 记录EDIT
            return saveFeeItemChange(slip, itemId, itemName, itemType, "EDIT", 
                    oldAmount, newAmount, changeReason, operatorId, operatorName);
        }
        
        return false;
    }
    
    /**
     * 保存费用项变更记录
     */
    private boolean saveFeeItemChange(SalarySlip slip, Long itemId, String itemName, Integer itemType,
                                       String changeType, BigDecimal oldAmount, BigDecimal newAmount,
                                       String changeReason, Long operatorId, String operatorName) {
        // 构建变更详情
        java.util.Map<String, Object> changeDetail = new java.util.HashMap<>();
        changeDetail.put("itemId", itemId);
        changeDetail.put("itemName", itemName);
        changeDetail.put("itemType", itemType);
        changeDetail.put("changeType", changeType);
        changeDetail.put("oldAmount", oldAmount);
        changeDetail.put("newAmount", newAmount);
        
        java.util.List<java.util.Map<String, Object>> changes = new java.util.ArrayList<>();
        changes.add(changeDetail);
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            String changeDetailJson = mapper.writeValueAsString(changes);
            
            // 查询当前工资条的所有费用项，计算新的汇总
            java.util.List<SalaryItem> items = salaryItemMapper.selectBySalarySlipId(slip.getId());
            BigDecimal newAddition = BigDecimal.ZERO;
            BigDecimal newDeduction = BigDecimal.ZERO;
            
            for (SalaryItem item : items) {
                // 应用当前变更
                BigDecimal amount = item.getAmount();
                if (item.getId().equals(itemId)) {
                    if ("DELETE".equals(changeType)) {
                        amount = BigDecimal.ZERO;
                    } else if ("EDIT".equals(changeType) || "ADD".equals(changeType)) {
                        amount = newAmount;
                    }
                }
                
                if (item.getItemType() != null && item.getItemType() == 1) {
                    newAddition = newAddition.add(amount);
                } else if (item.getItemType() != null && item.getItemType() == 2) {
                    newDeduction = newDeduction.add(amount);
                }
            }
            
            // 如果是ADD，需要加上新增的金额
            if ("ADD".equals(changeType)) {
                if (itemType != null && itemType == 1) {
                    newAddition = newAddition.add(newAmount);
                } else if (itemType != null && itemType == 2) {
                    newDeduction = newDeduction.add(newAmount);
                }
            }
            
            BigDecimal baseAmount = slip.getBaseAmount() != null ? slip.getBaseAmount() : BigDecimal.ZERO;
            BigDecimal newPayable = baseAmount.add(newAddition).subtract(newDeduction);
            
            // 创建变更记录
            SalarySlipChangeLog log = new SalarySlipChangeLog();
            log.setSalarySlipId(slip.getId());
            log.setProjectId(slip.getProjectId());
            log.setEmployeeId(slip.getEmployeeId());
            log.setSalaryPeriod(slip.getSalaryPeriod());
            log.setChangeType("FEE_CHANGE");
            log.setFeeItemsChangeDetail(changeDetailJson);
            log.setOldFeeItemsJson(null); // 简化处理
            log.setNewFeeItemsJson(null);
            log.setOldAttendanceDays(slip.getAttendanceDays());
            log.setOldBaseSalary(slip.getBaseSalary());
            log.setOldBaseAmount(slip.getBaseAmount());
            log.setOldAdditionAmount(slip.getAdditionAmount());
            log.setOldDeductionAmount(slip.getDeductionAmount());
            log.setOldPayableAmount(slip.getPayableAmount());
            log.setNewAttendanceDays(slip.getAttendanceDays());
            log.setNewBaseSalary(slip.getBaseSalary());
            log.setNewBaseAmount(slip.getBaseAmount());
            log.setNewAdditionAmount(newAddition);
            log.setNewDeductionAmount(newDeduction);
            log.setNewPayableAmount(newPayable);
            log.setChangeReason(changeReason);
            log.setCreatedBy(operatorId);
            log.setCreatedByName(operatorName);
            log.setCreatedTime(LocalDateTime.now());
            
            changeLogMapper.insert(log);
            
            SalarySlipService.log.info("[SalaryService] 记录费用项变更: slipId={}, itemId={}, 类型={}, 金额={}→{}", 
                    slip.getId(), itemId, changeType, oldAmount, newAmount);
            
            return true;
        } catch (Exception e) {
            SalarySlipService.log.error("保存费用项变更失败", e);
            throw new RuntimeException("保存变更记录失败");
        }
    }
    
    /**
     * 进入编辑模式（将状态改为变更待审 status=2）
     */
    @Transactional
    public void enterEditMode(Long projectId, String salaryPeriod, 
                               Long operatorId, String changeReason) {
        // 先查询是否存在记录
        SalaryMonthStatus existing = salaryMonthStatusMapper.findByYearMonthAndProject(salaryPeriod, projectId);
        
        if (existing != null) {
            // 更新现有记录
            existing.setStatus(SalaryMonthStatus.STATUS_CHANGE_PENDING);
            existing.setSubmitBy(operatorId);
            existing.setSubmitTime(LocalDateTime.now());
            existing.setSubmitRemark(changeReason);
            salaryMonthStatusMapper.update(existing);
        } else {
            // 插入新记录
            SalaryMonthStatus monthStatus = new SalaryMonthStatus();
            monthStatus.setYearMonth(salaryPeriod);
            monthStatus.setProjectId(projectId);
            monthStatus.setStatus(SalaryMonthStatus.STATUS_CHANGE_PENDING);
            monthStatus.setSubmitBy(operatorId);
            monthStatus.setSubmitTime(LocalDateTime.now());
            monthStatus.setSubmitRemark(changeReason);
            salaryMonthStatusMapper.insert(monthStatus);
        }
        
        log.info("[SalaryService] 进入编辑模式: projectId={}, period={}", projectId, salaryPeriod);
    }
    
    /**
     * 提交费用项变更审批（状态变为待审批 status=1）
     */
    @Transactional
    public void submitFeeChangesForApproval(Long projectId, String salaryPeriod, 
                                             Long operatorId, String changeReason) {
        // 更新工资月份状态为待审批（status=1）
        SalaryMonthStatus monthStatus = new SalaryMonthStatus();
        monthStatus.setYearMonth(salaryPeriod);
        monthStatus.setProjectId(projectId);
        monthStatus.setStatus(SalaryMonthStatus.STATUS_PENDING);
        monthStatus.setSubmitBy(operatorId);
        monthStatus.setSubmitTime(LocalDateTime.now());
        monthStatus.setSubmitRemark(changeReason);
        salaryMonthStatusMapper.insertOrUpdate(monthStatus);
        
        log.info("[SalaryService] 提交费用项变更审批: projectId={}, period={}", projectId, salaryPeriod);
    }
    
    /**
     * 取消费用项变更
     * 恢复工资月份状态为已锁定，删除变更日志
     */
    @Transactional
    public void cancelFeeChange(Long projectId, String salaryPeriod, Long operatorId) {
        // 更新工资月份状态为已锁定（status=5）
        SalaryMonthStatus monthStatus = new SalaryMonthStatus();
        monthStatus.setYearMonth(salaryPeriod);
        monthStatus.setProjectId(projectId);
        monthStatus.setStatus(SalaryMonthStatus.STATUS_APPROVED);
        monthStatus.setApproveBy(operatorId);
        monthStatus.setApproveTime(LocalDateTime.now());
        monthStatus.setApproveRemark("用户取消变更");
        salaryMonthStatusMapper.updateStatus(monthStatus);
        
        // 删除该项目的所有变更日志
        int deletedCount = changeLogMapper.deleteByProjectAndPeriod(projectId, salaryPeriod);
        
        log.info("[SalaryService] 取消费用项变更: projectId={}, period={}, 删除日志数={}", 
                projectId, salaryPeriod, deletedCount);
    }
    
    /**
     * 查询工资条的变更记录
     */
    public SalarySlipChangeLog getFeeChangeLogsBySalarySlipId(Long salarySlipId) {
        return changeLogMapper.selectBySalarySlipId(salarySlipId);
    }
    
    /**
     * 查询项目和月份的所有变更记录
     */
    /**
     * 查询项目和月份的费用项变更记录（解析JSON返回扁平化格式）
     * 返回格式便于前端展示：每条记录包含 employeeName, itemName, itemType, operation, oldAmount, newAmount
     */
    public java.util.List<java.util.Map<String, Object>> getFeeChangeLogsByProjectAndPeriod(Long projectId, String salaryPeriod) {
        List<SalarySlipChangeLog> logs = changeLogMapper.selectByProjectAndPeriod(projectId, salaryPeriod);
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        
        for (SalarySlipChangeLog changeLogEntry : logs) {
            if (changeLogEntry.getFeeItemsChangeDetail() == null || changeLogEntry.getFeeItemsChangeDetail().isEmpty()) {
                continue;
            }
            
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<java.util.Map<String, Object>> changes = mapper.readValue(changeLogEntry.getFeeItemsChangeDetail(), 
                        new TypeReference<List<java.util.Map<String, Object>>>() {});
                
                for (java.util.Map<String, Object> change : changes) {
                    java.util.Map<String, Object> record = new java.util.HashMap<>();
                    record.put("employeeName", changeLogEntry.getEmployeeName());
                    record.put("itemName", change.get("itemName"));
                    record.put("itemType", change.get("itemType"));
                    record.put("operation", change.get("changeType"));
                    record.put("oldAmount", change.get("oldAmount"));
                    record.put("newAmount", change.get("newAmount"));
                    record.put("changeReason", changeLogEntry.getChangeReason());
                    record.put("createdTime", changeLogEntry.getCreatedTime());
                    result.add(record);
                }
            } catch (Exception e) {
                SalarySlipService.log.error("解析变更详情失败: {}", changeLogEntry.getFeeItemsChangeDetail(), e);
            }
        }
        
        return result;
    }
    
    /**
     * 查询项目和月份的变更记录（应用抵消算法后的最终状态）
     * 抵消规则：
     * 1. 新增后删除 = 抵消
     * 2. 编辑后改回原值 = 抵消
     * 3. 编辑后改新值 = 保留EDIT
     * 4. 新增后修改 = 保留ADD（改后金额）
     */
    public java.util.List<java.util.Map<String, Object>> getMergedChangeLogs(Long projectId, String salaryPeriod) {
        List<SalarySlipChangeLog> allLogs = changeLogMapper.selectAllByProjectAndPeriod(projectId, salaryPeriod);
        
        // 按工资条ID分组处理
        java.util.Map<Long, java.util.List<SalarySlipChangeLog>> logsBySlip = new java.util.HashMap<>();
        for (SalarySlipChangeLog log : allLogs) {
            logsBySlip.computeIfAbsent(log.getSalarySlipId(), k -> new java.util.ArrayList<>()).add(log);
        }
        
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        
        for (java.util.Map.Entry<Long, java.util.List<SalarySlipChangeLog>> entry : logsBySlip.entrySet()) {
            Long slipId = entry.getKey();
            List<SalarySlipChangeLog> slipLogs = entry.getValue();
            
            // 按费用项ID合并变更
            java.util.Map<String, java.util.Map<String, Object>> itemChanges = new java.util.HashMap<>();
            
            for (SalarySlipChangeLog log : slipLogs) {
                if (log.getFeeItemsChangeDetail() == null || log.getFeeItemsChangeDetail().isEmpty()) {
                    continue;
                }
                
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    List<java.util.Map<String, Object>> changes = mapper.readValue(log.getFeeItemsChangeDetail(), 
                            new TypeReference<List<java.util.Map<String, Object>>>() {});
                    
                    for (java.util.Map<String, Object> change : changes) {
                        String itemId = String.valueOf(change.get("itemId"));
                        String changeType = (String) change.get("changeType");
                        String itemName = (String) change.get("itemName");
                        Integer itemType = (Integer) change.get("itemType");
                        
                        // 获取金额
                        BigDecimal oldAmount = change.get("oldAmount") != null ? 
                                new BigDecimal(change.get("oldAmount").toString()) : BigDecimal.ZERO;
                        BigDecimal newAmount = change.get("newAmount") != null ? 
                                new BigDecimal(change.get("newAmount").toString()) : BigDecimal.ZERO;
                        
                        String key = slipId + "_" + itemId;
                        
                        if ("ADD".equals(changeType)) {
                            // 新增：记录为ADD
                            java.util.Map<String, Object> item = new java.util.HashMap<>();
                            item.put("slipId", slipId);
                            item.put("itemId", itemId);
                            item.put("itemName", itemName);
                            item.put("itemType", itemType);
                            item.put("changeType", "ADD");
                            item.put("finalAmount", newAmount);
                            item.put("employeeName", log.getCreatedByName());
                            item.put("changeReason", log.getChangeReason());
                            item.put("createdTime", log.getCreatedTime());
                            itemChanges.put(key, item);
                        } else if ("DELETE".equals(changeType)) {
                            // 删除：检查是否有ADD记录
                            if (itemChanges.containsKey(key)) {
                                // 有ADD记录，抵消（删除）
                                itemChanges.remove(key);
                            } else {
                                // 没有ADD记录，记录为DELETE
                                java.util.Map<String, Object> item = new java.util.HashMap<>();
                                item.put("slipId", slipId);
                                item.put("itemId", itemId);
                                item.put("itemName", itemName);
                                item.put("itemType", itemType);
                                item.put("changeType", "DELETE");
                                item.put("finalAmount", BigDecimal.ZERO);
                                item.put("originalAmount", oldAmount);
                                item.put("employeeName", log.getCreatedByName());
                                item.put("changeReason", log.getChangeReason());
                                item.put("createdTime", log.getCreatedTime());
                                itemChanges.put(key, item);
                            }
                        } else if ("EDIT".equals(changeType)) {
                            // 编辑：检查当前状态
                            if (itemChanges.containsKey(key)) {
                                // 之前有ADD记录
                                java.util.Map<String, Object> existing = itemChanges.get(key);
                                BigDecimal originalAddAmount = new BigDecimal(existing.get("finalAmount").toString());
                                // 修改ADD的金额
                                existing.put("finalAmount", newAmount);
                            } else {
                                // 之前没有记录，检查是否改回原值
                                // 需要查询原始金额，这里简化处理，记录为EDIT
                                java.util.Map<String, Object> item = new java.util.HashMap<>();
                                item.put("slipId", slipId);
                                item.put("itemId", itemId);
                                item.put("itemName", itemName);
                                item.put("itemType", itemType);
                                item.put("changeType", "EDIT");
                                item.put("finalAmount", newAmount);
                                item.put("originalAmount", oldAmount);
                                item.put("employeeName", log.getCreatedByName());
                                item.put("changeReason", log.getChangeReason());
                                item.put("createdTime", log.getCreatedTime());
                                itemChanges.put(key, item);
                            }
                        }
                    }
                } catch (Exception e) {
                    SalarySlipService.log.error("解析变更详情失败", e);
                }
            }
            
            // 过滤掉EDIT但金额未变的（改回原值）
            for (java.util.Map<String, Object> item : itemChanges.values()) {
                String changeType = (String) item.get("changeType");
                if ("EDIT".equals(changeType)) {
                    BigDecimal finalAmount = new BigDecimal(item.get("finalAmount").toString());
                    BigDecimal originalAmount = item.get("originalAmount") != null ? 
                            new BigDecimal(item.get("originalAmount").toString()) : BigDecimal.ZERO;
                    if (finalAmount.compareTo(originalAmount) == 0) {
                        // 改回原值，抵消
                        continue;
                    }
                }
                result.add(item);
            }
        }
        
        return result;
    }
}
