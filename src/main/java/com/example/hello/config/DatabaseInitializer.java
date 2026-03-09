package com.example.hello.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public void run(String... args) {
        migrateTbMenuColumnNames();
        createWorkHourConfigTable();
        createAttendanceMonthStatusTable();
        createSalaryMonthStatusTable();
        ensureApprovalMenusExist();
        ensureBossApprovalPermissions();
        ensureFinanceAttendancePermissions();
        ensureSalaryTableColumns();
        createFeeItemTable();
        ensureAccountTableColumns();
    }

    /**
     * 将 tb_menu 表的 url→menu_url、icon→menu_icon 列重命名，保持与 Mapper 一致
     */
    private void migrateTbMenuColumnNames() {
        try {
            // 检测 url 列是否存在（老列名）
            Integer urlExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='tb_menu' AND column_name='url'",
                Integer.class);
            if (urlExists != null && urlExists > 0) {
                jdbcTemplate.execute("ALTER TABLE tb_menu CHANGE COLUMN `url` `menu_url` VARCHAR(200) COMMENT '菜单路径'");
                System.out.println("tb_menu.url 已重命名为 menu_url");
            }
            // 检测 icon 列是否存在（老列名）
            Integer iconExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='tb_menu' AND column_name='icon'",
                Integer.class);
            if (iconExists != null && iconExists > 0) {
                jdbcTemplate.execute("ALTER TABLE tb_menu CHANGE COLUMN `icon` `menu_icon` VARCHAR(50) COMMENT '菜单图标'");
                System.out.println("tb_menu.icon 已重命名为 menu_icon");
            }
        } catch (Exception e) {
            System.err.println("migrateTbMenuColumnNames 失败: " + e.getMessage());
        }
    }

    /**
     * 确保 tb_salary 表存在新增列
     */
    private void ensureSalaryTableColumns() {
        try {
            String[] columns = {"id_card", "job_category_name", "approve_remark", "approved_by", "approved_time", "project_id"};
            String[] ddls = {
                "ALTER TABLE tb_salary ADD COLUMN id_card VARCHAR(20) NULL COMMENT '身份证号'",
                "ALTER TABLE tb_salary ADD COLUMN job_category_name VARCHAR(100) NULL COMMENT '工种'",
                "ALTER TABLE tb_salary ADD COLUMN approve_remark VARCHAR(500) NULL COMMENT '审批备注'",
                "ALTER TABLE tb_salary ADD COLUMN approved_by BIGINT NULL COMMENT '审批人ID'",
                "ALTER TABLE tb_salary ADD COLUMN approved_time DATETIME NULL COMMENT '审批时间'",
                "ALTER TABLE tb_salary ADD COLUMN project_id BIGINT NULL COMMENT '项目ID'"
            };
            // 删除 tb_salary 的 status 字段（工资条本身无状态，由月份状态表控制）
            try {
                jdbcTemplate.execute("ALTER TABLE tb_salary DROP COLUMN IF EXISTS status");
                System.out.println("tb_salary.status 列已删除");
            } catch (Exception e) {
                // 列可能不存在，忽略错误
            }
            for (int i = 0; i < columns.length; i++) {
                try {
                    Integer cnt = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='tb_salary' AND column_name=?",
                        Integer.class, columns[i]);
                    if (cnt == null || cnt == 0) {
                        jdbcTemplate.execute(ddls[i]);
                        System.out.println("tb_salary." + columns[i] + " 列已添加");
                    }
                } catch (Exception e) {
                    System.err.println("添加 tb_salary." + columns[i] + " 失败: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("ensureSalaryTableColumns 失败: " + e.getMessage());
        }
    }
    
    private void createAttendanceMonthStatusTable() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tb_attendance_month_status'",
                Integer.class
            );
            if (count != null && count > 0) {
                System.out.println("tb_attendance_month_status 表已存在，跳过创建");
                return;
            }
            String sql = "CREATE TABLE IF NOT EXISTS tb_attendance_month_status (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                    "`year_month` VARCHAR(7) NOT NULL COMMENT '年月，格式：yyyy-MM'," +
                    "project_id BIGINT COMMENT '项目ID，null表示全部项目'," +
                    "status INT DEFAULT 0 COMMENT '状态：0-草稿，1-待审批，2-变更待审，5-已审批锁定，12-已驳回'," +
                    "submit_by BIGINT COMMENT '提交人ID'," +
                    "submit_time DATETIME COMMENT '提交时间'," +
                    "submit_remark VARCHAR(500) COMMENT '提交备注'," +
                    "approve_by BIGINT COMMENT '审批人ID'," +
                    "approve_time DATETIME COMMENT '审批时间'," +
                    "approve_remark VARCHAR(500) COMMENT '审批备注'," +
                    "change_apply_by BIGINT COMMENT '变更申请人ID'," +
                    "change_apply_time DATETIME COMMENT '变更申请时间'," +
                    "change_apply_remark VARCHAR(500) COMMENT '变更申请备注'," +
                    "created_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "UNIQUE KEY uk_year_month_project (`year_month`, project_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月度考勤表状态管理'";
            jdbcTemplate.execute(sql);
            System.out.println("tb_attendance_month_status 表创建成功");
        } catch (Exception e) {
            System.err.println("创建 tb_attendance_month_status 表失败: " + e.getMessage());
        }
    }

    /**
     * 创建工资条月份状态表
     */
    private void createSalaryMonthStatusTable() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tb_salary_month_status'",
                Integer.class
            );
            if (count != null && count > 0) {
                System.out.println("tb_salary_month_status 表已存在，跳过创建");
                return;
            }
            String sql = "CREATE TABLE IF NOT EXISTS tb_salary_month_status (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                    "`year_month` VARCHAR(7) NOT NULL COMMENT '年月，格式：yyyy-MM'," +
                    "project_id BIGINT COMMENT '项目ID，null表示全部项目'," +
                    "status INT DEFAULT 0 COMMENT '状态：0-草稿，1-待审批，5-已审批锁定，12-已驳回'," +
                    "submit_by BIGINT COMMENT '提交人ID'," +
                    "submit_time DATETIME COMMENT '提交时间'," +
                    "submit_remark VARCHAR(500) COMMENT '提交备注'," +
                    "approve_by BIGINT COMMENT '审批人ID'," +
                    "approve_time DATETIME COMMENT '审批时间'," +
                    "approve_remark VARCHAR(500) COMMENT '审批备注'," +
                    "created_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "UNIQUE KEY uk_year_month_project (`year_month`, project_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资条月份状态管理'";
            jdbcTemplate.execute(sql);
            System.out.println("tb_salary_month_status 表创建成功");
        } catch (Exception e) {
            System.err.println("创建 tb_salary_month_status 表失败: " + e.getMessage());
        }
    }

    private void createWorkHourConfigTable() {
        try {
            // 检查表是否已存在
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tb_work_hour_config'",
                Integer.class
            );
            
            if (count != null && count > 0) {
                System.out.println("tb_work_hour_config 表已存在，跳过创建");
                // 检查并添加新字段
                addApprovalFieldsIfNotExist();
                return;
            }
            
            // 表不存在时才创建
            String sql = "CREATE TABLE tb_work_hour_config (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID'," +
                    "calc_type TINYINT NOT NULL DEFAULT 2 COMMENT '计算方式：1-日薪计算，2-月薪计算'," +
                    "monthly_work_days INT DEFAULT 22 COMMENT '每月工作天数（仅月薪计算使用）'," +
                    "morning_start_time TIME DEFAULT '08:00:00' COMMENT '上午开始时间'," +
                    "morning_end_time TIME DEFAULT '12:00:00' COMMENT '上午结束时间'," +
                    "afternoon_start_time TIME DEFAULT '13:00:00' COMMENT '下午开始时间'," +
                    "afternoon_end_time TIME DEFAULT '17:00:00' COMMENT '下午结束时间'," +
                    "daily_work_hours DECIMAL(4,1) DEFAULT 8.0 COMMENT '每日标准工作时长（小时，自动计算）'," +
                    "overtime_start_time TIME COMMENT '加班开始时间'," +
                    "min_overtime_hours DECIMAL(3,1) DEFAULT 0.5 COMMENT '最小加班时长（小时）'," +
                    "weekday_overtime_rate DECIMAL(3,1) DEFAULT 1.5 COMMENT '工作日加班费率'," +
                    "weekday_overtime_hourly DECIMAL(10,2) COMMENT '工作日加班时薪'," +
                    "restday_overtime_rate DECIMAL(3,1) DEFAULT 2.0 COMMENT '休息日加班费率'," +
                    "restday_overtime_hourly DECIMAL(10,2) COMMENT '休息日加班时薪'," +
                    "holiday_overtime_rate DECIMAL(3,1) DEFAULT 3.0 COMMENT '法定节假日加班费率'," +
                    "holiday_overtime_hourly DECIMAL(10,2) COMMENT '节假日加班时薪'," +
                    "status TINYINT DEFAULT 12 COMMENT '状态：1-审批中，5-生效中，12-未生效（草稿/被拒绝/撤销/作废））'," +
                    "created_by_id BIGINT COMMENT '发起人ID'," +
                    "created_by_name VARCHAR(50) COMMENT '发起人姓名'," +
                    "approved_by_id BIGINT COMMENT '审批人ID'," +
                    "approved_by_name VARCHAR(50) COMMENT '审批人姓名'," +
                    "approved_time DATETIME COMMENT '审批时间'," +
                    "approve_remark VARCHAR(500) COMMENT '审批备注'," +
                    "remark VARCHAR(200) COMMENT '备注'," +
                    "created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工时配置表'";
            
            jdbcTemplate.execute(sql);
            System.out.println("tb_work_hour_config 表创建成功");
        } catch (Exception e) {
            System.err.println("创建 tb_work_hour_config 表失败: " + e.getMessage());
        }
    }
    
    private void addApprovalFieldsIfNotExist() {
        try {
            // 检查并添加 status 字段（更新注释）
            Integer statusCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_work_hour_config' AND column_name = 'status'",
                Integer.class
            );
            
            if (statusCount != null && statusCount == 0) {
                jdbcTemplate.execute("ALTER TABLE tb_work_hour_config ADD COLUMN status TINYINT DEFAULT 12 COMMENT '状态：1-审批中，5-生效中，12-未生效（草稿/被拒绝/撤销/作废）'");
                System.out.println("tb_work_hour_config 表添加 status 字段成功");
            }
            
            // 检查并添加发起人字段
            Integer createdByIdCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_work_hour_config' AND column_name = 'created_by_id'",
                Integer.class
            );
            
            if (createdByIdCount != null && createdByIdCount == 0) {
                jdbcTemplate.execute("ALTER TABLE tb_work_hour_config ADD COLUMN created_by_id BIGINT COMMENT '发起人ID' AFTER status");
                jdbcTemplate.execute("ALTER TABLE tb_work_hour_config ADD COLUMN created_by_name VARCHAR(50) COMMENT '发起人姓名' AFTER created_by_id");
                System.out.println("tb_work_hour_config 表添加发起人字段成功");
            }
            
            // 检查并添加审批人字段
            Integer approvedByIdCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_work_hour_config' AND column_name = 'approved_by_id'",
                Integer.class
            );
            
            if (approvedByIdCount != null && approvedByIdCount == 0) {
                jdbcTemplate.execute("ALTER TABLE tb_work_hour_config ADD COLUMN approved_by_id BIGINT COMMENT '审批人ID' AFTER created_by_name");
                jdbcTemplate.execute("ALTER TABLE tb_work_hour_config ADD COLUMN approved_by_name VARCHAR(50) COMMENT '审批人姓名' AFTER approved_by_id");
                jdbcTemplate.execute("ALTER TABLE tb_work_hour_config ADD COLUMN approved_time DATETIME COMMENT '审批时间' AFTER approved_by_name");
                System.out.println("tb_work_hour_config 表添加审批人字段成功");
            }
            
            // 检查并添加审批备注字段
            Integer approveRemarkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_work_hour_config' AND column_name = 'approve_remark'",
                Integer.class
            );
            
            if (approveRemarkCount != null && approveRemarkCount == 0) {
                jdbcTemplate.execute("ALTER TABLE tb_work_hour_config ADD COLUMN approve_remark VARCHAR(500) COMMENT '审批备注' AFTER approved_time");
                System.out.println("tb_work_hour_config 表添加 approve_remark 字段成功");
            }
        } catch (Exception e) {
            System.err.println("添加审批字段失败: " + e.getMessage());
        }
    }
    
    /**
     * 确保审批相关菜单存在（兼容 Flyway 迁移未执行的情况）
     */
    private void ensureApprovalMenusExist() {
        try {
            // 确保 tb_menu 表存在
            Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tb_menu'",
                Integer.class);
            if (tableCount == null || tableCount == 0) {
                System.out.println("tb_menu 表不存在，跳过审批菜单初始化");
                return;
            }
            
            // 删除审批管理目录菜单及其子菜单（审批功能移至原页面）
            jdbcTemplate.update("DELETE FROM tb_role_menu WHERE menu_id IN (SELECT id FROM tb_menu WHERE menu_code LIKE 'approval%')");
            jdbcTemplate.update("DELETE FROM tb_menu WHERE menu_code LIKE 'approval%'");
            System.out.println("审批管理菜单已清理");
        } catch (Exception e) {
            System.err.println("初始化审批菜单失败: " + e.getMessage());
        }
    }
    
    /**
     * 确保 BOSS 角色拥有审批菜单权限
     */
    private void ensureBossApprovalPermissions() {
        try {
            // 确保 tb_role 和 tb_role_menu 表存在
            Integer roleMenuTable = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tb_role_menu'",
                Integer.class);
            if (roleMenuTable == null || roleMenuTable == 0) {
                System.out.println("tb_role_menu 表不存在，跳过权限分配");
                return;
            }
            
            Long bossRoleId = null;
            try {
                bossRoleId = jdbcTemplate.queryForObject(
                    "SELECT id FROM tb_role WHERE role_code = 'boss'", Long.class);
            } catch (Exception ignore) {}
            if (bossRoleId == null) {
                System.out.println("未找到 boss 角色，跳过审批权限分配");
                return;
            }
            
            String[] approvalCodes = {
                "approval", "approval:workhour",
                "approval:attendance", "approval:salary"
            };
            int count = 0;
            for (String code : approvalCodes) {
                try {
                    Long menuId = jdbcTemplate.queryForObject(
                        "SELECT id FROM tb_menu WHERE menu_code = ?", Long.class, code);
                    if (menuId != null) {
                        jdbcTemplate.update(
                            "INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (?, ?)",
                            bossRoleId, menuId);
                        count++;
                    }
                } catch (Exception ignore) {}
            }
            System.out.println("=== BOSS 审批权限分配完成，共分配 " + count + " 个 ===");
        } catch (Exception e) {
            System.err.println("分配 BOSS 审批权限失败: " + e.getMessage());
        }
    }
    
    /**
     * 确保财务角色拥有考勤管理权限
     */
    private void ensureFinanceAttendancePermissions() {
        try {
            Long financeRoleId = null;
            try {
                financeRoleId = jdbcTemplate.queryForObject(
                    "SELECT id FROM tb_role WHERE role_code = 'finance'", Long.class);
            } catch (Exception ignore) {}
            if (financeRoleId == null) {
                System.out.println("未找到 finance 角色，跳过考勤权限分配");
                return;
            }
            
            String[] attendanceCodes = {
                "attendance", "attendance:add", "attendance:edit", "attendance:delete"
            };
            int count = 0;
            for (String code : attendanceCodes) {
                try {
                    Long menuId = jdbcTemplate.queryForObject(
                        "SELECT id FROM tb_menu WHERE menu_code = ?", Long.class, code);
                    if (menuId != null) {
                        jdbcTemplate.update(
                            "INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (?, ?)",
                            financeRoleId, menuId);
                        count++;
                    }
                } catch (Exception ignore) {}
            }
            System.out.println("=== 财务考勤权限分配完成，共分配 " + count + " 个 ===");
        } catch (Exception e) {
            System.err.println("分配财务考勤权限失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建费用项表
     */
    private void createFeeItemTable() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS tb_fee_item (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID'," +
                "  name VARCHAR(100) NOT NULL COMMENT '费用项名称'," +
                "  type TINYINT NOT NULL COMMENT '类型：1-加项，2-减项'," +
                "  status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用'," +
                "  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                "  UNIQUE KEY uk_name_type (name, type)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用项配置表'"
            );
            System.out.println("=== 费用项表 tb_fee_item 初始化完成 ===");
        } catch (Exception e) {
            System.err.println("创建费用项表失败: " + e.getMessage());
        }
    }
    
    /**
     * 确保费用项管理菜单存在
     */
    private void ensureFeeItemMenuExists() {
        try {
            // 获取基础配置菜单ID
            Long basicConfigId = null;
            try {
                basicConfigId = jdbcTemplate.queryForObject(
                    "SELECT id FROM tb_menu WHERE menu_code = 'basic_config'", Long.class);
            } catch (Exception e) {}
            
            if (basicConfigId == null) {
                System.err.println("基础配置菜单不存在，跳过费用项菜单创建");
                return;
            }
            
            // 检查费用项管理菜单是否存在
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_menu WHERE menu_code = 'feeitem:list'", Integer.class);
            
            if (count == null || count == 0) {
                // 创建费用项管理菜单
                jdbcTemplate.update(
                    "INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status) " +
                    "VALUES ('feeitem:list', '费用项管理', ?, 2, '/feeitem/list', 'bi-tags', 6, 1)",
                    basicConfigId
                );
                
                // 获取新创建的菜单ID
                Long feeItemMenuId = jdbcTemplate.queryForObject(
                    "SELECT id FROM tb_menu WHERE menu_code = 'feeitem:list'", Long.class);
                
                // 给 root 角色分配权限
                jdbcTemplate.update(
                    "INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (1, ?)",
                    feeItemMenuId
                );
                
                System.out.println("=== 费用项管理菜单创建完成 ===");
            } else {
                System.out.println("=== 费用项管理菜单已存在 ===");
            }
        } catch (Exception e) {
            System.err.println("创建费用项管理菜单失败: " + e.getMessage());
        }
    }
    
    /**
     * 确保 tb_account 表有所有必要的字段
     */
    private void ensureAccountTableColumns() {
        try {
            // 检查并添加 category_id 字段
            Integer categoryIdExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='tb_account' AND column_name='category_id'",
                Integer.class);
            if (categoryIdExists == null || categoryIdExists == 0) {
                jdbcTemplate.execute("ALTER TABLE tb_account ADD COLUMN category_id BIGINT COMMENT '分类ID'");
                System.out.println("tb_account.category_id 字段已添加");
            }
            
            // 检查并添加 approval_stage 字段
            Integer approvalStageExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='tb_account' AND column_name='approval_stage'",
                Integer.class);
            if (approvalStageExists == null || approvalStageExists == 0) {
                jdbcTemplate.execute("ALTER TABLE tb_account ADD COLUMN approval_stage TINYINT DEFAULT 1 COMMENT '审批阶段：1-待财务审批，2-待BOSS审批'");
                System.out.println("tb_account.approval_stage 字段已添加");
            }
            
            // 检查并添加 approved_by_finance 字段
            Integer approvedByFinanceExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='tb_account' AND column_name='approved_by_finance'",
                Integer.class);
            if (approvedByFinanceExists == null || approvedByFinanceExists == 0) {
                jdbcTemplate.execute("ALTER TABLE tb_account ADD COLUMN approved_by_finance VARCHAR(500) COMMENT '已审批的财务人员ID列表'");
                System.out.println("tb_account.approved_by_finance 字段已添加");
            }
            
            // 检查并添加 final_approver_id 字段
            Integer finalApproverIdExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='tb_account' AND column_name='final_approver_id'",
                Integer.class);
            if (finalApproverIdExists == null || finalApproverIdExists == 0) {
                jdbcTemplate.execute("ALTER TABLE tb_account ADD COLUMN final_approver_id BIGINT COMMENT '最终审批人ID（BOSS）'");
                System.out.println("tb_account.final_approver_id 字段已添加");
            }
            
            System.out.println("=== tb_account 表字段检查完成 ===");
        } catch (Exception e) {
            System.err.println("ensureAccountTableColumns 失败: " + e.getMessage());
        }
    }
}
