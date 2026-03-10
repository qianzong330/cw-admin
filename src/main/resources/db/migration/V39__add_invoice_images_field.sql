-- 添加发票图片字段到记账表
ALTER TABLE tb_account ADD COLUMN IF NOT EXISTS invoice_images TEXT COMMENT '发票图片URL列表，逗号分隔';
