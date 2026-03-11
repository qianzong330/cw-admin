package com.example.hello.mapper;

import com.example.hello.dto.CategoryStatsDTO;
import com.example.hello.dto.ProjectStatsDTO;
import com.example.hello.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface AccountMapper {
    
    Account findById(Long id);
    
    List<Account> findAll();
    
    List<Account> findByCreatorId(Long creatorId);
    
    List<Account> findByCondition(@Param("currentUserId") Long currentUserId, 
                                   @Param("isBoss") boolean isBoss,
                                   @Param("isProjectAdmin") boolean isProjectAdmin,
                                   @Param("projectId") Long projectId,
                                   @Param("status") Integer status);

    // 根据项目ID和状态查询记账列表
    List<Account> findByProjectIdAndStatus(@Param("projectId") Long projectId, 
                                           @Param("status") Integer status);

    List<Account> findByConditionWithPage(@Param("currentUserId") Long currentUserId, 
                                           @Param("isBoss") boolean isBoss,
                                           @Param("isProjectAdmin") boolean isProjectAdmin,
                                           @Param("isAdmin") boolean isAdmin,
                                           @Param("projectId") Long projectId,
                                           @Param("status") Integer status,
                                           @Param("type") Integer type,
                                           @Param("creatorName") String creatorName,
                                           @Param("offset") int offset,
                                           @Param("pageSize") int pageSize);

    int countByCondition(@Param("currentUserId") Long currentUserId, 
                         @Param("isBoss") boolean isBoss,
                         @Param("isProjectAdmin") boolean isProjectAdmin,
                         @Param("isAdmin") boolean isAdmin,
                         @Param("projectId") Long projectId,
                         @Param("status") Integer status,
                         @Param("type") Integer type,
                         @Param("creatorName") String creatorName);
    
    // 统计待财务审批的数量（所有待财务审批的帐条）
    int countPendingFinanceApproval();
    
    // 统计待BOSS审批的数量（所有待BOSS审批的帐条）
    int countPendingBossApproval();
    
    // 统计方法 - 支持时间筛选
    List<ProjectStatsDTO> getProjectStats(@Param("userId") Long userId, 
                                          @Param("isBoss") boolean isBoss,
                                          @Param("startDate") String startDate,
                                          @Param("endDate") String endDate);
    
    ProjectStatsDTO getSingleProjectStats(@Param("projectId") Long projectId,
                                          @Param("startDate") String startDate,
                                          @Param("endDate") String endDate);
    
    BigDecimal getTotalIncome(@Param("userId") Long userId, 
                              @Param("isBoss") boolean isBoss,
                              @Param("startDate") String startDate,
                              @Param("endDate") String endDate);
    
    BigDecimal getTotalExpense(@Param("userId") Long userId, 
                               @Param("isBoss") boolean isBoss,
                               @Param("startDate") String startDate,
                               @Param("endDate") String endDate);
    
    // 获取收入/支出TOP5项目
    List<ProjectStatsDTO> getTop5IncomeProjects(@Param("userId") Long userId, 
                                                @Param("isBoss") boolean isBoss,
                                                @Param("startDate") String startDate,
                                                @Param("endDate") String endDate);
    
    List<ProjectStatsDTO> getTop5ExpenseProjects(@Param("userId") Long userId, 
                                                 @Param("isBoss") boolean isBoss,
                                                 @Param("startDate") String startDate,
                                                 @Param("endDate") String endDate);
    
    // 获取收入/支出TOP5费用分类
    List<ProjectStatsDTO> getTop5IncomeCategories(@Param("userId") Long userId, 
                                                   @Param("isBoss") boolean isBoss,
                                                   @Param("startDate") String startDate,
                                                   @Param("endDate") String endDate);
    
    List<ProjectStatsDTO> getTop5ExpenseCategories(@Param("userId") Long userId, 
                                                    @Param("isBoss") boolean isBoss,
                                                    @Param("startDate") String startDate,
                                                    @Param("endDate") String endDate);
    
    // 获取单个项目的收入/支出分类统计
    List<CategoryStatsDTO> getProjectIncomeByCategory(@Param("projectId") Long projectId,
                                                      @Param("startDate") String startDate,
                                                      @Param("endDate") String endDate);
    
    List<CategoryStatsDTO> getProjectExpenseByCategory(@Param("projectId") Long projectId,
                                                       @Param("startDate") String startDate,
                                                       @Param("endDate") String endDate);
    
    int insert(Account account);
    
    int update(Account account);
    
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    int updateApprovalStage(Account account);
    
    int deleteById(Long id);
}
