package com.example.hello.service;

import com.example.hello.entity.WorkHourConfig;
import com.example.hello.entity.Employee;
import com.example.hello.entity.Project;
import com.example.hello.mapper.WorkHourConfigMapper;
import com.example.hello.mapper.EmployeeMapper;
import com.example.hello.mapper.ProjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    
    @Autowired
    private ProjectMapper projectMapper;
    
    /**
     * 创建工时配置
     * @param config 配置信息
     * @param currentUser 当前用户（发起人）
     * @throws Exception 业务异常
     */
    @Transactional
    public void createConfig(WorkHourConfig config, Employee currentUser) throws Exception {
        String calcTypeName = config.getCalcType() == 1 ? "日薪计算" : "月薪计算";
            
        // 检查同一项目下是否已存在相同计算方式的配置（不限制状态）
        WorkHourConfig existingConfig = workHourConfigMapper.selectByProjectIdAndCalcType(config.getProjectId(), config.getCalcType());
        if (existingConfig != null) {
            throw new Exception("该项目已存在" + calcTypeName + "配置，不允许重复添加");
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
     * BOSS角色：直接生效
     * 管理员角色：需要审批
     */
    @Transactional
    public void updateConfig(WorkHourConfig config, Employee currentUser) throws Exception {
        WorkHourConfig existing = workHourConfigMapper.selectById(config.getId());
        if (existing == null) {
            throw new Exception("配置不存在");
        }
        
        // 只有未生效或生效中状态的配置才能编辑
        if (existing.getStatus() != STATUS_INACTIVE && existing.getStatus() != STATUS_ACTIVE) {
            throw new Exception("只有未生效或生效中的配置才能编辑");
        }
        
        // 检查同一项目下是否已存在其他相同计算方式的配置（排除当前配置）
        WorkHourConfig existingConfig = workHourConfigMapper.selectByProjectIdAndCalcType(config.getProjectId(), config.getCalcType());
        if (existingConfig != null && !existingConfig.getId().equals(config.getId())) {
            String calcTypeName = config.getCalcType() == 1 ? "日薪计算" : "月薪计算";
            throw new Exception("该项目已存在" + calcTypeName + "配置，不允许重复添加");
        }
        
        // 判断是否为 root 或 BOSS
        String roleCode = currentUser.getRoleCode();
        boolean isBoss = "root".equalsIgnoreCase(roleCode) || "boss".equalsIgnoreCase(roleCode);
        
        if (isBoss) {
            // BOSS 编辑，直接生效，免审批
            config.setStatus(STATUS_ACTIVE);
            config.setApprovedById(currentUser.getId());
            config.setApprovedByName(currentUser.getName());
            config.setApprovedTime(LocalDateTime.now());
            workHourConfigMapper.update(config);
        } else {
            // 管理员编辑，进入审批流程
            config.setStatus(STATUS_PENDING);
            workHourConfigMapper.update(config);
            // 更新发起时间
            workHourConfigMapper.updateCreatedTime(config.getId(), LocalDateTime.now());
        }
    }
    
    /**
     * 审批通过
     * 如果是删除审批，则执行删除；否则更新为生效中
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
        
        // 检查是否是删除审批
        String approveRemark = config.getApproveRemark();
        if (approveRemark != null && approveRemark.contains("[删除审批]")) {
            // 删除审批通过，执行删除
            workHourConfigMapper.deleteById(id);
            return;
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
     * 删除配置
     * BOSS角色：可直接删除（免审批）
     * 管理员角色：需要发起删除审批
     */
    @Transactional
    public void deleteConfig(Long id, Employee currentUser) throws Exception {
        WorkHourConfig config = workHourConfigMapper.selectById(id);
        if (config == null) {
            throw new Exception("配置不存在");
        }
        
        String roleCode = currentUser.getRoleCode();
        boolean isBoss = "root".equalsIgnoreCase(roleCode) || "boss".equalsIgnoreCase(roleCode);
        boolean isAdmin = "admin".equalsIgnoreCase(roleCode);
        
        if (isBoss) {
            // BOSS 可直接删除（免审批）
            workHourConfigMapper.deleteById(id);
            return;
        }
        
        if (isAdmin) {
            // 管理员只能删除未生效状态的配置
            // 生效中的配置需要先发起作废审批
            if (config.getStatus() == STATUS_ACTIVE) {
                throw new Exception("生效中的配置需要先发起作废审批，审批通过后才能删除");
            }
            if (config.getStatus() == STATUS_PENDING) {
                throw new Exception("审批中的配置不能删除");
            }
            workHourConfigMapper.deleteById(id);
            return;
        }
        
        // 其他角色无权限
        throw new Exception("无权限删除该配置");
    }
    
    /**
     * 发起删除/作废审批（管理员对生效中配置）
     * 将配置状态改为审批中，并在备注中标记为删除审批
     */
    @Transactional
    public void requestDeleteApproval(Long id, Employee currentUser) throws Exception {
        WorkHourConfig config = workHourConfigMapper.selectById(id);
        if (config == null) {
            throw new Exception("配置不存在");
        }
        
        // 只有生效中的配置才能发起作废审批
        if (config.getStatus() != STATUS_ACTIVE) {
            throw new Exception("只有生效中的配置才能发起作废审批");
        }
        
        // 更新状态为审批中，备注标记为删除审批
        workHourConfigMapper.updateStatus(id, STATUS_PENDING, null, null, null, "[删除审批]");
        workHourConfigMapper.updateCreatedTime(id, LocalDateTime.now());
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
        
        // 验证权限：只有 root 才能作废
        String roleCode = currentUser.getRoleCode();
        if (!"root".equalsIgnoreCase(roleCode)) {
            throw new Exception("只有超级管理员才能作废配置");
        }
        
        // 更新状态为未生效
        workHourConfigMapper.updateStatus(id, STATUS_INACTIVE, null, null, null, "");
    }
    
    public WorkHourConfig getConfigById(Long id) {
        return workHourConfigMapper.selectById(id);
    }
    
    public List<WorkHourConfig> getAllConfigs() {
        List<WorkHourConfig> configs = workHourConfigMapper.selectAll();
        // 填充项目名称
        List<Project> projects = projectMapper.findAll();
        Map<Long, String> projectMap = projects.stream()
                .collect(Collectors.toMap(Project::getId, Project::getName, (k1, k2) -> k1));
        for (WorkHourConfig config : configs) {
            if (config.getProjectId() != null) {
                config.setProjectName(projectMap.getOrDefault(config.getProjectId(), "未知项目"));
            } else {
                config.setProjectName("通用");
            }
        }
        return configs;
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
     * 根据项目ID和计算类型查询配置
     */
    public WorkHourConfig findByProjectIdAndCalcType(Long projectId, Integer calcType) {
        return workHourConfigMapper.selectByProjectIdAndCalcType(projectId, calcType);
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

