-- 添加字段到 yonghu 表
-- 执行前请备份数据库

ALTER TABLE yonghu ADD COLUMN is_receive_notification VARCHAR(200) DEFAULT '是' COMMENT '是否接收通知';
