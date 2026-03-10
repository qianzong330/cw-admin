-- 为记账表添加发票图片字段
ALTER TABLE tb_account ADD COLUMN invoice_images TEXT COMMENT '发票图片URL列表，逗号分隔';
