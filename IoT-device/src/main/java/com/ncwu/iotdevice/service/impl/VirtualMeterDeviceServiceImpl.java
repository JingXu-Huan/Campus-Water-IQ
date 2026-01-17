package com.ncwu.iotdevice.service.impl;


import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncwu.common.VO.Result;
import com.ncwu.common.enums.ErrorCode;
import com.ncwu.common.enums.SuccessCode;
import com.ncwu.iotdevice.AOP.annotation.InitLuaScript;
import com.ncwu.iotdevice.AOP.annotation.Time;
import com.ncwu.iotdevice.Constants.DeviceStatus;
import com.ncwu.iotdevice.config.ServerConfig;
import com.ncwu.iotdevice.domain.Bo.DeviceIdList;
import com.ncwu.iotdevice.domain.Bo.MeterDataBo;
import com.ncwu.iotdevice.domain.entity.VirtualDevice;
import com.ncwu.iotdevice.mapper.DeviceMapper;
import com.ncwu.iotdevice.service.DataSender;
import com.ncwu.iotdevice.service.VirtualMeterDeviceService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ncwu.iotdevice.AOP.Aspects.InitLuaScript.Lua_script;
import static com.ncwu.iotdevice.utils.Utils.*;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/21
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualMeterDeviceServiceImpl extends ServiceImpl<DeviceMapper, VirtualDevice>
        implements VirtualMeterDeviceService {

    private int allSize;
    //运行中的设备集合
    private volatile Set<String> runningDevices = ConcurrentHashMap.newKeySet();
    //实验楼数量
    int experiment;
    //教学楼数量
    int education;
    final String deviceStatusPrefix = "cache:device:status:";

    private ScheduledExecutorService scheduler;
    private final StringRedisTemplate redisTemplate;
    private final RocketMQTemplate rocketMQTemplate;
    private final DeviceMapper deviceMapper;
    private final VirtualWaterQualityDeviceServiceImpl waterQualityDeviceService;
    private final ServerConfig serverConfig;
    private final DataSender dataSender;


    @PostConstruct
    void init() {
        // 经压测验证，约 70 台虚拟设备对应 1 个调度线程即可满足精度与吞吐
        // 取 max 防止入参为 0 发生异常
        scheduler = Executors.newScheduledThreadPool(Math.max(5, (int) (this.count() / 70)));
        experiment = Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get("device:experimentBuildings")));
        education = Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get("device:educationBuildings")));
    }

    private static final Random RANDOM = new Random();

    //开大小为5的线程池
    final ExecutorService pool = Executors.newFixedThreadPool(5);

    // 存储每个设备的调度句柄，用于精准停止模拟
    private final Map<String, ScheduledFuture<?>> deviceTasks = new ConcurrentHashMap<>();

    //存储设备上报时间戳，以便和心跳对齐
    private final ConcurrentHashMap<String, Long> reportTime = new ConcurrentHashMap<>();
    //设备是否已经完成了初始化
    public volatile boolean isInit = false;

    /**
     * 停止模拟时调用，优雅关闭资源
     */
    @PreDestroy
    public void destroy() {
        // 确保应用关闭时停止所有调度
        stopSimulation();
        // 确保应用关闭之后清空 redis 中所有数据
        clearRedisData(redisTemplate, deviceMapper);
        scheduler.shutdown();
    }

    /**
     * 前端入口：所有设备开始模拟
     */
    //获取方法耗时
    @Time
    //初始化 Lua 脚本
    @InitLuaScript
    @Override
    public Result<String> start() {
        //检查设备初始化状态开关
        if (!isInit) {
            return Result.fail(ErrorCode.DEVICE_INIT_ERROR.code(),
                    ErrorCode.DEVICE_INIT_ERROR.message());
        }
        Set<String> ids = redisTemplate.opsForSet().members("device:meter");
        //检查模拟器状态开关
        if (ids != null && runningDevices.size() == ids.size()) {
            log.info("模拟器已全部在运行中");
            return Result.fail(ErrorCode.DEVICE_DEVICE_RUNNING_NOW_ERROR.code(),
                    ErrorCode.DEVICE_DEVICE_RUNNING_NOW_ERROR.message());
        }
        //开始模拟
        if (ids != null) {
            runningDevices.addAll(ids);
        }
        if (ids != null && !ids.isEmpty()) {
            pool.submit(() -> {
                this.lambdaUpdate().in(VirtualDevice::getDeviceCode, ids)
                        .eq(VirtualDevice::getIsRunning, false)
                        .set(VirtualDevice::getIsRunning, true).update();
            });
            String hashKey = "OnLineMap";
            //使用Lua脚本加速redis操作！！！
            redisTemplate.execute(Lua_script, List.of(hashKey), "-1");
            // 每一个设备开启一个独立的递归流
            runningDevices.forEach(this::scheduleNextReport);
            runningDevices.forEach(this::startHeartbeat);
            log.info("成功开启 {} 台设备的模拟数据流", ids.size());
            //可以受检
            redisTemplate.opsForValue().set("MeterChecked", "1");
            return Result.ok("成功开启" + ids.size() + "台设备");
        }
        return Result.fail(ErrorCode.UNKNOWN.code(), ErrorCode.UNKNOWN.message());
    }

    @Override
    public Result<String> startList(List<String> ids) {
        if (ids != null && runningDevices.size() == allSize) {
            log.info("模拟器已全部在运行中");
            return Result.fail(ErrorCode.DEVICE_DEVICE_RUNNING_NOW_ERROR.code(),
                    ErrorCode.DEVICE_DEVICE_RUNNING_NOW_ERROR.message());
        }
        if (!isInit) {
            return Result.fail(ErrorCode.DEVICE_INIT_ERROR.code(),
                    ErrorCode.DEVICE_INIT_ERROR.message());
        }
        if (ids != null && !ids.isEmpty()) {
            //删除缓存
            ids.forEach(id -> redisTemplate.delete(deviceStatusPrefix + id));
            runningDevices.addAll(ids);
            // 每一个设备开启一个独立的递归流
            ids.forEach(id -> redisTemplate.opsForHash().put("OnLineMap", id, String.valueOf(System.currentTimeMillis())));
            ids.forEach(this::startHeartbeat);
            ids.forEach(this::scheduleNextReport);
            pool.submit(() -> {
                this.lambdaUpdate().in(VirtualDevice::getDeviceCode, ids)
                        .eq(VirtualDevice::getIsRunning, false)
                        .set(VirtualDevice::getIsRunning, true)
                        .set(VirtualDevice::getStatus, true).update();
            });
            log.info("批量：成功开启 {} 台设备的模拟数据流", ids.size());

            return Result.ok("成功开启" + ids.size() + "台设备");
        }
        return Result.fail(ErrorCode.UNKNOWN.code(), ErrorCode.UNKNOWN.message());
    }

    /**
     * 前端入口：所有虚拟设备停止模拟
     */
    @Time
    @InitLuaScript
    @Override
    public Result<String> stopSimulation() {
        //停止受检
        redisTemplate.opsForValue().set("MeterChecked", "0");
        //关闭开关
        runningDevices.clear();
        // 取消所有排队中的倒计时任务
        deviceTasks.forEach((id, future) -> {
            if (future != null && !future.isCancelled()) {
                future.cancel(false);
            }
        });
        //写回数据库状态，在异步线程池
        //由于异步线程的异常不被事务控制，这里最好用消息队列
        rocketMQTemplate.convertAndSend("OpsForDataBase", "LetAllMetersStopRunning");
        //删状态缓存
        //此方法是安全的，使用scan进行删除
        redisScanDel(deviceStatusPrefix + "*", 100, redisTemplate);
        deviceTasks.clear();
        log.info("已停止所有模拟数据上报任务");
        return Result.ok("已停止所有模拟数据上报任务");
    }

    /**
     * 前端入口：单设备或者批量设备停止模拟
     */
    @Override
    public Result<String> singleStopSimulation(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.fail(null, "设备列表为空");
        }
        ids.forEach(id -> {
            ScheduledFuture<?> future = deviceTasks.get(id);
            if (future != null) {
                future.cancel(false);
                deviceTasks.remove(id);
                runningDevices.remove(id);
            }
        });
        //这里同理
        pool.submit(() -> {
            LambdaUpdateWrapper<VirtualDevice> running = new LambdaUpdateWrapper<VirtualDevice>()
                    .in(VirtualDevice::getDeviceCode, ids)
                    .eq(VirtualDevice::getIsRunning, true)
                    .set(VirtualDevice::getIsRunning, false);
            deviceMapper.update(running);
        });
        return Result.ok(SuccessCode.DEVICE_STOP_SUCCESS.getCode(),
                SuccessCode.DEVICE_STOP_SUCCESS.getMessage());
    }

    @Override
    public Result<Map<String, String>> checkDeviceStatus(List<String> ids) {
        HashMap<String, String> map = new HashMap<>();
        //这个 map 标记哪些元素存在于 redis
        Map<String, String> map1 = ids.stream()
                .collect(Collectors.toMap(Function.identity(), id -> "UNKNOWN"));
        Iterator<Map.Entry<String, String>> it = map1.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            String status = redisTemplate.opsForValue().get(deviceStatusPrefix + entry.getKey());
            if (status != null) {
                map.put(entry.getKey(), status);
                it.remove();
            }
        }
        int offset = RANDOM.nextInt(120 + 1);
        map1.forEach((id, v) -> {
            VirtualDevice one = this.lambdaQuery()
                    .select(VirtualDevice::getStatus, VirtualDevice::getIsRunning)
                    .eq(VirtualDevice::getDeviceCode, id).one();
            String value = one.getStatus() + "," + one.getIsRunning();
            map.put(id, value);
            //加偏移量，防止缓存雪崩
            redisTemplate.opsForValue().set(deviceStatusPrefix + id, value, 180 + offset, TimeUnit.SECONDS);
        });
        return Result.ok(map);
    }

    @Override
    public Result<String> changeTime(int time) {
        redisTemplate.opsForValue().set("Time", String.valueOf(time));
        return Result.ok(SuccessCode.TIME_CHANGE_SUCCESS.getCode(),
                SuccessCode.TIME_CHANGE_SUCCESS.getMessage());
    }

    @Override
    public Result<String> changeSeason(int season) {
        redisTemplate.opsForValue().set("Season", String.valueOf(season));
        return Result.ok(SuccessCode.SEASON_CHANGE_SUCCESS.getCode(), SuccessCode.SEASON_CHANGE_SUCCESS.getMessage());
    }

    /**
     * 将设备列表里面的水表的水闸设置为关
     */
    @Override
    public Result<String> closeValue(List<String> ids) {
        ids.forEach(id -> redisTemplate.opsForSet().add("meter_closed", id));
        return Result.ok(SuccessCode.METER_CLOSE_SUCCESS.getCode(), SuccessCode.METER_CLOSE_SUCCESS.getMessage());
    }

    @Override
    public Result<String> open(List<String> ids) {
        redisTemplate.opsForSet().remove("meter_closed", ids.toArray());
        return Result.ok(SuccessCode.METER_OPEN_SUCCESS.getCode(), SuccessCode.METER_OPEN_SUCCESS.getMessage());
    }

    @Override
    public Result<String> openAllValue() {
        redisTemplate.delete("meter_closed");
        return Result.ok(SuccessCode.METER_OPEN_SUCCESS.getCode(), SuccessCode.METER_OPEN_SUCCESS.getMessage());
    }

    @Override
    public Result<String> closeAllValue() {
        Set<Object> keys = redisTemplate.opsForHash().keys("OnLineMap");
        List<String> list = keys.stream()
                .map(Object::toString)
                .filter(s -> !s.startsWith("2"))
                .toList();
        list.forEach(id -> redisTemplate.opsForSet().add("meter_closed", id));
        return Result.ok(SuccessCode.METER_CLOSE_SUCCESS.getCode(), SuccessCode.METER_CLOSE_SUCCESS.getMessage());
    }


    @Override
    public Result<String> destroyAll() {
        if (this.isRunning()) {
            return Result.fail(ErrorCode.DEVICE_CANT_RESET_ERROR.code(), ErrorCode.DEVICE_CANT_RESET_ERROR.message());
        }
        this.isInit = false;
        return Result.ok(SuccessCode.DEVICE_RESET_SUCCESS.getCode(), SuccessCode.DEVICE_RESET_SUCCESS.getMessage());
    }


    @Override
    public Result<String> offline(List<String> ids) {
        log.info("下线设备：{}", sanitizeForLog(ids.toString()));
        ids.forEach(runningDevices::remove);
        boolean updateResult = lambdaUpdate()
                .in(VirtualDevice::getDeviceCode, ids)
                .set(VirtualDevice::getStatus, "offline")
                .set(VirtualDevice::getIsRunning, false)
                .update();
        if (updateResult) {
            return Result.ok(SuccessCode.DEVICE_OFFLINE_SUCCESS.getCode(),
                    SuccessCode.DEVICE_OFFLINE_SUCCESS.getMessage());
        } else {
            return Result.fail(ErrorCode.SYSTEM_ERROR.code(), ErrorCode.SYSTEM_ERROR.message());
        }
    }

    @Override
    public Result<String> changeMode(String mode) {
        if (mode.equals("burstPipe") || mode.equals("leaking") || mode.equals("normal")) {
            redisTemplate.opsForValue().set("mode", mode);
            return Result.ok(SuccessCode.METER_MODE_CHANGE_SUCCESS.getCode(),
                    SuccessCode.METER_MODE_CHANGE_SUCCESS.getMessage());
        }
        return Result.fail(ErrorCode.PARAM_VALIDATION_ERROR.code(), ErrorCode.PARAM_VALIDATION_ERROR.message());
    }

    /**
     * 对日志内容进行简单清洗，防止换行等导致日志注入
     */
    private String sanitizeForLog(String input) {
        if (input == null) {
            return null;
        }
        // 去除回车和换行，防止伪造多行日志
        return input.replace('\r', ' ')
                .replace('\n', ' ');
    }

    // 单独开一个心跳任务
    private void startHeartbeat(String deviceId) {
        int time = Integer.parseInt(serverConfig.getMeterReportFrequency()) / 1000;
        int offset = Integer.parseInt(serverConfig.getMeterTimeOffset()) / 1000;
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            Long reportTime = this.reportTime.get(deviceId);
            if (reportTime == null || !runningDevices.contains(deviceId)) {
                return;
            }
            reportTime = now - reportTime >= 10_000L ? now : reportTime;
            try {
                dataSender.heartBeat(deviceId, reportTime);
            } catch (Exception e) {
                log.error("心跳发送异常: {}", e.getMessage(), e);
            }
        }, 0, time + offset, TimeUnit.SECONDS);
    }

    /**
     * 核心递归调度逻辑
     */
    private void scheduleNextReport(String deviceId) {
        String sanitizedDeviceId = sanitizeForLog(deviceId);
        // 检查全局控制位
        if (!runningDevices.contains(deviceId)) {
            log.info("设备 {} 不在运行集合中, 停止上报调度", sanitizedDeviceId);
            return;
        }
        String reportFrequency = serverConfig.getMeterReportFrequency();
        String timeOffset = serverConfig.getMeterTimeOffset();
        // 如果配置缺失，直接返回
        if (reportFrequency == null || timeOffset == null) {
            log.warn("设备 {} 上报配置缺失", sanitizedDeviceId);
            return;
        }
        // 计算随机上报延迟，打破设备集中上报
        long delay = Long.parseLong(reportFrequency) +
                ThreadLocalRandom.current().nextLong(Long.parseLong(timeOffset));
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                processSingleDevice(deviceId);
            } catch (Exception e) {
                log.error("设备 {} 数据上报失败: {}", sanitizedDeviceId, e.getMessage(), e);
            } finally {
                // 递归调度下一次上报
                reportTime.put(deviceId, System.currentTimeMillis());
                scheduleNextReport(deviceId);
            }
        }, delay, TimeUnit.MILLISECONDS);
        // 保存任务
        deviceTasks.put(deviceId, future);
    }

    /**
     * 判断设备是否在线
     */
    private boolean isDeviceOnline(String deviceId) {
        //先查询缓存
        String s = redisTemplate.opsForValue().get("device:OffLine:" + deviceId);
        String status = "online";
        if (s != null) {
            status = this.lambdaQuery().eq(VirtualDevice::getDeviceCode, deviceId)
                    .one().getStatus();
        }
        return status.equals("online");
    }

    /**
     * 生成单条模拟数据并执行发送
     *
     * @param id 设备编号
     */
    private void processSingleDevice(String id) {
        MeterDataBo dataBo = new MeterDataBo();
        dataBo.setDeviceId(id);
        dataBo.setDevice(1);
        int time = Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get("Time")));
        double flow = waterFlowGenerate(time, id);
        //得到正确水压
        double pressure = waterPressureGenerate(flow, serverConfig);
        // 时间戳微扰：增加纳秒级偏移，使排序更逼真
        dataBo.setTimeStamp(LocalDateTime.now().plusNanos(ThreadLocalRandom.current().nextInt(1000000)));
        dataBo.setFlow(flow);
        dataBo.setPressure(pressure);
        String season = Objects.requireNonNull(redisTemplate.opsForValue().get("Season"));
        int s = Integer.parseInt(season);
        int mid, step;
        //春季
        if (s == 1) {
            mid = 15;
            step = 3;
        }
        //夏季
        else if (s == 2) {
            mid = 22;
            step = 5;
        }
        //秋季
        else if (s == 3) {
            mid = 17;
            step = 2;
        }
        //冬季
        else {
            mid = 6;
            step = 2;
        }
        dataBo.setWaterTem(waterTemperateGenerate(time, mid, step));
        dataBo.setIsOpen(DeviceStatus.NORMAL);
        dataBo.setStatus(DeviceStatus.NORMAL);
        dataSender.sendMeterData(dataBo);
    }

    /**
     * 根据每天不同时段和不同的楼宇生成水流量的水流生成器
     */
    private double waterFlowGenerate(int time, String deviceId) {
        int buildingNum = Integer.parseInt(deviceId.substring(2, 4));
        double flow;
        if (buildingNum <= education) {
            flow = getEducationFlow(time, deviceId);
        } else if (buildingNum <= experiment) {
            flow = getExperimentFlow(time, deviceId);
        } else {
            flow = getDormitoryFlow(time, deviceId);
        }
        return keep3(flow);
    }

    Map<String, Integer> ids;
    Map<String, Integer> temp;
    volatile boolean flag = false;
    final Object lock = new Object();

    private double getDormitoryFlow(int time, String id) {
        double p = Math.random();
        int offset = ThreadLocalRandom.current().nextInt(10 * 60);
        double v = ThreadLocalRandom.current().nextDouble(1);
        //上报周期
        int fre = Integer.parseInt(serverConfig.getMeterReportFrequency()) / 1000;
        if (((time <= 7 * 3600) && (time >= 0)) || (time >= 23 * 3600) && (time <= 24 * 3600)) {
            if (p >= 0.8) {
                //模拟宿舍起夜上厕所行为，最多夜间两次
                int freCnt = 7 * 3600 / fre;
                double p1 = 1 - (2.0 / freCnt);
                if (v >= p1) {
                    return ThreadLocalRandom.current().nextDouble(0.1, 0.15);
                }
            }
        } else if (time > 7.25 * 3600 + offset && time <= 8 * 3600 + offset) {
            synchronized (lock) {
                if (!flag) {
                    ids = chooseDormitory(0.5);
                    temp = new HashMap<>(ids);
                    flag = true;
                }
            }
            if (p >= 0.93) {
                if (ids != null) {
                    synchronized (lock) {
                        if (ids.containsKey(id) && ids.get(id) > 0) {
                            Integer cnt = ids.get(id);
                            ids.put(id, --cnt);
                            return ThreadLocalRandom.current().nextDouble(0.15, 0.25);
                        }
                    }
                }
            }
        } else if (time > 8 * 3600 + offset && time <= 12.5 * 3600) {
            //剩余
            if (p >= 0.98) {
                synchronized (lock) {
                    if (temp.containsKey(id) && temp.get(id) > 0) {
                        Integer cnt = temp.get(id);
                        temp.put(id, --cnt);
                        return ThreadLocalRandom.current().nextDouble(0.1, 0.15);
                    } else return 0;
                }
            }
        } else if (time > 12.5 * 3600 && time <= 18 * 3600) {
            double freCnt = 7.5 * 3600 / fre;
            double p3 = 1 - (5.0 / freCnt);
            if (p >= p3) {
                return ThreadLocalRandom.current().nextDouble(0.05, 0.1);
            } else return 0;
        } else if (time >= 20 * 3600 && time <= 22 * 3600) {
            int freCnt = 3 * 3600 / fre;
            double p4 = 1 - (2.0 / freCnt);
            if (p >= p4) {
                return ThreadLocalRandom.current().nextDouble(0.1, 0.3);
            } else {
                return 0;
            }
        } else if (time >= 23.5 * 3600 && time <= 24 * 3600) {
            flag = true;
            return 0;
        } else {
            if (p > 0.9999) {
                return ThreadLocalRandom.current().nextDouble(0.02, 0.05);
            } else return 0;
        }
        return 0.0;
    }

    /**
     * 方法返回约总设备的0.v倍的设备数量
     */
    private Map<String, Integer> chooseDormitory(double v) {
        //宿舍楼的编号都在实验楼之后
        int firstIndex = Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get("device:experimentBuildings")));
        Long size;
        size = redisTemplate.opsForSet().size("device:meter");
        if (size != null) {
            Map<Object, Object> entries = redisTemplate.opsForHash().randomEntries("OnLineMap", (long) (size * v));
            if (entries != null) {
                return entries.keySet().stream()
                        .map(Object::toString)
                        .filter(id -> id.startsWith("1"))
                        .filter(id -> Integer.parseInt(id.substring(2, 4)) > firstIndex)
                        .collect(Collectors.toMap(id -> id, id -> 8));
            }
        }
        return null;
    }

    volatile boolean isExperimentGet = false;
    volatile boolean isEducationGet = false;
    volatile Set<String> educationIds;
    volatile Map<String, Integer> experimentIds;
    final Object lock1 = new Object();
    volatile boolean flag1 = false;
    volatile boolean flag2 = false;
    volatile boolean flag3 = false;
    volatile boolean flag4 = false;

    private double getExperimentFlow(int time, String deviceId) {
        //正在运行试验的教室，用水量可能一直存在
        double p = ThreadLocalRandom.current().nextDouble(1);
        if (time >= 8 * 3600 && time <= 12 * 3600) {
            //这里两次检查的目的是提高并发能力。只有一个线程是需要加锁的，其他线程无需加锁，因此在外部只需检查标志位
            if (!flag1) {
                synchronized (lock1) {
                    if (!flag1) {
                        assignRunnableDevice();
                        flag1 = true;
                    }
                }
            }
            if (p >= 0.95) {
                return returnFlow(deviceId, 0.01, 0.05);
            } else return 0;
        } else if (time > 12 * 3600 && time <= 15 * 3600) {
            if (!flag2) {
                synchronized (lock1) {
                    if (!flag2) {
                        assignRunnableDevice();
                        flag2 = true;
                    }
                }
            }
            if (p >= 0.95) {
                return returnFlow(deviceId, 0.01, 0.05);
            } else return 0;
        } else if (time > 15 * 3600 && time <= 18 * 3600) {
            if (!flag3) {
                synchronized (lock1) {
                    if (!flag3) {
                        assignRunnableDevice();
                        flag3 = true;
                    }
                }
            }
            if (p >= 0.95) {
                return returnFlow(deviceId, 0.1, 0.15);
            } else return 0;
        } else if (time > 18 * 3600 && time <= 22 * 3600) {
            if (!flag4) {
                synchronized (lock1) {
                    if (!flag4) {
                        assignRunnableDevice();
                        flag4 = true;
                    }
                }
            }
            if (p >= 0.95) {
                return returnFlow(deviceId, 0.05, 0.1);
            } else return 0;
        } else if (time >= 23 * 3600 && time <= 24 * 3600) {
            //重置标志位，以便第二天选取不同的运行集合
            reset();
            return 0;
        } else {
            return 0;
        }
    }

    private void reset() {
        flag1 = false;
        flag2 = false;
        flag3 = false;
        flag4 = false;
    }

    private double returnFlow(String deviceId, double origin, double bound) {
        Integer cnt = experimentIds.get(deviceId);
        if (cnt != null && cnt > 0) {
            experimentIds.put(deviceId, --cnt);
            return ThreadLocalRandom.current().nextDouble(origin, bound);
        } else return 0;
    }

    private void assignRunnableDevice() {
        synchronized (lock1) {
            if (!isExperimentGet) {
                //设备全量列表
                Set<String> members = Objects.requireNonNull(redisTemplate.opsForSet().members("device:meter"));
                //是实验楼的所有设备数量
                int canRunning = (int) members.stream().filter(id -> {
                    int buildingNum = Integer.parseInt(id.substring(2, 4));
                    return buildingNum > education && buildingNum <= experiment;
                }).count();
                //选择的所有运行设备
                experimentIds = members
                        .stream()
                        .filter(id -> {
                            int buildingNum = Integer.parseInt(id.substring(2, 4));
                            return buildingNum > education && buildingNum <= experiment;
                        })
                        .limit(Math.max(1, canRunning / 3))
                        .collect(Collectors.toMap(id -> id, id -> 60));
                isExperimentGet = true;
            }
        }
    }

    private double getEducationFlow(int time, String deviceId) {
        // 教学区活跃时段（以小时为单位，0-23）
        // 假设：早上 8-12 点，下午 14-20 点
        boolean isActiveTime = (time >= 8 * 3600 && time <= 12 * 3600) || (time >= 14 * 3600 && time <= 21 * 3600);
        // 教学区设备集合，只第一次获取
        synchronized (this) {
            if (!isEducationGet) {
                Set<String> members = Objects.requireNonNull(redisTemplate.opsForSet().members("device:meter"));
                int canRunning = (int) members.stream()
                        .filter(id -> {
                            int building = Integer.parseInt(id.substring(2, 4));
                            return building <= education;
                        })
                        .count();
                educationIds = members.stream()
                        .filter(id -> {
                            int building = Integer.parseInt(id.substring(2, 4));
                            return building <= education;
                        })
                        .limit(Math.max(1, canRunning / 2))
                        .collect(Collectors.toSet());
                isEducationGet = true;
            }
        }
        // 如果这个设备属于教学楼
        if (educationIds != null && educationIds.contains(deviceId)) {
            double p = ThreadLocalRandom.current().nextDouble(); // 0-1 随机概率
            if (!isActiveTime) {
                // 非活跃时间，绝大多数为 0
                if (p <= 0.99991) {
                    return 0.0;
                } else {
                    return ThreadLocalRandom.current().nextDouble(0.1, 0.15);
                }
            } else {
                // 活跃时间，偶尔有人使用水，流量低且离散
                if (p <= 0.999) {
                    return 0.0; // 大多数时间没用水
                } else {
                    return ThreadLocalRandom.current().nextDouble(0.05, 0.15);
                }
            }
        } else {
            // 非教学楼设备，不处理
            return 0.0;
        }
    }

    /**
     * 根据每天不同时段生成水温的水温生成器
     *
     * @param time 时间
     * @param mid  季节基准值
     * @param step 昼夜温差
     */
    private double waterTemperateGenerate(double time, double mid, double step) {
        double pi = Math.PI;

        // 一天 = 86400 秒，14 点 = 14 * 3600 秒
        double phi = (pi / 2) - (2 * pi * 14 * 3600 / 86400);

        return keep3(
                mid + step * Math.sin(2 * pi * time / 86400 + phi)
        );
    }


    /**
     * 初始化设备并入库（逻辑保持原样）
     */
    @Time
    @Override
    public Result<String> init(int buildings, int floors, int rooms, int dormitoryBuildings,
                               int educationBuildings, int experimentBuildings) throws InterruptedException {
        if (isRunning()) {
            return Result.fail(ErrorCode.DEVICE_DEVICE_RUNNING_NOW_ERROR.code(),
                    ErrorCode.DEVICE_DEVICE_RUNNING_NOW_ERROR.message());
        }
        if (isInit) {
            return Result.fail(ErrorCode.DEVICE_ALREADY_INIT_ERROR.code()
                    , ErrorCode.DEVICE_ALREADY_INIT_ERROR.message());
        }
        String prefix = "device:";
        clearRedisData(redisTemplate, deviceMapper);
        redisTemplate.opsForValue().set(prefix + "educationBuildings", String.valueOf(educationBuildings));
        redisTemplate.opsForValue().set(prefix + "experimentBuildings", String.valueOf(educationBuildings + experimentBuildings));
        DeviceIdList deviceIdList = initAllRedisData(buildings, floors, rooms, redisTemplate);

        List<String> meterDeviceIds = deviceIdList.getMeterDeviceIds();
        List<String> waterQualityDeviceIds = deviceIdList.getWaterQualityDeviceIds();

        List<VirtualDevice> meterList = new ArrayList<>();
        List<VirtualDevice> waterList = new ArrayList<>();

        build(meterDeviceIds, meterList, 1);
        build(waterQualityDeviceIds, waterList, 2);

        ExecutorService executor = Executors.newFixedThreadPool(5);
        VirtualMeterDeviceServiceImpl proxy = (VirtualMeterDeviceServiceImpl) AopContext.currentProxy();
        executor.submit(() -> proxy.saveBatch(meterList, 2000));
        executor.submit(() -> proxy.saveBatch(waterList, 2000));
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);
        this.isInit = true;
        waterQualityDeviceService.setInit(true);
        log.info("设备注册完成：校区 3 楼宇 {} 层数 {} 房间 {}", buildings, floors, rooms);
        this.allSize = buildings * floors * rooms;
        return Result.ok(SuccessCode.DEVICE_REGISTER_SUCCESS.getCode(),
                SuccessCode.DEVICE_REGISTER_SUCCESS.getMessage());
    }

    /**
     * 构建设备集合，方便存入数据库
     */
    private static void build(List<String> deviceIds, List<VirtualDevice> virtualDeviceList, int type) {
        Date now = new Date();
        deviceIds.forEach(id -> {
            VirtualDevice virtualDevice = new VirtualDevice();
            virtualDevice.setDeviceType(type);
            virtualDevice.setDeviceCode(id);
            String sn = "JX" + UUID.fastUUID().toString().substring(0, 18).toUpperCase();
            virtualDevice.setInstallDate(now);
            virtualDevice.setSnCode(sn);
            virtualDevice.setCampusNo(id.substring(1, 2));
            virtualDevice.setBuildingNo(id.substring(2, 4));
            virtualDevice.setFloorNo(id.substring(4, 6));
            virtualDevice.setRoomNo(id.substring(6, 9));
            virtualDevice.setStatus("online");
            virtualDevice.setIsRunning(false);
            virtualDeviceList.add(virtualDevice);
        });
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VirtualMeterDeviceServiceImpl that)) return false;
        return isInit == that.isInit && isRunning() == that.isRunning();
    }

    @Override
    public int hashCode() {
        return Objects.hash(isInit, isRunning());
    }

    private boolean isRunning() {
        return !runningDevices.isEmpty();
    }
}
