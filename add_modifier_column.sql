-- 添加修改人字段到工资费用项表
ALTER TABLE tb_salary_item 
ADD COLUMN modifier VARCHAR(50) COMMENT '修改人姓名' AFTER remark;

-- 添加变更日志表（用于记录费用项变更历史）
CREATE TABLE IF NOT EXISTS tb_salary_item_change_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    salary_slip_id BIGINT NOT NULL COMMENT '工资条ID',
    item_id BIGINT COMMENT '费用项ID（新增时为NULL）',
    item_type TINYINT NOT NULL COMMENT '费用类型：1-费用+，2-费用-',
    item_name VARCHAR(50) NOT NULL COMMENT '费用名称',
    old_amount DECIMAL(12,2) COMMENT '变更前金额',
    new_amount DECIMAL(12,2) COMMENT '变更后金额',
    change_type VARCHAR(20) NOT NULL COMMENT '变更类型：ADD-新增，EDIT-修改，DELETE-删除',
    modifier VARCHAR(50) COMMENT '修改人姓名',
    remark VARCHAR(200) COMMENT '备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_salary_slip (salary_slip_id),
    INDEX idx_item_id (item_id),
    INDEX idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资费用项变更日志表';
