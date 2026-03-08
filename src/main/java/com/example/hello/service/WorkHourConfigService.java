package com.example.hello.service;

import com.example.hello.entity.WorkHourConfig;
import com.example.hello.entity.Employee;
import com.example.hello.mapper.WorkHourConfigMapper;
import com.example.hello.mapper.EmployeeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkHourConfigService {
    
    // 状态常量
    public static final int STATUS_PENDING = 1;     // 审批中
    public static final int STATUS_ACTIVE = 5;      // 生效中
    public static final int STATUS_INACTIVE = 12;   // 未生效（草稿/被拒绝/撤销/作废）
    
    @Autowired
    private WorkHourConfigMapper workHourConfigMapper;
    
    @Autowired
    private EmployeeMapper employeeMapper;
    
    /**
     * 创建工时配置
     * @param config 配置信息
     * @param currentUser 当前用户（发起人）
     * @throws Exception 业务异常
     */
    @Transactional
    public void createConfig(WorkHourConfig config, Employee currentUser) throws Exception {
        String calcTypeName = config.getCalcType() == 1 ? "日薪计算" : "月薪计算";
            
        // 检查是否已存在相同计算方式的配置（不限制状态）
        List<WorkHourConfig> existingConfigs = workHourConfigMapper.selectByCalcType(config.getCalcType());
        if (existingConfigs != null && !existingConfigs.isEmpty()) {
            throw new Exception(calcTypeName + "已存在配置，不允许重复添加");
        }
            
        // 设置发起人信息
        config.setCreatedById(currentUser.getId());
        config.setCreatedByName(currentUser.getName());
            
        // 判断是否为 root 或 BOSS 发起（直接生效，跳过审批）
        String roleCode = currentUser.getRoleCode();
        boolean isDirectActive = "root".equalsIgnoreCase(roleCode) || "boss".equalsIgnoreCase(roleCode);
            
        if (isDirectActive) {
            // root/BOSS 发起，直接生效
            config.setStatus(STATUS_ACTIVE);
            config.setApprovedById(currentUser.getId());
            config.setApprovedByName(currentUser.getName());
            config.setApprovedTime(LocalDateTime.now());
        } else {
            // 其他情况，进入审批流程
            config.setStatus(STATUS_PENDING);
        }
            
        workHourConfigMapper.insert(config);
    }
    
    /**
     * 更新工时配置（仅允许更新未生效或生效中状态的配置）
     */
    @Transactional
    public void updateConfig(WorkHourConfig config) throws Exception {
        WorkHourConfig existing = workHourConfigMapper.selectById(config.getId());
        if (existing == null) {
            throw new Exception("配置不存在");
        }
        
        // 只有未生效或生效中状态的配置才能编辑
        if (existing.getStatus() != STATUS_INACTIVE && existing.getStatus() != STATUS_ACTIVE) {
            throw new Exception("只有未生效或生效中的配置才能编辑");
        }
        
        // 检查是否已存在其他相同计算方式的配置（排除当前配置）
        List<WorkHourConfig> existingConfigs = workHourConfigMapper.selectByCalcType(config.getCalcType());
        if (existingConfigs != null && !existingConfigs.isEmpty()) {
            for (WorkHourConfig c : existingConfigs) {
                if (!c.getId().equals(config.getId())) {
                    String calcTypeName = config.getCalcType() == 1 ? "日薪计算" : "月薪计算";
                    throw new Exception(calcTypeName + "已存在配置，不允许重复添加");
                }
            }
        }
        
        // 更新配置数据
        workHourConfigMapper.update(config);
        
        // 如果是发起人撤销后重新编辑保存，需要重新发起审批（状态改为审批中）
        // 但如果是 root 或 BOSS 编辑且没有生效中的配置，则直接生效
        Employee creator = employeeMapper.findById(existing.getCreatedById());
        boolean isDirectActive = creator != null && (
            "root".equalsIgnoreCase(creator.getRoleCode()) || 
            "boss".equalsIgnoreCase(creator.getRoleCode())
        );
        WorkHourConfig existingActive = workHourConfigMapper.selectActiveByCalcType(existing.getCalcType());
        
        if (isDirectActive && existingActive == null) {
            // root/BOSS 编辑且没有生效中的配置，直接生效
            workHourConfigMapper.updateStatus(config.getId(), STATUS_ACTIVE, null, null, null, null);
        } else {
            // 其他情况，重新进入审批流程，并更新发起时间
            workHourConfigMapper.updateStatus(config.getId(), STATUS_PENDING, null, null, null, null);
            // 更新发起时间为当前系统时间
            workHourConfigMapper.updateCreatedTime(config.getId(), LocalDateTime.now());
        }
    }
    
    /**
     * 审批通过
     */
    @Transactional
    public void approveConfig(Long id, Employee approver, String remark) throws Exception {
        WorkHourConfig config = workHourConfigMapper.selectById(id);
        if (config == null) {
            throw new Exception("配置不存在");
        }
        
        if (config.getStatus() != STATUS_PENDING) {
            throw new Exception("只有审批中的配置才能审批");
        }
        
        // 检查是否已存在相同计算方式的生效配置
        WorkHourConfig existingActive = workHourConfigMapper.selectActiveByCalcType(config.getCalcType());
        if (existingActive != null) {
            String calcTypeName = config.getCalcType() == 1 ? "日薪计算" : "月薪计算";
            throw new Exception("已存在生效中的" + calcTypeName + "配置，请先禁用后再审批");
        }
        
        // 更新为生效中
        workHourConfigMapper.updateStatus(id, STATUS_ACTIVE, approver.getId(), approver.getName(), LocalDateTime.now(), remark);
    }
    
    /**
     * 审批拒绝
     */
    @Transactional
    public void rejectConfig(Long id, Employee approver, String remark) throws Exception {
        WorkHourConfig config = workHourConfigMapper.selectById(id);
        if (config == null) {
            throw new Exception("配置不存在");
        }
        
        if (config.getStatus() != STATUS_PENDING) {
            throw new Exception("只有审批中的配置才能审批");
        }
        
        // 更新为未生效
        workHourConfigMapper.updateStatus(id, STATUS_INACTIVE, approver.getId(), approver.getName(), LocalDateTime.now(), remark);
    }
    
    /**
     * 删除所有配置（仅用于测试/重置）
     */
    @Transactional
    public void deleteAllConfigs() {
        List<WorkHourConfig> configs = workHourConfigMapper.selectAll();
        for (WorkHourConfig config : configs) {
            workHourConfigMapper.deleteById(config.getId());
        }
    }
    
    /**
     * 禁用生效中的配置
     */
    @Transactional
    public void deactivateConfig(Long id) throws Exception {
        WorkHourConfig config = workHourConfigMapper.selectById(id);
        if (config == null) {
            throw new Exception("配置不存在");
        }
        
        if (config.getStatus() != STATUS_ACTIVE) {
            throw new Exception("只有生效中的配置才能禁用");
        }
        
        workHourConfigMapper.updateStatus(id, STATUS_INACTIVE, null, null, null, null);
    }
    
    /**
     * 撤销审批中的配置
     */
    @Transactional
    public void revokeConfig(Long id, Long userId) throws Exception {
        WorkHourConfig config = workHourConfigMapper.selectById(id);
        if (config == null) {
            throw new Exception("配置不存在");
        }
        
        if (config.getStatus() != STATUS_PENDING) {
            throw new Exception("只有审批中的配置才能撤销");
        }
        
        // 只有发起人才能撤销
        if (config.getCreatedById() == null || !config.getCreatedById().equals(userId)) {
            throw new Exception("只有发起人才能撤销");
        }
        
        // 更新为未生效
        workHourConfigMapper.updateStatus(id, STATUS_INACTIVE, null, null, null, "发起人撤销");
    }
    
    /**
     * 删除未生效配置或其他状态配置（非员工角色可用：BOSS、财务、root）
     */
    @Transactional
    public void deleteConfig(Long id, Employee currentUser) throws Exception {
        WorkHourConfig config = workHourConfigMapper.selectById(id);
        if (config == null) {
            throw new Exception("配置不存在");
        }
        
        // root 角色可以删除任何非标准状态的记录
        boolean isRoot = "root".equalsIgnoreCase(currentUser.getRoleCode());
        if (isRoot) {
            // root 角色可以删除除审批中、生效中、未生效外的其他状态
            if (config.getStatus() == STATUS_PENDING || config.getStatus() == STATUS_ACTIVE || config.getStatus() == STATUS_INACTIVE) {
                throw new Exception("不能删除正常流转状态的配置");
            }
            // root 角色可以直接删除
            workHourConfigMapper.deleteById(id);
            return;
        }
        
        // 只能删除未生效状态的配置
        if (config.getStatus() != STATUS_INACTIVE) {
            throw new Exception("只能删除未生效的配置");
        }
        
        // 验证权限：只有非员工角色（BOSS、财务）才可以删除
        String roleCode = currentUser.getRoleCode();
        if (roleCode == null || ("employee".equalsIgnoreCase(roleCode))) {
            throw new Exception("只有非员工角色才可以删除该配置");
        }
        
        // 执行删除
        workHourConfigMapper.deleteById(id);
    }
    
    /**
     * 作废生效中的配置（仅 BOSS 可用）
     */
    @Transactional
    public void invalidateConfig(Long id, Employee currentUser) throws Exception {
        WorkHourConfig config = workHourConfigMapper.selectById(id);
        if (config == null) {
            throw new Exception("配置不存在");
        }
        
        // 只能作废生效中的配置
        if (config.getStatus() != STATUS_ACTIVE) {
            throw new Exception("只能作废生效中的配置");
        }
        
        // 验证权限：只有 root 或 BOSS 才能作废
        String roleCode = currentUser.getRoleCode();
        if (!"root".equalsIgnoreCase(roleCode) && !"boss".equalsIgnoreCase(roleCode)) {
            throw new Exception("只有超级管理员或 BOSS 才能作废配置");
        }
        
        // 更新状态为未生效
        workHourConfigMapper.updateStatus(id, STATUS_INACTIVE, null, null, null, "BOSS 作废");
    }
    
    public WorkHourConfig getConfigById(Long id) {
        return workHourConfigMapper.selectById(id);
    }
    
    public List<WorkHourConfig> getAllConfigs() {
        return workHourConfigMapper.selectAll();
    }
    
    /**
     * 获取所有审批中的配置
     */
    public List<WorkHourConfig> getPendingConfigs() {
        List<WorkHourConfig> allConfigs = workHourConfigMapper.selectAll();
        return allConfigs.stream()
                .filter(c -> c.getStatus() != null && c.getStatus() == STATUS_PENDING)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 获取当前生效中的工时配置（查第一条 status=5 的记录）
     */
    public WorkHourConfig getActiveConfig() {
        List<WorkHourConfig> all = workHourConfigMapper.selectAll();
        if (all != null) {
            for (WorkHourConfig c : all) {
                if (c.getStatus() != null && c.getStatus() == STATUS_ACTIVE) {
                    return c;
                }
            }
            if (!all.isEmpty()) {
                return all.get(0);
            }
        }
        return null;
    }
    

    
    /**
     * 计算加班费
     */
    public BigDecimal calculateOvertimePay(WorkHourConfig config, Integer overtimeType, BigDecimal hours, BigDecimal baseSalary) {
        if (config == null || hours == null || hours.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal hourlyRate;
        BigDecimal rate;
        
        switch (overtimeType) {
            case 1: // 工作日加班
                hourlyRate = config.getWeekdayOvertimeHourly();
                rate = config.getWeekdayOvertimeRate();
                break;
            case 2: // 休息日加班
                hourlyRate = config.getRestdayOvertimeHourly();
                rate = config.getRestdayOvertimeRate();
                break;
            case 3: // 节假日加班
                hourlyRate = config.getHolidayOvertimeHourly();
                rate = config.getHolidayOvertimeRate();
                break;
            default:
                return BigDecimal.ZERO;
        }
        
        // 如果没有设置固定时薪，按基本工资计算
        if (hourlyRate == null || hourlyRate.compareTo(BigDecimal.ZERO) <= 0) {
            if (baseSalary != null && baseSalary.compareTo(BigDecimal.ZERO) > 0) {
                // 假设每月21.75个工作日，每天8小时
                hourlyRate = baseSalary.divide(new BigDecimal("21.75"), 2, BigDecimal.ROUND_HALF_UP)
                        .divide(new BigDecimal("8"), 2, BigDecimal.ROUND_HALF_UP);
            } else {
                hourlyRate = BigDecimal.ZERO;
            }
        }
        
        return hourlyRate.multiply(rate).multiply(hours).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}

