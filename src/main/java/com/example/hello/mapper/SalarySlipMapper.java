package com.example.hello.mapper;

import com.example.hello.entity.SalarySlip;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工资条 Mapper
 */
@Mapper
public interface SalarySlipMapper {

    int insert(SalarySlip salarySlip);

    int update(SalarySlip salarySlip);

    int updateAmounts(@Param("id") Long id,
                      @Param("additionAmount") BigDecimal additionAmount,
                      @Param("deductionAmount") BigDecimal deductionAmount,
                      @Param("payableAmount") BigDecimal payableAmount);

    // 工资条本身无状态，以下方法已废弃
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    int updateApprove(@Param("id") Long id,
                      @Param("status") Integer status,
                      @Param("approvedBy") Long approvedBy,
                      @Param("approvedTime") LocalDateTime approvedTime,
                      @Param("approveRemark") String approveRemark);

    int deleteById(Long id);

    /**
     * 按项目和工资期删除工资条（用于考勤驳回时清理）
     */
    int deleteByProjectAndPeriod(@Param("projectId") Long projectId, @Param("salaryPeriod") String salaryPeriod);

    SalarySlip selectById(Long id);

    List<SalarySlip> selectList(@Param("employeeId") Long employeeId,
                                @Param("salaryPeriod") String salaryPeriod,
                                @Param("projectId") Long projectId);

    SalarySlip selectByEmployeeAndPeriod(@Param("employeeId") Long employeeId,
                                         @Param("salaryPeriod") String salaryPeriod);
}
