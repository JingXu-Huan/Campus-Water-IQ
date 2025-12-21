CREATE TABLE `virtual_device` (
                                 `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增唯一主键',
                                 `device_code` CHAR(8) NOT NULL COMMENT '业务唯一编码 (ABCXYZZZ)',
                                 `sn_code` VARCHAR(50) DEFAULT NULL COMMENT '设备物理序列号（出厂唯一码）',
                                 `device_type` TINYINT NOT NULL COMMENT '1: 水表, 2: 传感器',
                                 `building_no` CHAR(2) NOT NULL COMMENT '楼宇编号（对应 BC）',
                                 `floor_no` INT NOT NULL COMMENT '楼层（对应 XY）',
                                 `room_no` CHAR(3) NOT NULL COMMENT '房间/单元号（对应 ZZZ）',
                                 `install_date` DATE DEFAULT NULL COMMENT '安装日期',
                                 `status` ENUM('online', 'offline', 'error', 'scrapped') DEFAULT 'offline' COMMENT '在线、离线、异常、报废',

                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_device_code` (`device_code`), -- 保证 ABCXYZZZ 编码唯一
                                 INDEX `idx_sn_code` (`sn_code`)               -- 方便通过物理序列号检索
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备基础资产表';