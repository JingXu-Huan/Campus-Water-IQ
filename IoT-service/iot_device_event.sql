CREATE TABLE iot_device_event (
                                  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',

                                  device_code VARCHAR(64) NOT NULL COMMENT '设备编号',
                                  device_type VARCHAR(32) NOT NULL COMMENT '设备类型',

                                  event_type VARCHAR(32) NOT NULL COMMENT '事件类型（OFFLINE / ABNORMAL / THRESHOLD）',
                                  event_level VARCHAR(16) COMMENT '事件级别（INFO/WARN/ERROR）',

                                  event_desc VARCHAR(255),
                                  event_time DATETIME NOT NULL,

                                  handled_flag TINYINT DEFAULT 0,

                                  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,

                                  INDEX idx_event_device_time (device_code, event_time)
) COMMENT='IoT 设备事件表';
