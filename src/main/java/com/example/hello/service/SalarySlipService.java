package com.example.hello.service;

import com.example.hello.entity.Employee;
import com.example.hello.entity.SalaryItem;
import com.example.hello.entity.SalarySlip;
import com.example.hello.mapper.SalaryMonthStatusMapper;
import com.example.hello.mapper.EmployeeMapper;
import com.example.hello.mapper.SalaryItemMapper;
import com.example.hello.mapper.SalarySlipMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

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

    // ===== 工资条 CRUD =====

    /**
     * 考勤审批通过后，批量为项目内所有员工自动创建工资条（已存在则跳过）
     */
    @Transactional
    public void batchCreateForProject(Long projectId, String salaryPeriod) {
        List<Employee> employees = employeeMapper.findByProjectId(projectId);
        log.info("[SalaryService] batchCreateForProject: projectId={}, period={}, employees={}", projectId, salaryPeriod, employees.size());
        for (Employee emp : employees) {
            SalarySlip existing = salarySlipMapper.selectByEmployeeAndPeriod(emp.getId(), salaryPeriod, projectId);
            if (existing != null) {
                // 已存在则重新计算出勤天数和应付工资（确保 projectId 已设置，考勤按项目过滤）
                if (existing.getProjectId() == null) {
                    existing.setProjectId(projectId);
                }
                recalcBaseAmount(existing);
                existing.setAdditionAmount(existing.getAdditionAmount() != null ? existing.getAdditionAmount() : BigDecimal.ZERO);
                existing.setDeductionAmount(existing.getDeductionAmount() != null ? existing.getDeductionAmount() : BigDecimal.ZERO);
                existing.calculatePayable();
                salarySlipMapper.update(existing);
                log.info("[SalaryService] Updated existing slip for employee {} {}, days={}", emp.getId(), emp.getName(), existing.getAttendanceDays());
                continue;
            }

            log.info("[SalaryService] Creating slip for employee {} {}", emp.getId(), emp.getName());
            SalarySlip slip = new SalarySlip();
            slip.setEmployeeId(emp.getId());
            slip.setProjectId(projectId);
            slip.setSalaryPeriod(salaryPeriod);
            slip.setIdCard(emp.getPhone());
            slip.setJobCategoryName(emp.getJobCategoryName());
            slip.setSalaryType(emp.getSalaryType());
            slip.setBaseSalary(emp.getSalaryAmount());
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
            if (slip.getIdCard() == null)          slip.setIdCard(emp.getPhone()); // phone 暂代 id_card
            if (slip.getJobCategoryName() == null) slip.setJobCategoryName(emp.getJobCategoryName());
            if (slip.getSalaryType() == null)      slip.setSalaryType(emp.getSalaryType());
            if (slip.getBaseSalary() == null)      slip.setBaseSalary(emp.getSalaryAmount());
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
        return salarySlipMapper.selectList(employeeId, salaryPeriod, projectId);
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
    public void updateSalaryItemAmount(Long itemId, BigDecimal newAmount) {
        SalaryItem item = salaryItemMapper.selectById(itemId);
        if (item == null) throw new RuntimeException("费用项不存在");
        checkNotLocked(item.getSalarySlipId());
        item.setAmount(newAmount);
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
            slip.setAttendanceDays(daysDecimal);
            BigDecimal base = slip.getBaseSalary() != null ? slip.getBaseSalary() : BigDecimal.ZERO;
            slip.setBaseAmount(base.multiply(daysDecimal));
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
        BigDecimal payable = base.add(addition).subtract(deduction);
        salarySlipMapper.updateAmounts(salarySlipId, addition, deduction, payable);
    }
}
