package com.example.hello.service;

import com.example.hello.entity.Attendance;
import com.example.hello.entity.SalarySlip;
import com.example.hello.entity.WorkHourConfig;
import com.example.hello.mapper.AttendanceMapper;
import com.example.hello.mapper.SalarySlipMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 考勤服务
 */
@Service
public class AttendanceService {
    
    @Autowired
    private AttendanceMapper attendanceMapper;
    
    @Autowired
    private SalarySlipMapper salarySlipMapper;
    
    @Autowired
    private WorkHourConfigService workHourConfigService;
    
    /**
     * 检查指定月份的工资条是否已审批（状态为已确认或已发放）
     * 如果已审批，则不允许修改考勤
     */
    private void checkSalarySlipStatus(Long employeeId, LocalDate workDate) {
        String salaryPeriod = workDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        // 工资条本身无状态，由月份状态表控制是否可编辑
        // 此检查逻辑已移至月份状态表控制
    }
    

    
    /**
     * 创建考勤记录
     * 规则：
     * 1. 出勤(1)和加班(2)可以共存
     * 2. 请假(3)和缺勤(4)与出勤/加班互斥
     * 3. 请假会按比例扣减出勤工时
     */
    @Transactional
    public void createAttendance(Attendance attendance) {
        // 检查该月工资条是否已审批
        checkSalarySlipStatus(attendance.getEmployeeId(), attendance.getWorkDate());
        
        Integer newType = attendance.getAttendanceType();
        BigDecimal newHours = attendance.getWorkHours();
        
        // 查询当天同项目的考勤记录（按 project_id 过滤，避免跨项目记录互相冲突）
        List<Attendance> existingList = attendanceMapper.selectByEmployeeAndDateRange(
                attendance.getEmployeeId(), attendance.getWorkDate(), attendance.getWorkDate(),
                attendance.getProjectId());
        
        // 获取工时配置（统一使用默认配置）
        WorkHourConfig config = workHourConfigService.getActiveConfig();
        BigDecimal standardHours = config != null ? config.getDailyWorkHours() : new BigDecimal("8");
        
        // 检查互斥规则
        for (Attendance existing : existingList) {
            Integer existingType = existing.getAttendanceType();
            
            // 情况1：新添加的是请假
            if (newType == 3) {
                if (existingType == 3) {
                    // 同类型已存在，累加请假时间
                    existing.setWorkHours(existing.getWorkHours().add(newHours));
                    attendanceMapper.update(existing);
                    return; // 直接返回，不插入新记录
                }
            }
            // 情况2：新添加的是缺勤 - 覆盖所有记录
            else if (newType == 4) {
                if (existingType == 1 || existingType == 2) {
                    attendanceMapper.deleteById(existing.getId());
                } else if (existingType == 4) {
                    throw new RuntimeException("该员工在" + attendance.getWorkDate() + "已有缺勤记录");
                }
            }
            // 情况3：新添加的是出勤
            else if (newType == 1) {
                // 同类型已存在
                if (existingType == 1) {
                    throw new RuntimeException("该员工在" + attendance.getWorkDate() + "已有出勤记录");
                }
                // 加班可以共存
            }
            // 情况4：新添加的是加班
            else if (newType == 2) {
                // 同类型已存在
                if (existingType == 2) {
                    throw new RuntimeException("该员工在" + attendance.getWorkDate() + "已有加班记录");
                }
                // 出勤可以共存
            }
            // 情其5：新添加的是迟到
            else if (newType == 5) {
                if (existingType == 5) {
                    throw new RuntimeException("该员工在" + attendance.getWorkDate() + "已有迟到记录");
                }
            }
            // 情其6：新添加的是旷工
            else if (newType == 6) {
                if (existingType == 6) {
                    throw new RuntimeException("该员工在" + attendance.getWorkDate() + "已有旷工记录");
                }
            }
        } // end for
        
        // 直接保存
        attendanceMapper.insert(attendance);
    }
    
