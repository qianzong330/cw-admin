package com.example.hello.mapper;

import com.example.hello.entity.WorkHourConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WorkHourConfigMapper {
    
    int insert(WorkHourConfig config);
    
    int update(WorkHourConfig config);
    
    int deleteById(@Param("id") Long id);
    
    WorkHourConfig selectById(@Param("id") Long id);
    
    List<WorkHourConfig> selectAll();
    
    WorkHourConfig selectActiveByCalcType(@Param("calcType") Integer calcType);
    
    List<WorkHourConfig> selectByCalcType(@Param("calcType") Integer calcType);
    
    int updateStatus(@Param("id") Long id, @Param("status") Integer status,
                     @Param("approvedById") Long approvedById, @Param("approvedByName") String approvedByName,
                     @Param("approvedTime") java.time.LocalDateTime approvedTime,
                     @Param("approveRemark") String approveRemark);
    
    int updateCreatedTime(@Param("id") Long id, @Param("createdTime") java.time.LocalDateTime createdTime);
}
