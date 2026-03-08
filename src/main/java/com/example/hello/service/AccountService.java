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

    public List<Account> findByCondition(Long currentUserId, boolean isBoss, Long projectId, Integer status) {
        return accountMapper.findByCondition(currentUserId, isBoss, projectId, status);
    }

    @Transactional
    public boolean save(Account account, Employee currentUser) {
        boolean isNew = account.getId() == null;
        
        if (isNew) {
            // 新增
            account.setCreatorId(currentUser.getId());
            account.setRoleId(currentUser.getRoleId());
            account.setJobCategoryId(currentUser.getJobCategoryId());
            
            // BOSS直接生效，其他人需要审批
            String roleCode = currentUser.getRoleCode() != null ? currentUser.getRoleCode().toLowerCase() : "";
            boolean isBoss = "boss".equals(roleCode);
            if (isBoss) {
                account.setStatus(5); // 生效
            } else {
                account.setStatus(1); // 审批中
            }
            
            accountMapper.insert(account);
            
            // 记录操作明细
            AccountDetail detail = new AccountDetail();
            detail.setProjectId(account.getProjectId());
            detail.setAccountId(account.getId());
            detail.setOperatorName(currentUser.getName());
            detail.setOperatorId(currentUser.getId());
            detail.setActionType("CREATE");
            detail.setRemark(account.getRemark());
            detail.setOperateTime(LocalDateTime.now());
            accountDetailMapper.insert(detail);
            
            // 非BOSS提交审批记录
            if (!isBoss) {
                AccountDetail auditDetail = new AccountDetail();
                auditDetail.setProjectId(account.getProjectId());
                auditDetail.setAccountId(account.getId());
                auditDetail.setOperatorName(currentUser.getName());
                auditDetail.setOperatorId(currentUser.getId());
                auditDetail.setActionType("SUBMIT_AUDIT");
                // 获取财务对接人姓名
                String financeContactName = "-";
                Long financeContactId = currentUser.getFinanceContactId();
                if (financeContactId != null) {
                    Employee financeContact = employeeMapper.findById(financeContactId);
                    if (financeContact != null) {
                        financeContactName = financeContact.getName();
                    }
                }
                // 如果提交人自己就是财务对接人
                if (financeContactId != null && financeContactId.equals(currentUser.getId())) {
                    auditDetail.setRemark("提交审批（自己审批自己）");
                } else {
                    auditDetail.setRemark("提交审批，等待 " + financeContactName + " 审批");
                }
                auditDetail.setOperateTime(LocalDateTime.now());
                accountDetailMapper.insert(auditDetail);
            }
        } else {
            // 编辑
            accountMapper.update(account);
            
            // 记录操作明细
            AccountDetail detail = new AccountDetail();
            detail.setProjectId(account.getProjectId());
            detail.setAccountId(account.getId());
            detail.setOperatorName(currentUser.getName());
            detail.setOperatorId(currentUser.getId());
            detail.setActionType("UPDATE");
            detail.setRemark(account.getRemark());
            detail.setOperateTime(LocalDateTime.now());
            accountDetailMapper.insert(detail);
            
            // 编辑后变为审批中状态
            accountMapper.updateStatus(account.getId(), 1);
            
            AccountDetail auditDetail = new AccountDetail();
            auditDetail.setProjectId(account.getProjectId());
            auditDetail.setAccountId(account.getId());
            auditDetail.setOperatorName(currentUser.getName());
            auditDetail.setOperatorId(currentUser.getId());
            auditDetail.setActionType("SUBMIT_AUDIT");
            // 获取财务对接人姓名
            String financeContactName = "-";
            Long financeContactId = currentUser.getFinanceContactId();
            if (financeContactId != null) {
                Employee financeContact = employeeMapper.findById(financeContactId);
                if (financeContact != null) {
                    financeContactName = financeContact.getName();
                }
            }
            // 如果提交人自己就是财务对接人
            if (financeContactId != null && financeContactId.equals(currentUser.getId())) {
                auditDetail.setRemark("编辑后重新提交审批（自己审批自己）");
            } else {
                auditDetail.setRemark("编辑后重新提交审批，等待 " + financeContactName + " 审批");
            }
            auditDetail.setOperateTime(LocalDateTime.now());
            accountDetailMapper.insert(auditDetail);
        }
        
        return true;
    }

    @Transactional
    public boolean approve(Long accountId, Employee currentUser, boolean approved, String remark) {
        Account account = accountMapper.findById(accountId);
        if (account == null) {
            return false;
        }
        
        int newStatus = approved ? 5 : 12; // 5=生效, 12=审核未通过
        accountMapper.updateStatus(accountId, newStatus);
        
        // 记录操作明细
        AccountDetail detail = new AccountDetail();
        detail.setProjectId(account.getProjectId());
        detail.setAccountId(accountId);
        detail.setOperatorName(currentUser.getName());
        detail.setOperatorId(currentUser.getId());
        detail.setActionType(approved ? "APPROVE" : "REJECT");
        detail.setRemark(remark);
        detail.setOperateTime(LocalDateTime.now());
        accountDetailMapper.insert(detail);
        
        return true;
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

    public List<Account> findByConditionWithPage(Long userId, boolean isBoss, Long projectId, Integer status, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return accountMapper.findByConditionWithPage(userId, isBoss, projectId, status, offset, pageSize);
    }

    public int countByCondition(Long userId, boolean isBoss, Long projectId, Integer status) {
        return accountMapper.countByCondition(userId, isBoss, projectId, status);
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
