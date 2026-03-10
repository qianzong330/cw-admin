package com.example.hello.service;

import com.example.hello.dto.CategoryStatsDTO;
import com.example.hello.dto.ProjectStatsDTO;
import com.example.hello.entity.Account;
import com.example.hello.entity.AccountDetail;
import com.example.hello.entity.Employee;
import com.example.hello.mapper.AccountMapper;
import com.example.hello.mapper.AccountDetailMapper;
import com.example.hello.mapper.EmployeeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountService {

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private AccountDetailMapper accountDetailMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    public Account findById(Long id) {
        return accountMapper.findById(id);
    }

    public List<Account> findAll() {
        return accountMapper.findAll();
    }

    public List<Account> findByCondition(Long currentUserId, boolean isBoss, boolean isFinance, Long projectId, Integer status) {
        return accountMapper.findByCondition(currentUserId, isBoss, isFinance, projectId, status);
    }

    @Transactional
    public boolean save(Account account, Employee currentUser) {
        boolean isNew = account.getId() == null;
        
        if (isNew) {
            // 新增
            account.setCreatorId(currentUser.getId());
            account.setRoleId(currentUser.getRoleId());
            account.setJobCategoryId(currentUser.getJobCategoryId());
            
            // BOSS直接生效，财务直接到BOSS审批，普通员工需要财务审批
            String roleCode = currentUser.getRoleCode() != null ? currentUser.getRoleCode().toLowerCase() : "";
            boolean isBoss = "boss".equals(roleCode) || "root".equals(roleCode);
            boolean isFinance = currentUser.isFinance();
            
            if (isBoss) {
                account.setStatus(5); // 生效
                account.setApprovalStage(null);
            } else if (isFinance) {
                // 财务发起的记账，直接到BOSS审批阶段
                account.setStatus(1); // 审批中
                account.setApprovalStage(2); // 待BOSS审批
                account.setApprovedByFinance("FINANCE"); // 标记为财务发起，跳过财务审批
            } else {
                // 普通员工，需要财务审批
                account.setStatus(1); // 审批中
                account.setApprovalStage(1); // 待财务审批
                account.setApprovedByFinance(""); // 初始化空字符串
            }
            
            accountMapper.insert(account);
            
            // 记录操作明细：BOSS直接生效记录CREATE，其他人记录SUBMIT（发起记账审批）
            AccountDetail detail = new AccountDetail();
            detail.setProjectId(account.getProjectId());
            detail.setAccountId(account.getId());
            detail.setOperatorName(currentUser.getName());
            detail.setOperatorId(currentUser.getId());
            detail.setActionType(isBoss ? "CREATE" : "SUBMIT");
            // 使用用户填写的备注，如果没有则显示默认文案
            String newRemark = account.getRemark();
            if (isBoss) {
                detail.setRemark((newRemark != null && !newRemark.isEmpty()) ? newRemark : "创建帐条");
            } else {
                detail.setRemark((newRemark != null && !newRemark.isEmpty()) ? newRemark : "发起记账审批");
            }
            detail.setOperateTime(LocalDateTime.now());
            accountDetailMapper.insert(detail);
        } else {
            // 编辑前检查：已生效(status=5)的帐条不允许编辑
            Account existing = accountMapper.findById(account.getId());
            if (existing != null && existing.getStatus() != null && existing.getStatus() == 5) {
                throw new RuntimeException("已生效的帐条不允许修改");
            }
            
            // 先更新帐条基本信息
            accountMapper.update(account);
            
            // 编辑后重置审批状态：财务直接到BOSS审批，普通员工到财务审批
            String roleCode = currentUser.getRoleCode() != null ? currentUser.getRoleCode().toLowerCase() : "";
            boolean isFinance = currentUser.isFinance();
            
            Account statusUpdate = new Account();
            statusUpdate.setId(account.getId());
            statusUpdate.setStatus(1); // 审批中
            if (isFinance) {
                // 财务编辑，直接到BOSS审批
                statusUpdate.setApprovalStage(2);
                statusUpdate.setApprovedByFinance("FINANCE");
            } else {
                // 普通员工编辑，到财务审批
                statusUpdate.setApprovalStage(1);
                statusUpdate.setApprovedByFinance("");
            }
            accountMapper.updateApprovalStage(statusUpdate);
            
            // 记录操作明细：重新提交审批，使用用户填写的备注
            AccountDetail detail = new AccountDetail();
            detail.setProjectId(account.getProjectId());
            detail.setAccountId(account.getId());
            detail.setOperatorName(currentUser.getName());
            detail.setOperatorId(currentUser.getId());
            detail.setActionType("RESUBMIT");
            // 使用用户填写的备注，如果没有则显示默认文案
            String userRemark = account.getRemark();
            detail.setRemark((userRemark != null && !userRemark.isEmpty()) ? userRemark : "重新提交审批");
            detail.setOperateTime(LocalDateTime.now());
            accountDetailMapper.insert(detail);
        }
        
        return true;
    }
    
    /**
     * 记录提交审批的操作明细
     */
    private void recordAuditSubmit(Account account, Employee currentUser) {
        AccountDetail auditDetail = new AccountDetail();
        auditDetail.setProjectId(account.getProjectId());
        auditDetail.setAccountId(account.getId());
        auditDetail.setOperatorName(currentUser.getName());
        auditDetail.setOperatorId(currentUser.getId());
        auditDetail.setActionType("SUBMIT_AUDIT");
        auditDetail.setRemark("提交审批，等待财务审批");
        auditDetail.setOperateTime(LocalDateTime.now());
        accountDetailMapper.insert(auditDetail);
    }

    @Transactional
    public boolean approve(Long accountId, Employee currentUser, boolean approved, String remark) {
        Account account = accountMapper.findById(accountId);
        if (account == null) {
            return false;
        }
        
        String roleCode = currentUser.getRoleCode() != null ? currentUser.getRoleCode().toLowerCase() : "";
        boolean isBoss = "boss".equals(roleCode) || "root".equals(roleCode);
        boolean isFinance = currentUser.isFinance();
        
        Integer approvalStage = account.getApprovalStage();
        if (approvalStage == null) {
            approvalStage = 1;
        }
        
        if (approved) {
            // 审批通过
            if (isFinance && approvalStage == 1) {
                // 财务审批通过，进入BOSS审批阶段
                String approvedByFinance = account.getApprovedByFinance();
                if (approvedByFinance == null) {
                    approvedByFinance = "";
                }
                
                // 记录已审批的财务
                if (!approvedByFinance.isEmpty()) {
                    approvedByFinance += ",";
                }
                approvedByFinance += currentUser.getId();
                
                account.setApprovedByFinance(approvedByFinance);
                account.setApprovalStage(2); // 进入BOSS审批
                accountMapper.updateApprovalStage(account);
                
                // 记录操作明细
                recordApprovalDetail(account, currentUser, "APPROVE", "财务审批通过，转BOSS审批");
                
            } else if (isBoss || (isFinance && approvalStage == 2)) {
                // BOSS审批通过，或财务在BOSS阶段审批（兜底）
                account.setStatus(5); // 生效
                account.setApprovalStage(null);
                account.setFinalApproverId(currentUser.getId());
                accountMapper.updateApprovalStage(account);
                accountMapper.updateStatus(accountId, 5);
                
                // 记录操作明细，包含审批通过时的金额
                String approverType = isBoss ? "BOSS" : "财务";
                String amountStr = account.getAmount() != null ? account.getAmount().toString() : "0";
                recordApprovalDetail(account, currentUser, "APPROVE", 
                    approverType + "审批通过，金额：" + amountStr + "元，帐条生效");
            }
        } else {
            // 审批驳回
            account.setStatus(12); // 审核未通过
            account.setApprovalStage(null);
            accountMapper.updateApprovalStage(account);
            accountMapper.updateStatus(accountId, 12);
            
            // 记录操作明细
            recordApprovalDetail(account, currentUser, "REJECT", "审批驳回：" + (remark != null ? remark : ""));
        }
        
        return true;
    }
    
    /**
     * 记录审批操作明细
     */
    private void recordApprovalDetail(Account account, Employee currentUser, String actionType, String remark) {
        AccountDetail detail = new AccountDetail();
        detail.setProjectId(account.getProjectId());
        detail.setAccountId(account.getId());
        detail.setOperatorName(currentUser.getName());
        detail.setOperatorId(currentUser.getId());
        detail.setActionType(actionType);
        detail.setRemark(remark);
        detail.setOperateTime(LocalDateTime.now());
        accountDetailMapper.insert(detail);
    }

    @Transactional
    public boolean revoke(Long accountId, Employee currentUser) {
        Account account = accountMapper.findById(accountId);
        if (account == null) {
            return false;
        }
        
        // 删除帐条
        accountMapper.deleteById(accountId);
        
        // 记录操作明细
        AccountDetail detail = new AccountDetail();
        detail.setProjectId(account.getProjectId());
        detail.setAccountId(accountId);
        detail.setOperatorName(currentUser.getName());
        detail.setOperatorId(currentUser.getId());
        detail.setActionType("REVOKE");
        detail.setRemark("撤销帐条");
        detail.setOperateTime(LocalDateTime.now());
        accountDetailMapper.insert(detail);
        
        return true;
    }

    public boolean deleteById(Long id) {
        return accountMapper.deleteById(id) > 0;
    }

    public List<Account> findPendingByFinanceContactId(Long financeContactId, boolean isBoss) {
        return accountMapper.findPendingByFinanceContactId(financeContactId, isBoss);
    }

    public List<Account> findByConditionWithPage(Long userId, boolean isBoss, boolean isFinance, Long projectId, Integer status, Integer type, String creatorName, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return accountMapper.findByConditionWithPage(userId, isBoss, isFinance, projectId, status, type, creatorName, offset, pageSize);
    }

    public int countByCondition(Long userId, boolean isBoss, boolean isFinance, Long projectId, Integer status, Integer type, String creatorName) {
        return accountMapper.countByCondition(userId, isBoss, isFinance, projectId, status, type, creatorName);
    }

    public List<Account> findPendingByFinanceContactIdWithPage(Long financeContactId, boolean isBoss, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return accountMapper.findPendingByFinanceContactIdWithPage(financeContactId, isBoss, offset, pageSize);
    }

    public int countPendingByFinanceContactId(Long financeContactId, boolean isBoss) {
        return accountMapper.countPendingByFinanceContactId(financeContactId, isBoss);
    }

    // 统计方法 - 支持时间筛选
    public List<ProjectStatsDTO> getProjectStats(Long userId, boolean isBoss, String startDate, String endDate) {
        return accountMapper.getProjectStats(userId, isBoss, startDate, endDate);
    }

    public ProjectStatsDTO getSingleProjectStats(Long projectId, String startDate, String endDate) {
        return accountMapper.getSingleProjectStats(projectId, startDate, endDate);
    }

    public BigDecimal getTotalIncome(Long userId, boolean isBoss, String startDate, String endDate) {
        return accountMapper.getTotalIncome(userId, isBoss, startDate, endDate);
    }

    public BigDecimal getTotalExpense(Long userId, boolean isBoss, String startDate, String endDate) {
        return accountMapper.getTotalExpense(userId, isBoss, startDate, endDate);
    }

    public List<ProjectStatsDTO> getTop5IncomeProjects(Long userId, boolean isBoss, String startDate, String endDate) {
        return accountMapper.getTop5IncomeProjects(userId, isBoss, startDate, endDate);
    }

    public List<ProjectStatsDTO> getTop5ExpenseProjects(Long userId, boolean isBoss, String startDate, String endDate) {
        return accountMapper.getTop5ExpenseProjects(userId, isBoss, startDate, endDate);
    }

    public List<CategoryStatsDTO> getProjectIncomeByCategory(Long projectId, String startDate, String endDate) {
        return accountMapper.getProjectIncomeByCategory(projectId, startDate, endDate);
    }

    public List<CategoryStatsDTO> getProjectExpenseByCategory(Long projectId, String startDate, String endDate) {
        return accountMapper.getProjectExpenseByCategory(projectId, startDate, endDate);
    }
}
