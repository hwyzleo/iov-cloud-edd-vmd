-- 车系品牌代码迁移
-- 将车系的平台代码关联改为品牌代码关联

-- 添加品牌代码字段
ALTER TABLE `tb_veh_series` ADD COLUMN `brand_code` varchar(32) DEFAULT NULL COMMENT '品牌代码' AFTER `id`;

-- 添加品牌代码索引
CREATE INDEX `idx_series_brand_code` ON `tb_veh_series` (`brand_code`);

-- 移除平台代码字段
ALTER TABLE `tb_veh_series` DROP COLUMN `platform_code`;