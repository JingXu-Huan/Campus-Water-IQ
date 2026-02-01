create table water.virtual_device
(
    id           bigint auto_increment comment '自增唯一主键'
        primary key,
    device_code  char(10)                                                          not null comment '业务唯一编码 (AXBCXYZZZ)',
    sn_code      varchar(50)                                                       null comment '设备物理序列号（出厂唯一码）',
    device_type  tinyint                                                           not null comment '1: 水表, 2: 传感器',
    building_no  char(2)                                                           not null comment '楼宇编号（对应 BC）',
    campus_no    varchar(2) charset utf8mb3                                        not null comment '校区编号',
    floor_no     int                                                               not null comment '楼层（对应 XY）',
    room_no      char(3)                                                           not null comment '房间/单元号（对应 ZZZ）',
    install_date date                                                              null comment '安装日期',
    status       enum ('online', 'offline', 'error', 'scrapped') default 'offline' null comment '在线、离线、异常、报废',
    is_running   tinyint(1)                                                        null,

    constraint uk_device_code
        unique (device_code)
)
    comment '设备基础资产表';

create index idx_sn_code
    on water.virtual_device (sn_code);
