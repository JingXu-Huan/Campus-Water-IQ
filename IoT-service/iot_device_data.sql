create table water.iot_device_data
(
    id           bigint auto_increment comment '主键'
        primary key,
    device_code  varchar(64)                        not null comment '设备唯一编号',
    device_type  varchar(32)                        not null comment '设备类型（WATER_METER / WATER_QUALITY）',
    collect_time datetime                           not null comment '设备采集时间',
    data_payload json                               not null comment '设备上报原始数据（JSON）',
    create_time  datetime default CURRENT_TIMESTAMP null
)
    comment 'IoT 设备上报数据表';

create index idx_device_time
    on water.iot_device_data (device_code, collect_time);