    private String getAttendanceTypeName(Integer type) {
        switch (type) {
            case 1: return "出勤";
            case 2: return "加班";
            case 3: return "请假";
            case 4: return "缺勤";
            case 5: return "迟到";
            case 6: return "旷工";
            default: return "考勤";
        }
    }
    
    /**
     * 更新考勤记录
     */
    @Transactional
    public void updateAttendance(Attendance attendance) {
        Attendance existing = attendanceMapper.selectById(attendance.getId());
        if (existing == null) {
            throw new RuntimeException("考勤记录不存在");
        }
        
        // 检查原日期对应的工资条是否已审批
        checkSalarySlipStatus(existing.getEmployeeId(), existing.getWorkDate());
        
        // 如果修改了日期，检查新日期是否已有记录，并检查新日期对应的工资条状态
        if (!existing.getWorkDate().equals(attendance.getWorkDate())) {
            checkSalarySlipStatus(attendance.getEmployeeId(), attendance.getWorkDate());
            Attendance conflict = attendanceMapper.selectByEmployeeAndDate(
                    attendance.getEmployeeId(), attendance.getWorkDate());
            if (conflict != null && !conflict.getId().equals(attendance.getId())) {
                throw new RuntimeException("该员工在" + attendance.getWorkDate() + "已有考勤记录");
            }
        }
        
        attendanceMapper.update(attendance);
    }
    
    /**
     * 删除考勤记录
     */
    @Transactional
    public void deleteAttendance(Long id) {
        Attendance existing = attendanceMapper.selectById(id);
        if (existing != null) {
            // 检查该月工资条是否已审批
            checkSalarySlipStatus(existing.getEmployeeId(), existing.getWorkDate());
        }
        attendanceMapper.deleteById(id);
    }
    
    /**
     * 根据ID查询考勤记录
     */
    public Attendance getAttendanceById(Long id) {
        return attendanceMapper.selectById(id);
    }
    
    /**
     * 查询考勤记录列表
     */
    public List<Attendance> listAttendances(Long employeeId, Long projectId, LocalDate startDate, LocalDate endDate) {
        return attendanceMapper.selectList(employeeId, projectId, startDate, endDate);
    }
    
    /**
     * 统计员工在指定月份的出勤天数（只统计已审批的）
     */
    public Integer countAttendanceDays(Long employeeId, YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        Integer days = attendanceMapper.countApprovedAttendanceDays(employeeId, startDate, endDate);
        return days != null ? days : 0;
    }

    /**
     * 结合工时配置计算员工出勤天数：
     * 出勤天数 = (出勤总工时 / 每日工时)
     *           + (工作日加班工时 * 工作日加班费率 / 每日工时)
     *           + (休息日加班工时 * 休息日加班费率 / 每日工时)
     *           + (节假日加班工时 * 节假日加班费率 / 每日工时)
     */
    public BigDecimal calcAttendanceDaysWithConfig(Long employeeId, YearMonth yearMonth) {
        return calcAttendanceDaysWithConfig(employeeId, yearMonth, null, null);
    }
        
    /**
     * 带中间计算明细的重载方法，将中间数据填充到 slip （如果不为 null）
     */
    public BigDecimal calcAttendanceDaysWithConfig(Long employeeId, YearMonth yearMonth, com.example.hello.entity.SalarySlip slip) {
        return calcAttendanceDaysWithConfig(employeeId, yearMonth, slip, slip != null ? slip.getProjectId() : null);
    }
    
