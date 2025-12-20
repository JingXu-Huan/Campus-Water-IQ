CREATE TABLE iot_device_data (
                                 id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',

                                 device_code VARCHAR(64) NOT NULL COMMENT '设备唯一编号',
                                 device_type VARCHAR(32) NOT NULL COMMENT '设备类型（WATER_METER / WATER_QUALITY）',

                                 collect_time DATETIME NOT NULL COMMENT '设备采集时间',

                                 data_payload JSON NOT NULL COMMENT '设备上报原始数据（JSON）',

                                 create_time DATETIME DEFAULT CURRENT_TIMESTAMP,

                                 INDEX idx_device_time (device_code, collect_time)
) COMMENT='IoT 设备上报数据表';
