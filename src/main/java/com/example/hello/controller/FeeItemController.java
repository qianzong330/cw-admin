package com.example.hello.controller;

import com.example.hello.entity.FeeItem;
import com.example.hello.service.FeeItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 费用项管理控制器
 */
@Controller
@RequestMapping("/feeitem")
public class FeeItemController {
    
    @Autowired
    private FeeItemService feeItemService;
    
    /**
     * 费用项列表页面
     */
    @GetMapping("/list")
    public String list(Model model) {
        List<FeeItem> feeItems = feeItemService.getAllFeeItems();
        model.addAttribute("feeItems", feeItems);
        return "feeitem/list";
    }
    
    /**
     * 获取所有启用的费用项（API）
     */
    @GetMapping("/api/list")
    @ResponseBody
    public List<FeeItem> apiList() {
        return feeItemService.getAllEnabledFeeItems();
    }
    
    /**
     * 根据类型获取费用项（API）
     */
    @GetMapping("/api/listByType")
    @ResponseBody
    public List<FeeItem> apiListByType(@RequestParam Integer type) {
        return feeItemService.getFeeItemsByType(type);
    }
    
    /**
     * 新增费用项
     */
    @PostMapping("/add")
    @ResponseBody
    public String add(@RequestBody FeeItem feeItem) {
        try {
            if (feeItem.getName() == null || feeItem.getName().trim().isEmpty()) {
                return "费用项名称不能为空";
            }
            if (feeItem.getType() == null) {
                return "请选择费用类型";
            }
            feeItem.setName(feeItem.getName().trim());
            feeItemService.addFeeItem(feeItem);
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
    
    /**
     * 删除费用项
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public String delete(@PathVariable Long id) {
        try {
            feeItemService.deleteFeeItem(id);
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
    
    /**
     * 更新费用项状态
     */
    @PostMapping("/updateStatus/{id}")
    @ResponseBody
    public String updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        try {
            FeeItem item = feeItemService.getFeeItemById(id);
            if (item == null) {
                return "费用项不存在";
            }
            item.setStatus(status);
            feeItemService.updateFeeItem(item);
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
