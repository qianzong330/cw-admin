package com.example.hello.mapper;

import com.example.hello.entity.SalaryItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工资费用项Mapper
 */
@Mapper
public interface SalaryItemMapper {
    
    /**
     * 插入费用项
     */
    int insert(SalaryItem salaryItem);
    
    /**
     * 更新费用项
     */
    int update(SalaryItem salaryItem);
    
    /**
     * 根据ID删除费用项
     */
    int deleteById(Long id);
    
    /**
     * 根据工资条ID删除所有费用项
     */
    int deleteBySalarySlipId(Long salarySlipId);
    
    /**
     * 根据ID查询费用项
     */
    SalaryItem selectById(Long id);
    
    /**
     * 根据工资条ID查询费用项列表
     */
    List<SalaryItem> selectBySalarySlipId(Long salarySlipId);
    
    /**
     * 更新费用项金额
     */
    int updateAmount(@Param("id") Long id, @Param("amount") java.math.BigDecimal amount);
    
    /**
     * 计算工资条的费用+合计
     */
    java.math.BigDecimal sumAdditionBySalarySlipId(Long salarySlipId);
    
    /**
     * 计算工资条的费用-合计
     */
    java.math.BigDecimal sumDeductionBySalarySlipId(Long salarySlipId);
}
