-- 添加字段到 jiuzhentongzhi 表
-- 执行前请备份数据库

ALTER TABLE jiuzhentongzhi ADD COLUMN send_status VARCHAR(200) DEFAULT NULL COMMENT '发送状态';
ALTER TABLE jiuzhentongzhi ADD COLUMN send_time DATETIME DEFAULT NULL COMMENT '发送时间';
ALTER TABLE jiuzhentongzhi ADD COLUMN retry_count INT DEFAULT 0 COMMENT '重试次数';
ALTER TABLE jiuzhentongzhi ADD COLUMN error_message TEXT DEFAULT NULL COMMENT '错误信息';
