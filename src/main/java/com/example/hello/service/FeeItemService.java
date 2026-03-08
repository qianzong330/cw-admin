package com.example.hello.service;

import com.example.hello.entity.FeeItem;
import com.example.hello.mapper.FeeItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 费用项服务
 */
@Service
public class FeeItemService {
    
    @Autowired
    private FeeItemMapper feeItemMapper;
    
    /**
     * 新增费用项
     */
    public void addFeeItem(FeeItem feeItem) {
        if (feeItem.getStatus() == null) {
            feeItem.setStatus(1);
        }
        feeItemMapper.insert(feeItem);
    }
    
    /**
     * 删除费用项
     */
    public void deleteFeeItem(Long id) {
        feeItemMapper.deleteById(id);
    }
    
    /**
     * 更新费用项
     */
    public void updateFeeItem(FeeItem feeItem) {
        feeItemMapper.update(feeItem);
    }
    
    /**
     * 查询所有费用项
     */
    public List<FeeItem> getAllFeeItems() {
        return feeItemMapper.selectAll();
    }
    
    /**
     * 查询所有启用的费用项
     */
    public List<FeeItem> getAllEnabledFeeItems() {
        return feeItemMapper.selectAllEnabled();
    }
    
    /**
     * 根据类型查询费用项
     */
    public List<FeeItem> getFeeItemsByType(Integer type) {
        return feeItemMapper.selectByType(type);
    }
    
    /**
     * 根据ID查询费用项
     */
    public FeeItem getFeeItemById(Long id) {
        return feeItemMapper.selectById(id);
    }
}
