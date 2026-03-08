package com.example.hello.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 工时配置相关表初始化器
 * 自动创建 tb_work_hour_config 表（如果不存在）
 */
@Component
public class WorkHourTableInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            System.out.println("=== 检查工时配置相关表 ===");

            // 1. 检查并创建 tb_work_hour_config 表
            Integer configCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tb_work_hour_config'",
                Integer.class
            );
            if (configCount == null || configCount == 0) {
                jdbcTemplate.execute(
                    "CREATE TABLE tb_work_hour_config (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID'," +
                    "calc_type TINYINT NOT NULL DEFAULT 2 COMMENT '计算方式：1-日薪计算，2-月薪计算'," +
                    "monthly_work_days INT DEFAULT 22 COMMENT '每月工作天数'," +
                    "morning_start_time TIME DEFAULT '08:00:00' COMMENT '上午开始时间'," +
                    "morning_end_time TIME DEFAULT '12:00:00' COMMENT '上午结束时间'," +
                    "afternoon_start_time TIME DEFAULT '13:00:00' COMMENT '下午开始时间'," +
                    "afternoon_end_time TIME DEFAULT '17:00:00' COMMENT '下午结束时间'," +
                    "daily_work_hours DECIMAL(4,1) DEFAULT 8.0 COMMENT '每日标准工作时长'," +
                    "overtime_start_time TIME COMMENT '加班开始时间'," +
                    "min_overtime_hours DECIMAL(3,1) DEFAULT 0.5 COMMENT '最小加班时长'," +
                    "weekday_overtime_rate DECIMAL(3,1) DEFAULT 1.5 COMMENT '工作日加班费率'," +
                    "weekday_overtime_hourly DECIMAL(10,2) COMMENT '工作日加班时薪'," +
                    "restday_overtime_rate DECIMAL(3,1) DEFAULT 2.0 COMMENT '休息日加班费率'," +
                    "restday_overtime_hourly DECIMAL(10,2) COMMENT '休息日加班时薪'," +
                    "holiday_overtime_rate DECIMAL(3,1) DEFAULT 3.0 COMMENT '法定节假日加班费率'," +
                    "holiday_overtime_hourly DECIMAL(10,2) COMMENT '节假日加班时薪'," +
                    "status TINYINT DEFAULT 12 COMMENT '状态：1-审批中，5-生效中，12-未生效'," +
                    "created_by_id BIGINT COMMENT '发起人ID'," +
                    "created_by_name VARCHAR(50) COMMENT '发起人姓名'," +
                    "approved_by_id BIGINT COMMENT '审批人ID'," +
                    "approved_by_name VARCHAR(50) COMMENT '审批人姓名'," +
                    "approved_time DATETIME COMMENT '审批时间'," +
                    "approve_remark VARCHAR(500) COMMENT '审批备注'," +
                    "remark VARCHAR(200) COMMENT '备注'," +
                    "created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工时配置表'"
                );
                System.out.println("  ✓ 创建表: tb_work_hour_config");

                // 插入默认配置
                jdbcTemplate.update(
                    "INSERT INTO tb_work_hour_config (id, calc_type, monthly_work_days, " +
                    "morning_start_time, morning_end_time, afternoon_start_time, afternoon_end_time, " +
                    "daily_work_hours, min_overtime_hours, " +
                    "weekday_overtime_rate, restday_overtime_rate, holiday_overtime_rate, " +
                    "status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    1, 2, 22, "08:00:00", "12:00:00", "13:00:00", "17:00:00",
                    8.0, 0.5, 1.5, 2.0, 3.0, 5
                );
                System.out.println("  ✓ 插入默认工时配置");
            } else {
                System.out.println("  ✓ 表已存在: tb_work_hour_config");
            }

            System.out.println("=== 工时配置表检查完成 ===");

        } catch (Exception e) {
            System.err.println("工时配置表初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
