create database water;
use water;
create table water.device_reservation
(
    id            bigint auto_increment
        primary key,
    device_code   varchar(16)                           not null comment '设备编码：ANBCXYZZZ',
    device_type   tinyint                               not null comment 'A：1水表 / 2传感器',
    campus_no     tinyint                               not null comment 'N：1花园 / 2龙子湖 / 3江淮',
    building_no   char(2)                               not null comment 'BC：01-99',
    floor_no      char(2)                               not null comment 'XY：01-99',
    unit_no       char(3)                               not null comment 'ZZZ：001-999（传感器固定001）',
    reporter_name varchar(64)                           not null comment '姓名',
    contact_info  varchar(64)                           not null comment '联系方式',
    fault_desc    varchar(500)                          not null comment '故障描述',
    severity      tinyint     default 2                 not null comment '严重程度：1低 2中 3高',
    status        varchar(16) default 'DRAFT'           not null comment '状态：DRAFT / CONFIRMED / PROCESSING / DONE / CANCELLED',
    remark        varchar(255)                          null comment '备注',
    created_at    datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at    datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '智能报修/预约表';

create index idx_device_code
    on water.device_reservation (device_code);

create index idx_location
    on water.device_reservation (campus_no, building_no, floor_no);

create index idx_status
    on water.device_reservation (status);

create table water.device_user
(
    id          int auto_increment
        primary key,
    device_code varchar(10) null comment '设备ID',
    uid         varchar(40) not null
)
    comment '设备和用户对应关系表';

create index device_user_device_code_index
    on water.device_user (device_code);

create index device_user_id_index
    on water.device_user (uid);

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

create table water.user
(
    id                bigint auto_increment
        primary key,
    uid               varchar(100)                       not null comment '唯一用户ID',
    nick_name         varchar(20)                        not null comment '昵称',
    email             varchar(50)                        not null comment '邮箱',
    phone_num         varchar(20)                        null comment '手机号',
    password          varchar(255)                       not null comment '密码',
    last_login_time   datetime                           null comment '上一次登陆时间',
    create_date       datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time       datetime default CURRENT_TIMESTAMP null comment '更新时间',
    user_type         tinyint  default 1                 not null comment '1普通用户 2运维 3管理员',
    bind_device_count int      default 0                 not null comment '绑定设备数量',
    deleted           tinyint  default 0                 not null comment '逻辑删除',
    status            tinyint  default 1                 not null comment '1正常 0禁用 2冻结',
    github_id         varchar(100)                       null comment 'GitHub用户ID，用于OAuth登录绑定'
);

create index idx_user_github_id
    on water.user (github_id);

create index user_email_index
    on water.user (email);

create index user_phone_num_index_
    on water.user (phone_num);

create index user_uid_index
    on water.user (uid);

create table water.virtual_device
(
    id           bigint auto_increment comment '自增唯一主键'
        primary key,
    device_code  char(10)                                                          not null comment '业务唯一编码 (AXBCXYZZZ)',
    sn_code      varchar(50)                                                       null comment '设备物理序列号（出厂唯一码）',
    device_type  tinyint                                                           not null comment '1: 水表, 2: 传感器',
    building_no  char(2)                                                           not null comment '楼宇编号（对应 BC）',
    floor_no     int                                                               not null comment '楼层（对应 XY）',
    room_no      char(3)                                                           not null comment '房间/单元号（对应 ZZZ）',
    install_date date                                                              null comment '安装日期',
    status       enum ('online', 'offline', 'error', 'scrapped') default 'offline' null comment '在线、离线、异常、报废',
    is_running   tinyint(1)                                                        null,
    campus_no    varchar(2) charset utf8mb3                                        null,
    constraint uk_device_code
        unique (device_code)
)
    comment '设备基础资产表';

create index idx_sn_code
    on water.virtual_device (sn_code);

