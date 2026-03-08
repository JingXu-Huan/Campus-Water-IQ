import axios from 'axios'

// AI 预测服务
const aiApiClient = axios.create({
    baseURL: 'http://localhost:18011',
    timeout: 15000,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: false,
})

// 请求拦截器
aiApiClient.interceptors.request.use((config: any) => {
    // 可选：添加 token 认证（如果需要）
    // const token = localStorage.getItem('auth-token')
    // if (token) {
    //     config.headers.Authorization = `Bearer ${token}`
    // }
    return config
})

// 响应拦截器
aiApiClient.interceptors.response.use(
    (response: any) => response.data,
    (error: any) => {
        console.error('AI API Error:', error)
        const message = error.response?.data?.message || '预测服务请求失败'
        return Promise.reject(new Error(message))
    }
)

// 用水量预测结果
export interface UsageVO {
    campus: number      // 校区
    usage: number       // 预测的用水量
}

export const aiApi = {
    /**
     * 预测某校区明天的用水量（自动获取近七天数据）
     * @param campus 校区 (1=花园, 2=龙子湖, 3=江淮)
     */
    predictTomorrowWaterUsage: async (campus: number): Promise<UsageVO> => {
        const res = await aiApiClient.post<any>('/ai/predictTomorrowWaterUsage', null, {
            params: { campus }
        })
        // 后端返回 Result<UsageVO>，data 字段才是 UsageVO
        return res.data
    },

    /**
     * 获取水质建议
     * @param score 水质分数
     * @param ph 酸碱度
     * @param ch 含氯量
     * @param th 浊度
     */
    getWaterQualitySuggestion: async (score: number, ph: number, ch: number, th: number): Promise<string> => {
        const res = await aiApiClient.post<any>('/ai/suggestionOfWater', null, {
            params: { score, ph, ch, th }
        })
        return res.data
    },

    /**
     * 获取节水建议
     */
    getWaterSavingSuggestions: async (): Promise<string> => {
        const res = await aiApiClient.post<any>('/ai/suggestions')
        return res.data
    }
}
