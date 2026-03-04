import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth-token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const message = error.response?.data?.message || '请求失败，请稍后重试'
    return Promise.reject(new Error(message))
  }
)

// 报修单状态
export type RepairStatus = 'DRAFT' | 'CONFIRMED' | 'PROCESSING' | 'DONE' | 'CANCELLED'

// 报修单详情
export interface RepairOrder {
  id: string
  deviceCode: string
  reportName: string
  contactInfo: string
  desc: string
  severity: number
  status: RepairStatus
  remark: string
  createdAt: string
  updatedAt: string
}

// 状态标签颜色映射
export const statusColors: Record<RepairStatus, { bg: string; text: string }> = {
  DRAFT: { bg: 'bg-gray-100', text: 'text-gray-600' },
  CONFIRMED: { bg: 'bg-blue-100', text: 'text-blue-600' },
  PROCESSING: { bg: 'bg-yellow-100', text: 'text-yellow-600' },
  DONE: { bg: 'bg-green-100', text: 'text-green-600' },
  CANCELLED: { bg: 'bg-red-100', text: 'text-red-600' },
}

// 状态中文映射
export const statusLabels: Record<RepairStatus, string> = {
  DRAFT: '草稿',
  CONFIRMED: '已确认',
  PROCESSING: '待确认',
  DONE: '已完成',
  CANCELLED: '已取消',
}

// 严重程度标签
export const severityLabels: Record<number, string> = {
  1: '轻微',
  2: '一般',
  3: '严重',
  4: '紧急',
}

export const severityColors: Record<number, { bg: string; text: string }> = {
  1: { bg: 'bg-green-100', text: 'text-green-600' },
  2: { bg: 'bg-yellow-100', text: 'text-yellow-600' },
  3: { bg: 'bg-orange-100', text: 'text-orange-600' },
  4: { bg: 'bg-red-100', text: 'text-red-600' },
}

export const repairApi = {
  // 按状态查询报修单
  getByStatus: (status: RepairStatus, pageNum = 1, pageSize = 10) => 
    api.get<{ code: string; data: RepairOrder[] }>('/operations/listByStatus', {
      params: { status, pageNum, pageSize }
    }),

  // 修改报修单状态
  changeStatus: (status: RepairStatus, deviceReservationId: string) =>
    api.get<{ code: string; data: boolean }>('/operations/changeStatus', {
      params: { status, deviceReservationId }
    }),

  // 查询未解决的报修单数量
  getUnclosedCount: () => 
    api.get<{ code: string; data: number }>('/operations/getAllUnClosedNums'),

  // 获取校园告警列表
  getCampusWarnings: async (campus: number): Promise<any[]> => {
    try {
        const res = await api.get<any[]>('/operations/getCampusWarings', {
            params: { campus }
        })
        return res?.data ?? res ?? []
    } catch (error) {
        console.error('获取校园告警失败:', error)
        return []
    }
}
}
export default repairApi
