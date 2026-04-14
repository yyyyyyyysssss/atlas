-- 1. 在业务库中创建广播表镜像（结构与 atlas_user 中一致）
CREATE TABLE IF NOT EXISTS `organization` LIKE `atlas_user`.`organization`;
CREATE TABLE IF NOT EXISTS `role_data_scope` LIKE `atlas_user`.`role_data_scope`;
CREATE TABLE IF NOT EXISTS `user_role` LIKE `atlas_user`.`user_role`;

-- 2. 为业务表增加权限核心字段（幂等处理）
-- 根据实际业务表名替换 biz_table
ALTER TABLE `biz_table`
    ADD COLUMN IF NOT EXISTS `org_id` bigint DEFAULT NULL COMMENT '组织ID',
    ADD COLUMN IF NOT EXISTS `creator_id` bigint DEFAULT NULL COMMENT '创建者ID',
    ADD INDEX IF NOT EXISTS `idx_data_permission` (`org_id`, `creator_id`);

-- 3. 将母体库的数据全量覆盖到业务库（广播表初始化）
-- 根据实际业务库名替换 biz_db
INSERT INTO `biz_db`.`organization` SELECT * FROM `atlas_user`.`organization`
    ON DUPLICATE KEY UPDATE org_name=VALUES(org_name), org_path=VALUES(org_path);

INSERT INTO `biz_db`.`role_data_scope` SELECT * FROM `atlas_user`.`role_data_scope`
    ON DUPLICATE KEY UPDATE role_id=VALUES(role_id), org_id=VALUES(org_id);

INSERT INTO `biz_db`.`user_role` SELECT * FROM `atlas_user`.`user_role`
    ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);