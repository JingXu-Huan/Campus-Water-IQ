INSERT INTO water_usage_record (school, `usage`, record_date)
VALUES (1, 520.5, '2026-03-10 08:00:00'),
       (1, 545.2, '2026-03-11 08:30:00'),
       (1, 510.8, '2026-03-12 09:00:00'),
       (1, 580.3, '2026-03-13 08:15:00'),
       (1, 490.6, '2026-03-14 10:00:00'),
       (1, 485.0, '2026-03-15 11:00:00'),
       (1, 530.1, '2026-03-16 08:45:00'),


       (2, 310.2, '2026-03-10 08:10:00'),
       (2, 325.4, '2026-03-11 08:40:00'),
       (2, 318.7, '2026-03-12 09:10:00'),
       (2, 340.9, '2026-03-13 08:20:00'),
       (2, 295.5, '2026-03-14 10:15:00'),
       (2, 288.3, '2026-03-15 11:20:00'),
       (2, 315.6, '2026-03-16 08:50:00'),


       (3, 150.8, '2026-03-10 08:20:00'),
       (3, 165.3, '2026-03-11 08:50:00'),
       (3, 158.1, '2026-03-12 09:20:00'),
       (3, 172.4, '2026-03-13 08:30:00'),
       (3, 140.2, '2026-03-14 10:30:00'),
       (3, 135.7, '2026-03-15 11:40:00'),
       (3, 160.9, '2026-03-16 09:00:00');

-- 场景 1: 花园校区 01号楼 02层 001号水表，中等严重程度
INSERT INTO device_reservation (device_code, device_type, campus_no, building_no, floor_no, unit_no, reporter_name,
                                contact_info, fault_desc, severity, status)
VALUES ('110102001', 1, 1, '01', '02', '001', '张三', '13800138000', '水表指针不走，疑似堵塞', 2, 'CONFIRMED');

-- 场景 2: 龙子湖校区 05号楼 10层 传感器 (固定001)，高严重程度
INSERT INTO device_reservation (device_code, device_type, campus_no, building_no, floor_no, unit_no, reporter_name,
                                contact_info, fault_desc, severity, status)
VALUES ('220510001', 2, 2, '05', '10', '001', '李四', '13912345678', '传感器离线，无法上传数据', 3, 'PROCESSING');

-- 场景 3: 江淮校区 02号楼 03层 015号水表，低严重程度
INSERT INTO device_reservation (device_code, device_type, campus_no, building_no, floor_no, unit_no, reporter_name,
                                contact_info, fault_desc, severity, status)
VALUES ('130203015', 1, 3, '02', '03', '015', '王五', '0371-66668888', '水表外壳轻微裂缝，暂不影响使用', 1, 'DRAFT');