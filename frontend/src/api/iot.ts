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
export const parseDeviceCode = (code: string): DeviceCodeInfo | null => {
  if (!code || code.length !== 9) return null
  const match = code.match(/^(\d)(\d)(\d{2})(\d{2})(\d{3})$/)
  if (!match) return null
  return {
    deviceType: parseInt(match[1]),
    campusNo: parseInt(match[2]),
    buildingNo: match[3],
    floorNo: match[4],
    unitNo: match[5]
  }
}

// 生成设备ID (水表 = deviceType 1)
// 格式: deviceType(1) + campus(1) + building(2) + floor(2) + unit(3) = 9位
export const generateDeviceId = (campusNo: number, buildingNo: number, floorNo: number, unitNo: number): string => {
  // campusNo 不需要补零 (1-3)
  return `1${campusNo}${String(buildingNo).padStart(2, '0')}${String(floorNo).padStart(2, '0')}${String(unitNo).padStart(3, '0')}`
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
  }
}

export default iotApi
