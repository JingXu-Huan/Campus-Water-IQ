create table water.iot_device_event
(
    id           bigint auto_increment comment '主键'
        primary key,
    device_code  varchar(64)                        not null comment '设备编号',
    device_type  varchar(32)                        not null comment '设备类型',
    event_type   varchar(32)                        not null comment '事件类型（OFFLINE / ABNORMAL / THRESHOLD）',
    event_level  varchar(16)                        null comment '事件级别（INFO/WARN/ERROR）',
    event_desc   varchar(255)                       null,
    event_time   datetime                           not null,
    handled_flag tinyint  default 0                 null,
    create_time  datetime default CURRENT_TIMESTAMP null,
    parent_id    bigint                             null,
    cnt          int                                null
)
    comment 'IoT 设备事件表';

create index idx_event_device_time
    on water.iot_device_event (device_code, event_time);

