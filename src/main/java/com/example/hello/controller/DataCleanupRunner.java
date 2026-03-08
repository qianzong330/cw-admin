package com.example.hello.controller;

import com.example.hello.mapper.WorkHourConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 临时工具类：删除重复的工时配置数据
 */
@Component
public class DataCleanupRunner implements CommandLineRunner {
    
    @Autowired
    private WorkHourConfigMapper workHourConfigMapper;
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== 开始清理重复的工时配置数据 ===");
        
        // 查询所有配置
        var allConfigs = workHourConfigMapper.selectAll();
        
        // 按 calc_type 和 created_by_name 分组，找出重复的
        java.util.Map<String, java.util.List<com.example.hello.entity.WorkHourConfig>> grouped = 
            allConfigs.stream().collect(java.util.stream.Collectors.groupingBy(
                c -> c.getCalcType() + "_" + c.getCreatedByName()
            ));
        
        int deletedCount = 0;
        for (java.util.Map.Entry<String, java.util.List<com.example.hello.entity.WorkHourConfig>> entry : grouped.entrySet()) {
            java.util.List<com.example.hello.entity.WorkHourConfig> configs = entry.getValue();
            if (configs.size() > 1) {
                // 保留创建时间最早的那条，删除其他的
                configs.sort((c1, c2) -> {
                    if (c1.getCreatedTime() == null) return 1;
                    if (c2.getCreatedTime() == null) return -1;
                    return c1.getCreatedTime().compareTo(c2.getCreatedTime());
                });
                
                // 删除除了第一条以外的所有记录
                for (int i = 1; i < configs.size(); i++) {
                    Long idToDelete = configs.get(i).getId();
                    System.out.println("删除重复配置 ID: " + idToDelete + 
                        ", 计算方式：" + configs.get(i).getCalcType() + 
                        ", 发起人：" + configs.get(i).getCreatedByName());
                    workHourConfigMapper.deleteById(idToDelete);
                    deletedCount++;
                }
            }
        }
        
        System.out.println("=== 清理完成，共删除 " + deletedCount + " 条重复数据 ===");
    }
}
