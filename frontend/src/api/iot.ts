import axios from 'axios'

// IoT-device 服务 (楼宇配置)
const iotDeviceApi = axios.create({
  baseURL: 'http://127.0.0.1:18097',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: false,
})

// IoT-service 服务 (流量数据)
const iotDataApi = axios.create({
  baseURL: 'http://127.0.0.1:18016',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: false,
})

// 请求拦截器
iotDeviceApi.interceptors.request.use((config: any) => {
  const token = localStorage.getItem('auth-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

iotDataApi.interceptors.request.use((config: any) => {
  const token = localStorage.getItem('auth-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器
iotDeviceApi.interceptors.response.use(
  (response: any) => response.data,
  (error: any) => {
    console.error('API Error:', error)
    const message = error.response?.data?.message || '请求失败，请稍后重试'
    return Promise.reject(new Error(message))
  }
)

iotDataApi.interceptors.response.use(
  (response: any) => response.data,
  (error: any) => {
    console.error('API Error:', error)
    const message = error.response?.data?.message || '请求失败，请稍后重试'
    return Promise.reject(new Error(message))
  }
)

// IoT-device 响应拦截器
iotDeviceApi.interceptors.response.use(
  (response: any) => response.data,
  (error: any) => {
    console.error('API Error:', error)
    const message = error.response?.data?.message || error.response?.data?.msg || '请求失败，请稍后重试'
    return Promise.reject(new Error(message))
  }
)

export interface WaterUsageData {
  value: number
  timestamp?: string
}

export interface SchoolUsageParams {
  school: number
  start: string
  end: string
}

// 格式化日期为 yyyy-MM-dd HH:mm:ss
const formatDate = (date: Date): string => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

export interface UsageResponse {
  data: number
  yesterday?: number
  lastMonthSameDay?: number
}

// 设备编码解析
export interface DeviceCodeInfo {
  deviceType: number  // A: 1=水表, 2=传感器
  campusNo: number   // B: 1=花园, 2=龙子湖, 3=江淮
  buildingNo: string // CD: 01-99
  floorNo: string    // XY: 01-99
  unitNo: string     // ZZZ: 001-999
}

// 解析设备编码
// 严格校验: ^[1-2][1-3](0[1-9]|[1-9][0-9])(0[1-9]|[1-9][0-9])(00[1-9]|0[1-9][0-9]|[1-9][0-9]{2})$
export const parseDeviceCode = (code: string): DeviceCodeInfo | null => {
  if (!code || code.length !== 9) return null
  const match = code.match(/^([1-2])([1-3])(0[1-9]|[1-9][0-9])(0[1-9]|[1-9][0-9])(00[1-9]|0[1-9][0-9]|[1-9][0-9]{2})$/)
  if (!match) return null
  return {
    deviceType: parseInt(match[1]),
    campusNo: parseInt(match[2]),
    buildingNo: match[3],
    floorNo: match[4],
    unitNo: match[5]
  }
}

// 校验设备ID格式是否合法
export const isValidDeviceCode = (code: string): boolean => {
  return parseDeviceCode(code) !== null
}

// 生成设备ID (水表 = deviceType 1)
// 格式: deviceType(1) + campus(1) + building(2) + floor(2) + unit(3) = 9位
// 校验: buildingNo 1-99, floorNo 1-99, unitNo 1-999
export const generateDeviceId = (campusNo: number, buildingNo: number, floorNo: number, unitNo: number): string => {
  // 校验参数范围
  if (campusNo < 1 || campusNo > 3) throw new Error('campusNo must be 1-3')
  if (buildingNo < 1 || buildingNo > 99) throw new Error('buildingNo must be 1-99')
  if (floorNo < 1 || floorNo > 99) throw new Error('floorNo must be 1-99')
  if (unitNo < 1 || unitNo > 999) throw new Error('unitNo must be 1-999')
  return `1${campusNo}${String(buildingNo).padStart(2, '0')}${String(floorNo).padStart(2, '0')}${String(unitNo).padStart(3, '0')}`
}

// 生成水质传感器设备ID (deviceType = 2)
// 格式: deviceType(2) + campus(1) + building(2) + floor(2) + 001 = 9位
// 校验: buildingNo 1-99, floorNo 1-99
export const generateWaterQualitySensorId = (campusNo: number, buildingNo: number, floorNo: number): string => {
  // 校验参数范围
  if (campusNo < 1 || campusNo > 3) throw new Error('campusNo must be 1-3')
  if (buildingNo < 1 || buildingNo > 99) throw new Error('buildingNo must be 1-99')
  if (floorNo < 1 || floorNo > 99) throw new Error('floorNo must be 1-99')
  return `2${campusNo}${String(buildingNo).padStart(2, '0')}${String(floorNo).padStart(2, '0')}001`
}

// 楼宇类型
export type BuildingType = 'education' | 'experiment' | 'dormitory'

// 楼宇信息
export interface BuildingInfo {
  id: string
  type: BuildingType
  name: string
  buildingNo: number
  startIndex: number
}

// 设备实时数据
export interface DeviceFlowData {
  deviceId: string
  flow: number
  pressure?: number
  temperature?: number
  timestamp?: string
  status: 'online' | 'offline'
}

// 水质传感器数据
export interface WaterQualityData {
  deviceId: string
  turbidity: number      // 浊度 NTU
  ph: number           // pH值
  chlorine: number     // 含氯量 mg/L
  temperature: number  // 温度 °C
  status: 'online' | 'offline'
}

// 楼宇配置 VO
interface BuildingConfigResponse {
  educationStart: number
  experimentStart: number
  dormitoryStart: number
  totalBuildings: number
  floors?: number
  rooms?: number
}

// 获取楼宇配置（从后端获取）
export const getBuildingConfig = async (): Promise<{
  educationStart: number
  experimentStart: number
  dormitoryStart: number
  totalBuildings: number
  floors: number
  rooms: number
}> => {
  try {
    // 调用后端 API 获取真实的 Redis 配置
    const res = await iotDeviceApi.get<BuildingConfigResponse>('/device/buildingConfig')
    const data = res.data || res
    return {
      educationStart: data.educationStart || 1,
      experimentStart: data.experimentStart || 3,
      dormitoryStart: data.dormitoryStart || 4,
      totalBuildings: data.totalBuildings || 6,
      floors: data.floors || 6,
      rooms: data.rooms || 10
    }
  } catch (error) {
    console.error('获取楼宇配置失败，使用默认配置:', error)
    // 失败时返回默认配置
    return {
      educationStart: 1,
      experimentStart: 3,
      dormitoryStart: 4,
      totalBuildings: 6,
      floors: 6,
      rooms: 10
    }
  }
}

// 根据楼栋编号判断类型
export const getBuildingType = (
  buildingNo: number,
  educationStart: number,
  experimentStart: number
): BuildingType => {
  if (buildingNo <= educationStart) {
    return 'education'
  } else if (buildingNo <= experimentStart) {
    return 'experiment'
  } else {
    return 'dormitory'
  }
}

export const iotApi = {
  // 获取今日用水量
  getTodayUsage: async (school: number): Promise<UsageResponse> => {
    const today = new Date()
    const startOfDay = new Date(today.getFullYear(), today.getMonth(), today.getDate())
    const now = new Date()
    
    // 获取昨日同期数据用于对比
    const yesterday = new Date(startOfDay)
    yesterday.setDate(yesterday.getDate() - 1)
    const yesterdayStart = new Date(yesterday.getFullYear(), yesterday.getMonth(), yesterday.getDate())
    const yesterdayEnd = new Date(yesterday.getFullYear(), yesterday.getMonth(), yesterday.getDate(), 23, 59, 59)
    
    try {
      const [todayRes, yesterdayRes] = await Promise.all([
        iotDataApi.get<number>('/Data/schoolUsage', {
          params: { school, start: formatDate(startOfDay), end: formatDate(now) }
        }),
        iotDataApi.get<number>('/Data/schoolUsage', {
          params: { school, start: formatDate(yesterdayStart), end: formatDate(yesterdayEnd) }
        }).catch(() => ({ data: 0 }))
      ])
      
      const todayValue = todayRes?.data ?? todayRes ?? 0
      const yesterdayValue = yesterdayRes?.data ?? yesterdayRes ?? 0
      
      return {
        data: Number(todayValue),
        yesterday: Number(yesterdayValue)
      }
    } catch (error) {
      console.error('获取今日用水量失败:', error)
      return { data: 0, yesterday: 0 }
    }
  },
  
  // 获取本月用水量
  getMonthUsage: async (school: number): Promise<UsageResponse> => {
    const now = new Date()
    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1)
    
    // 获取上月同期数据
    const lastMonthStart = new Date(now.getFullYear(), now.getMonth() - 1, 1)
    const lastMonthNow = new Date(now.getFullYear(), now.getMonth() - 1, now.getDate(), now.getHours(), now.getMinutes())
    
    try {
      const [monthRes, lastMonthRes] = await Promise.all([
        iotDataApi.get<number>('/Data/schoolUsage', {
          params: { school, start: formatDate(startOfMonth), end: formatDate(now) }
        }),
        iotDataApi.get<number>('/Data/schoolUsage', {
          params: { school, start: formatDate(lastMonthStart), end: formatDate(lastMonthNow) }
        }).catch(() => ({ data: 0 }))
      ])
      
      const monthValue = monthRes?.data ?? monthRes ?? 0
      const lastMonthValue = lastMonthRes?.data ?? lastMonthRes ?? 0
      
      return {
        data: Number(monthValue),
        lastMonthSameDay: Number(lastMonthValue)
      }
    } catch (error) {
      console.error('获取本月用水量失败:', error)
      return { data: 0, lastMonthSameDay: 0 }
    }
  },
  
  // 批量获取用水量（推荐使用）
  getWaterUsage: async (school: number): Promise<{ todayUsage: UsageResponse; monthUsage: UsageResponse }> => {
    const [todayUsage, monthUsage] = await Promise.all([
      iotApi.getTodayUsage(school),
      iotApi.getMonthUsage(school)
    ])
    return { todayUsage, monthUsage }
  },

  // 获取离线设备列表
  getOfflineDeviceList: async (school: number): Promise<string[]> => {
    try {
      const res = await iotDataApi.get<string[]>('/Data/getCampusOffLineDeviceList', {
        params: { campus: school }
      })
      return res?.data || res || []
    } catch (error) {
      console.error('获取离线设备列表失败:', error)
      return []
    }
  },

  // 获取离线率
  getOfflineRate: async (): Promise<number> => {
    try {
      const res = await iotDataApi.get<number>('/Data/getOffLineRate')
      return res?.data ?? res ?? 0
    } catch (error) {
      console.error('获取离线率失败:', error)
      return 0
    }
  },

  // 获取某设备的最近一条水流量
  getFlowNow: async (deviceId: string): Promise<number> => {
    try {
      const res = await iotDataApi.get<number>('/Data/getFlowNow', {
        params: { deviceId }
      })
      return res?.data ?? res ?? 0
    } catch (error) {
      console.error(`获取设备流量失败 ${deviceId}:`, error)
      return 0
    }
  },

  // 批量获取设备状态
  getDeviceStatus: async (deviceIds: string[]): Promise<Record<string, boolean>> => {
    if (!deviceIds || deviceIds.length === 0) {
      return {}
    }
    try {
      const res = await iotDeviceApi.post<Record<string, string>>('/device/status', { ids: deviceIds })
      const data = res?.data ?? res ?? {}
      // 解析格式: 只要是online就算在线
      const statusMap: Record<string, boolean> = {}
      for (const [deviceId, statusStr] of Object.entries(data)) {
        statusMap[deviceId] = statusStr.startsWith('online')
      }
      return statusMap
    } catch (error) {
      console.error('获取设备状态失败:', error)
      // 返回空对象，全部视为离线
      return {}
    }
  },

  // 批量获取设备流量、压力和温度
  getBatchFlow: async (deviceIds: string[]): Promise<DeviceFlowData[]> => {
    try {
      // 获取设备状态
      const statusMap = await iotApi.getDeviceStatus(deviceIds)
      
      const results = await Promise.allSettled(
        deviceIds.map(async (deviceId) => {
          const isOnline = statusMap[deviceId] === true
          try {
            // 不管在线离线，都请求数据
            const [flow, pressure, temperature] = await Promise.all([
              iotDataApi.get<number>('/Data/getFlowNow', {
                params: { deviceId }
              }),
              iotDataApi.get<number>('/Data/getPressureNow', {
                params: { deviceId }
              }).catch(() => ({ data: 0 })),
              iotDataApi.get<number>('/Data/getTemNow', {
                params: { deviceId }
              }).catch(() => ({ data: 0 }))
            ])
            return {
              deviceId,
              flow: flow?.data ?? flow ?? 0,
              pressure: pressure?.data ?? pressure ?? 0,
              temperature: temperature?.data ?? temperature ?? 0,
              status: isOnline ? 'online' as const : 'offline' as const
            }
          } catch {
            return {
              deviceId,
              flow: 0,
              pressure: 0,
              temperature: 0,
              status: 'offline' as const
            }
          }
        })
      )
      return results.map((r, i) => r.status === 'fulfilled' ? r.value : { deviceId: deviceIds[i], flow: 0, status: 'offline' as const })
    } catch (error) {
      console.error('批量获取设备流量失败:', error)
      return deviceIds.map(id => ({ deviceId: id, flow: 0, status: 'offline' as const }))
    }
  },

  // 批量获取水质传感器数据
  getBatchWaterQuality: async (sensorIds: string[]): Promise<WaterQualityData[]> => {
    try {
      // 获取设备状态
      const statusMap = await iotApi.getDeviceStatus(sensorIds)
      
      const results = await Promise.allSettled(
        sensorIds.map(async (deviceId) => {
          const isOnline = statusMap[deviceId] === true
          try {
            const [turbidity, ph, chlorine, temperature] = await Promise.all([
              iotDataApi.get<number>('/Data/getTurbidity', {
                params: { deviceId }
              }).catch(() => ({ data: 0 })),
              iotDataApi.get<number>('/Data/getPh', {
                params: { deviceId }
              }).catch(() => ({ data: 0 })),
              iotDataApi.get<number>('/Data/getChlorine', {
                params: { deviceId }
              }).catch(() => ({ data: 0 })),
              iotDataApi.get<number>('/Data/getTemNow', {
                params: { deviceId }
              }).catch(() => ({ data: 0 }))
            ])
            return {
              deviceId,
              turbidity: turbidity?.data ?? turbidity ?? 0,
              ph: ph?.data ?? ph ?? 0,
              chlorine: chlorine?.data ?? chlorine ?? 0,
              temperature: temperature?.data ?? temperature ?? 0,
              status: isOnline ? 'online' as const : 'offline' as const
            }
          } catch {
            return {
              deviceId,
              turbidity: 0,
              ph: 0,
              chlorine: 0,
              temperature: 0,
              status: 'offline' as const
            }
          }
        })
      )
      return results.map((r, i) => r.status === 'fulfilled' ? r.value : { 
        deviceId: sensorIds[i], 
        turbidity: 0, 
        ph: 0, 
        chlorine: 0, 
        temperature: 0,
        status: 'offline' as const 
      })
    } catch (error) {
      console.error('批量获取水质数据失败:', error)
      return sensorIds.map(id => ({ 
        deviceId: id, 
        turbidity: 0, 
        ph: 0, 
        chlorine: 0, 
        temperature: 0,
        status: 'offline' as const 
      }))
    }
  },

  // 获取某设备的环比增长率
  getAnnulus: async (deviceId: string): Promise<number> => {
    try {
      const res = await iotDataApi.get<number>('/Data/getAnnulus', {
        params: { deviceId }
      })
      return res?.data ?? res ?? 0
    } catch (error) {
      console.error(`获取环比失败 ${deviceId}:`, error)
      return 0
    }
  },

  // 获取某校区在线设备数量
  getCampusOnlineDeviceCount: async (campus: number): Promise<number> => {
    try {
      const res = await iotDataApi.get<number>('/Data/getAnCampusOnLineDeviceNums', {
        params: { campus }
      })
      return res?.data ?? res ?? 0
    } catch (error) {
      console.error('获取在线设备数量失败:', error)
      return 0
    }
  },

  // 获取设备健康评分
  getHealthyScore: async (): Promise<number> => {
    try {
      const res = await iotDataApi.get<number>('/Data/healthyScoreOfDevices')
      // 处理响应可能是 { data: number } 或直接 number
      const value = res?.data ?? res ?? 0
      return typeof value === 'number' ? value : Number(value) || 0
    } catch (error) {
      console.error('获取健康评分失败:', error)
      return 0
    }
  },

  // ==================== 数字孪生相关API (IoT-device) ====================
  
  // 初始化设备
  initDevices: async (params: {
    dormitoryBuildings?: number
    educationBuildings?: number
    experimentBuildings?: number
    floors?: number
    rooms?: number
  } = {}): Promise<{ success: boolean; message: string }> => {
    try {
      const res = await iotDeviceApi.get('/device/init', { params }) as any
      return { 
        success: res?.code === '00000' || res?.code === '0' || res?.code === 'DEV_1002', 
        message: res?.message || res?.msg || '初始化成功' 
      }
    } catch (error: any) {
      console.error('初始化设备失败:', error)
      return { 
        success: false, 
        message: error?.message || '初始化失败' 
      }
    }
  },

  // 重置所有设备
  resetDevices: async (): Promise<{ success: boolean; message: string }> => {
    try {
      const res = await iotDeviceApi.get('/device/destroyAll') as any
      return { 
        success: res?.code === '00000' || res?.code === '0' || res?.code === 'DEV_1002', 
        message: res?.message || res?.msg || '重置成功' 
      }
    } catch (error: any) {
      console.error('重置设备失败:', error)
      return { 
        success: false, 
        message: error?.message || '重置失败' 
      }
    }
  },

  // 获取已开启设备数量
  getDeviceNums: async (): Promise<number> => {
    try {
      const res = await iotDeviceApi.get('/device/getDevicesNum') as any
      console.log('getDeviceNums response:', res)
      return res?.data ?? res ?? 0
    } catch (error) {
      console.error('获取设备数量失败:', error)
      return 0
    }
  },

  // 检查设备是否已初始化
  checkIsInitialized: async (): Promise<boolean> => {
    try {
      const res = await iotDeviceApi.get('/device/isInit') as any
      console.log('isInit response:', res, 'type:', typeof res)
      // 处理直接返回 true 或 { data: true } 两种格式
      if (typeof res === 'boolean') return res
      return res?.data === true || res?.data === 'true'
    } catch (error) {
      console.error('检查初始化状态失败:', error)
      return false
    }
  },

  // 获取任务运行状态
  getTaskStatus: async (): Promise<{ meterRunning: boolean; sensorRunning: boolean }> => {
    try {
      const res = await iotDeviceApi.get('/device/taskStatus') as any
      return {
        meterRunning: res?.data?.meterRunning ?? false,
        sensorRunning: res?.data?.sensorRunning ?? false
      }
    } catch (error) {
      console.error('获取任务状态失败:', error)
      return { meterRunning: false, sensorRunning: false }
    }
  },

  // 开启所有水表
  startAllMeters: async (): Promise<{ success: boolean; message: string }> => {
    try {
      const res = await iotDeviceApi.get('/simulator/meter/startAll') as any
      return { 
        success: res?.code === '00000' || res?.code === '0' || res?.code === 'DEV_1002', 
        message: res?.message || res?.msg || '水表开启成功' 
      }
    } catch (error: any) {
      console.error('开启水表失败:', error)
      return { 
        success: false, 
        message: error?.message || '开启水表失败' 
      }
    }
  },

  // 停止所有水表上报
  stopAllMeters: async (): Promise<{ success: boolean; message: string }> => {
    try {
      const res = await iotDeviceApi.get('/simulator/meter/endAll') as any
      return { 
        success: res?.code === '00000' || res?.code === '0' || res?.code === 'DEV_1002', 
        message: res?.message || res?.msg || '水表停止成功' 
      }
    } catch (error: any) {
      console.error('停止水表失败:', error)
      return { 
        success: false, 
        message: error?.message || '停止水表失败' 
      }
    }
  },

  // 开启所有水质传感器
  startAllSensors: async (): Promise<{ success: boolean; message: string }> => {
    try {
      const res = await iotDeviceApi.get('/simulator/quality/startAll') as any
      return { 
        success: res?.code === '00000' || res?.code === '0' || res?.code === 'DEV_1002', 
        message: res?.message || res?.msg || '传感器开启成功' 
      }
    } catch (error: any) {
      console.error('开启传感器失败:', error)
      return { 
        success: false, 
        message: error?.message || '开启传感器失败' 
      }
    }
  },

  // 停止所有水质传感器
  stopAllSensors: async (): Promise<{ success: boolean; message: string }> => {
    try {
      const res = await iotDeviceApi.get('/simulator/quality/stopAll') as any
      return { 
        success: res?.code === '00000' || res?.code === '0' || res?.code === 'DEV_1002', 
        message: res?.message || res?.msg || '传感器停止成功' 
      }
    } catch (error: any) {
      console.error('停止传感器失败:', error)
      return { 
        success: false, 
        message: error?.message || '停止传感器失败' 
      }
    }
  },

  // 水表下线
  offlineMeters: async (deviceIds: string[] = []): Promise<{ success: boolean; message: string }> => {
    try {
      const res = await iotDeviceApi.post('/simulator/meter/offLine', { ids: deviceIds }) as any
      return { 
        success: res?.code === '00000' || res?.code === '0' || res?.code === 'DEV_1002', 
        message: res?.message || res?.msg || '水表下线成功' 
      }
    } catch (error: any) {
      console.error('水表下线失败:', error)
      return { 
        success: false, 
        message: error?.message || '水表下线失败' 
      }
    }
  },

  // 传感器下线
  offlineSensors: async (deviceIds: string[] = []): Promise<{ success: boolean; message: string }> => {
    try {
      // 传感器下线用 POST /simulator/quality/stopAll
      const res = await iotDeviceApi.post('/simulator/quality/stopAll', { ids: deviceIds }) as any
      return { 
        success: res?.code === '00000' || res?.code === '0' || res?.code === 'DEV_1002', 
        message: res?.message || res?.msg || '传感器下线成功' 
      }
    } catch (error: any) {
      console.error('传感器下线失败:', error)
      return { 
        success: false, 
        message: error?.message || '传感器下线失败' 
      }
    }
  },

  // 批量开启水表
  startMeters: async (deviceIds: string[]): Promise<{ success: boolean; message: string }> => {
    try {
      const res = await iotDeviceApi.post('/simulator/meter/startList', { ids: deviceIds }) as any
      return { 
        success: res?.code === '00000' || res?.code === '0' || res?.code === 'DEV_1002', 
        message: res?.message || res?.msg || '水表开启成功' 
      }
    } catch (error: any) {
      console.error('开启水表失败:', error)
      return { 
        success: false, 
        message: error?.message || '开启水表失败' 
      }
    }
  },

  // 批量停止水表
  stopMeters: async (deviceIds: string[]): Promise<{ success: boolean; message: string }> => {
    try {
      const res = await iotDeviceApi.post('/simulator/meter/endList', { ids: deviceIds }) as any
      return { 
        success: res?.code === '00000' || res?.code === '0' || res?.code === 'DEV_1002', 
        message: res?.message || res?.msg || '水表停止成功' 
      }
    } catch (error: any) {
      console.error('停止水表失败:', error)
      return { 
        success: false, 
        message: error?.message || '停止水表失败' 
      }
    }
  },

  // 批量开启传感器
  startSensors: async (deviceIds: string[]): Promise<{ success: boolean; message: string }> => {
    try {
      const res = await iotDeviceApi.post('/simulator/quality/startList', { ids: deviceIds }) as any
      return { 
        success: res?.code === '00000' || res?.code === '0' || res?.code === 'DEV_1002', 
        message: res?.message || res?.msg || '传感器开启成功' 
      }
    } catch (error: any) {
      console.error('开启传感器失败:', error)
      return { 
        success: false, 
        message: error?.message || '开启传感器失败' 
      }
    }
  },

  // 批量停止传感器
  stopSensors: async (deviceIds: string[]): Promise<{ success: boolean; message: string }> => {
    try {
      const res = await iotDeviceApi.post('/simulator/quality/stopList', { ids: deviceIds }) as any
      return { 
        success: res?.code === '00000' || res?.code === '0' || res?.code === 'DEV_1002', 
        message: res?.message || res?.msg || '传感器停止成功' 
      }
    } catch (error: any) {
      console.error('停止传感器失败:', error)
      return { 
        success: false, 
        message: error?.message || '停止传感器失败' 
      }
    }
  },

  // 开启所有阀门
  openAllValves: async (): Promise<{ success: boolean; message: string }> => {
    try {
      const res = await iotDeviceApi.get('/simulator/meter/openAllValues') as any
      return { 
        success: res?.code === '00000' || res?.code === '0' || res?.code === 'DEV_1002', 
        message: res?.message || res?.msg || '阀门开启成功' 
      }
    } catch (error: any) {
      console.error('开启阀门失败:', error)
      return { 
        success: false, 
        message: error?.message || '开启阀门失败' 
      }
    }
  },

  // 关闭所有阀门
  closeAllValves: async (): Promise<{ success: boolean; message: string }> => {
    try {
      const res = await iotDeviceApi.get('/simulator/meter/closeAllValues') as any
      return { 
        success: res?.code === '00000' || res?.code === '0' || res?.code === 'DEV_1002', 
        message: res?.message || res?.msg || '阀门关闭成功' 
      }
    } catch (error: any) {
      console.error('关闭阀门失败:', error)
      return { 
        success: false, 
        message: error?.message || '关闭阀门失败' 
      }
    }
  },

  // 改变模拟模式
  changeSimulatorMode: async (mode: 'normal' | 'leaking' | 'burstPipe' | 'shows'): Promise<{ success: boolean; message: string }> => {
    try {
      const res = await iotDeviceApi.get('/device/changeModel', { params: { mode } }) as any
      return { 
        success: res?.code === '00000' || res?.code === '0' || res?.code === 'DEV_1002', 
        message: res?.message || res?.msg || '模式切换成功' 
      }
    } catch (error: any) {
      console.error('切换模式失败:', error)
      return { 
        success: false, 
        message: error?.message || '切换模式失败' 
      }
    }
  },

  // 获取楼宇配置
  getBuildingConfig: async (): Promise<{
    educationStart: number
    experimentStart: number
    dormitoryStart: number
    totalBuildings: number
    floors: number
    rooms: number
  }> => {
    try {
      const res = await iotDeviceApi.get('/device/buildingConfig')
      const data = res?.data || res
      return {
        educationStart: data?.educationStart || 1,
        experimentStart: data?.experimentStart || 3,
        dormitoryStart: data?.dormitoryStart || 4,
        totalBuildings: data?.totalBuildings || 6,
        floors: data?.floors || 6,
        rooms: data?.rooms || 10
      }
    } catch (error) {
      console.error('获取楼宇配置失败:', error)
      return {
        educationStart: 1,
        experimentStart: 3,
        dormitoryStart: 4,
        totalBuildings: 6,
        floors: 6,
        rooms: 10
      }
    }
  }
}
