package com.example.hello.mapper;

import com.example.hello.entity.FeeItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 费用项Mapper
 */
@Mapper
public interface FeeItemMapper {
    
    /**
     * 插入费用项
     */
    int insert(FeeItem feeItem);
    
    /**
     * 更新费用项
     */
    int update(FeeItem feeItem);
    
    /**
     * 根据ID删除费用项
     */
    int deleteById(Long id);
    
    /**
     * 根据ID查询费用项
     */
    FeeItem selectById(Long id);
    
    /**
     * 查询所有启用的费用项
     */
    List<FeeItem> selectAllEnabled();
    
    /**
     * 根据类型查询费用项
     */
    List<FeeItem> selectByType(@Param("type") Integer type);
    
    /**
     * 查询所有费用项（包含禁用）
     */
    List<FeeItem> selectAll();
}