    /**
     * 核心方法：按项目ID过滤考勤记录，不同项目的考勤互不干扰
     */
    public BigDecimal calcAttendanceDaysWithConfig(Long employeeId, YearMonth yearMonth, com.example.hello.entity.SalarySlip slip, Long projectId) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        // 获取生效的工时配置（日薪计算方式 calcType=1）
        WorkHourConfig config = workHourConfigService.getActiveConfig();
        if (config == null || config.getDailyWorkHours() == null
                || config.getDailyWorkHours().compareTo(BigDecimal.ZERO) == 0) {
            // 没有工时配置则回退到老方法：直接计数出勤天数
            BigDecimal fallbackDays = new BigDecimal(countAttendanceDays(employeeId, yearMonth));
            if (slip != null) {
                // 用 dailyWorkHours=-1 作为降级模式标记，前端根据此展示不同说明
                slip.setDailyWorkHours(new BigDecimal("-1"));
                slip.setAttendanceHours(fallbackDays); // 复用字段存考勤天数
            }
            return fallbackDays;
        }
        
        BigDecimal dailyHours = config.getDailyWorkHours();
        
        // 1. 出勤总工时（attendance_type=1），按项目过滤
        BigDecimal attendanceHours = attendanceMapper.sumWorkHoursByType(employeeId, startDate, endDate, 1, null, projectId);
        if (attendanceHours == null) attendanceHours = BigDecimal.ZERO;
        
        // 2. 加班工时（attendance_type=2，按 overtime_type 分类），按项目过滤
        BigDecimal weekdayOT  = attendanceMapper.sumWorkHoursByType(employeeId, startDate, endDate, 2, 1, projectId);
        BigDecimal restdayOT  = attendanceMapper.sumWorkHoursByType(employeeId, startDate, endDate, 2, 2, projectId);
        BigDecimal holidayOT  = attendanceMapper.sumWorkHoursByType(employeeId, startDate, endDate, 2, 3, projectId);
        if (weekdayOT == null) weekdayOT = BigDecimal.ZERO;
        if (restdayOT == null) restdayOT = BigDecimal.ZERO;
        if (holidayOT == null) holidayOT = BigDecimal.ZERO;
        
        // 加班费率（未配置则默认 1.0）
        BigDecimal weekdayRate  = config.getWeekdayOvertimeRate()  != null ? config.getWeekdayOvertimeRate()  : BigDecimal.ONE;
        BigDecimal restdayRate  = config.getRestdayOvertimeRate()  != null ? config.getRestdayOvertimeRate()  : BigDecimal.ONE;
        BigDecimal holidayRate  = config.getHolidayOvertimeRate()  != null ? config.getHolidayOvertimeRate()  : BigDecimal.ONE;
        
        // 计算出勤天数（保睵2位小数，四舍五入）
        BigDecimal days = attendanceHours.divide(dailyHours, 4, java.math.RoundingMode.HALF_UP)
            .add(weekdayOT.multiply(weekdayRate).divide(dailyHours, 4, java.math.RoundingMode.HALF_UP))
            .add(restdayOT.multiply(restdayRate).divide(dailyHours, 4, java.math.RoundingMode.HALF_UP))
            .add(holidayOT.multiply(holidayRate).divide(dailyHours, 4, java.math.RoundingMode.HALF_UP));
        
        // 如果提供了 slip，将中间计算明细填充到 slip
        if (slip != null) {
            slip.setAttendanceHours(attendanceHours);
            slip.setWeekdayOTHours(weekdayOT);
            slip.setRestdayOTHours(restdayOT);
            slip.setHolidayOTHours(holidayOT);
            slip.setDailyWorkHours(dailyHours);
            slip.setWeekdayOTRate(weekdayRate);
            slip.setRestdayOTRate(restdayRate);
            slip.setHolidayOTRate(holidayRate);
        }
        
        return days.setScale(2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * 查询员工在某月份的所有考勤记录
     */
    public List<Attendance> getAttendancesByEmployeeAndMonth(Long employeeId, String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        return attendanceMapper.selectList(employeeId, null, startDate, endDate);
    }
    
    /**
     * 根据员工、项目和月份查询考勤记录
     */
    public List<Attendance> getAttendancesByEmployeeProjectAndMonth(Long employeeId, Long projectId, String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        return attendanceMapper.selectByEmployeeProjectAndMonth(employeeId, projectId, startDate, endDate);
    }
}
