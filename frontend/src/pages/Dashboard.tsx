import {useNavigate} from 'react-router-dom'
import {useAuthStore} from '@/store/authStore'
import {
    Droplets,
    LogOut,
    User,
    BarChart3,
    AlertTriangle,
    Settings,
    LayoutDashboard,
    Activity,
    Map,
    FileText,
    HelpCircle,
    Menu,
    X,
    RefreshCw,
    TrendingUp,
    TrendingDown,
    WifiOff,
    Camera,
    Eye,
    EyeOff,
    Check,
    Wrench,
    Sun,
    Lightbulb,
    MessageCircle,
    Send,
    Bot,
    Loader2
} from 'lucide-react'
import {useState, useEffect, useRef} from 'react'
import {iotApi, generateDeviceId} from '@/api/iot'
import {aiApi} from '@/api/ai'
import {authApi} from '@/api/auth'
import {BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, LineChart, Line} from 'recharts'
import repair from "@/api/repair.ts";

export default function Dashboard() {
    const navigate = useNavigate()
    const {clearAuth, uid, nickname, avatar, updateProfile} = useAuthStore()
    const [sidebarOpen, setSidebarOpen] = useState(true)
    const [activeMenu, setActiveMenu] = useState('dashboard')
    const [selectedCampus, setSelectedCampus] = useState('longzi')

    // Profile modal state
    const [showProfileModal, setShowProfileModal] = useState(false)
    const [profileTab, setProfileTab] = useState<'info' | 'password' | 'avatar'>('info')
    const [editingNickname, setEditingNickname] = useState('')
    const [newPassword, setNewPassword] = useState('')
    const [oldPassword, setOldPassword] = useState('')
    const [showPassword, setShowPassword] = useState(false)
    const [profileLoading, setProfileLoading] = useState(false)
    const [profileMessage, setProfileMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null)

    // AI 聊天机器人状态
    const [showChatBot, setShowChatBot] = useState(false)
    const [chatMessages, setChatMessages] = useState<{ role: 'user' | 'assistant'; content: string }[]>([
        {
            role: 'assistant',
            content: '你好！我是校园用水数据分析助手，可以帮你查询各校区、楼宇的用水数据。有什么想了解的吗？'
        }
    ])
    const [chatInput, setChatInput] = useState('')
    const [chatLoading, setChatLoading] = useState(false)
    const chatEndRef = useRef<HTMLDivElement>(null)

    const campuses = [
        {id: 'huayuan', name: '花园校区', code: 'HY', schoolId: 1, city: '郑州', lat: 34.76, lon: 113.62},
        {id: 'longzi', name: '龙子湖校区', code: 'LZ', schoolId: 2, city: '郑州', lat: 34.76, lon: 113.62},
        {id: 'jianghuai', name: '江淮校区', code: 'JH', schoolId: 3, city: '信阳', lat: 32.13, lon: 114.09}
    ]

    // 天气状态
    const [weather, setWeather] = useState<{
        temp: number;
        condition: string;
        humidity: number;
        wind: string
    } | null>(null)
    const [weatherLoading, setWeatherLoading] = useState(false)

    const [todayUsage, setTodayUsage] = useState<number>(0)
    const [yesterdayUsage, setYesterdayUsage] = useState<number>(0)
    const [monthUsage, setMonthUsage] = useState<number>(0)
    const [lastMonthSameDay, setLastMonthSameDay] = useState<number>(0)
    const [buildingStats, setBuildingStats] = useState<{
        name: string;
        flow: number;
        pressure: number;
        status: string;
        onlineCount: number;
        totalCount: number
    }[]>([])
    const [, setLoadingBuildings] = useState(false)
    const [alertCount, setAlertCount] = useState<number>(0)
    const [deviceCount, setDeviceCount] = useState<number>(0)
    const [offlineDevices, setOfflineDevices] = useState<string[]>([])
    const [offlineRate, setOfflineRate] = useState<number>(0)
    const [healthyScore, setHealthyScore] = useState<number>(0)
    const [loading, setLoading] = useState<boolean>(true)

    // 告警数据
    const [warnings, setWarnings] = useState<{
        id: string
        deviceCode: string
        eventDesc: string
        eventLevel: string
        deviceType: string
        eventTime: string
    }[]>([])
    const [loadingWarnings, setLoadingWarnings] = useState(false)

    // 校区用水占比
    const [campusRate, setCampusRate] = useState<{ name: string; value: number }[]>([])
    const [loadingCampusRate, setLoadingCampusRate] = useState<boolean>(true)

    // 区域用水占比（宿舍、教学、实验）
    const [regionRate, setRegionRate] = useState<{ name: string; value: number }[]>([])
    const [loadingRegionRate, setLoadingRegionRate] = useState<boolean>(true)

    // 用水波动指数
    const [waterSwings, setWaterSwings] = useState<{
        school_1: number | null;
        school_2: number | null;
        school_3: number | null
    } | null>(null)
    const [loadingWaterSwings, setLoadingWaterSwings] = useState<boolean>(true)

    // 夜间异常用水量
    const [unNormalUsage, setUnNormalUsage] = useState<number>(0)
    const [loadingUnNormal, setLoadingUnNormal] = useState<boolean>(true)

    // 卡片提示信息
    const [activeTooltip, setActiveTooltip] = useState<string | null>(null)
    const cardTooltips: Record<string, string> = {
        'todayUsage': '统计当日00:00至当前时间的总用水量。',
        'monthUsage': '统计当月1日00:00至当前时间的总用水量。',
        'prediction': '基于历史用水数据，使用AI模型预测明日用水量。(此数据可能需要一定时间才能确定)',
        'waterSwings': '反映各校区用水量的波动程度。数值越大表示用水变化越剧烈，可用于检测异常用水情况(此数据可能需要一定时间才能确定)',
        'alerts': '显示系统目前自检的设备告警信息，包括水表异常、传感器故障等，不包括用户的报修信息。',
        'devices': '显示当前在线的IoT设备数量及离线率',
        'healthScore': '综合评估所有设备的运行状态，满分100分。'
    }

    // 渲染卡片提示图标（带点击提示）
    const CardTooltipIcon = ({id}: { id: string }) => (
        <span
            className="cursor-help text-gray-400 hover:text-blue-500"
            onClick={(e) => {
                e.stopPropagation();
                setActiveTooltip(activeTooltip === id ? null : id)
            }}
        >
      <HelpCircle className="w-3 h-3"/>
    </span>
    )

    // 设备类型中文映射
    const deviceTypeMap: Record<string, string> = {
        'METER': '水表告警',
        '1': '水表',
        '2': '水质传感器'
    }

    // 图表数据 - 近7天用水趋势
    const [weeklyUsageData, setWeeklyUsageData] = useState([
        {day: '周一', usage: 0},
        {day: '周二', usage: 0},
        {day: '周三', usage: 0},
        {day: '周四', usage: 0},
        {day: '周五', usage: 0},
        {day: '周六', usage: 0},
        {day: '周日', usage: 0}
    ])

    // 各校区用水占比 - 使用真实API数据
    const campusUsageData = campusRate.length > 0 ? campusRate.map(c => ({
        name: c.name.replace('校区', ''),
        usage: c.value
    })) : [
        {name: '花园', usage: 0},
        {name: '龙子湖', usage: 0},
        {name: '江淮', usage: 0}
    ]
    const [loadingToday, setLoadingToday] = useState<boolean>(true)
    const [loadingMonth, setLoadingMonth] = useState<boolean>(true)
    const [loadingOffline, setLoadingOffline] = useState<boolean>(true)
    const [loadingPrediction, setLoadingPrediction] = useState<boolean>(false)
    const [predictedTomorrowUsage, setPredictedTomorrowUsage] = useState<number | null>(null)
    const [waterSuggestion, setWaterSuggestion] = useState<string | null>(null)
    const [highUsageTimes, setHighUsageTimes] = useState<string[]>([])
    const [error, setError] = useState<string | null>(null)

    const handleLogout = () => {
        clearAuth()
        navigate('/login')
    }

    // 手动刷新
    const handleRefresh = () => {
        fetchWaterUsageData()
        fetchBuildingStats()
        fetchUnNormalUsage()
        if (currentCampus) {
            fetchPrediction(currentCampus.schoolId)
            fetchRegionRate()
        }
    }

    const currentCampus = campuses.find(c => c.id === selectedCampus)

    // 计算变化率
    const calculateChange = (current: number, previous: number): { value: number; isPositive: boolean } => {
        if (previous === 0) return {value: 0, isPositive: true}
        const change = ((current - previous) / previous) * 100
        return {value: Math.abs(change), isPositive: change >= 0}
    }

    const todayChange = calculateChange(todayUsage, yesterdayUsage)
    const monthChange = calculateChange(monthUsage, lastMonthSameDay)

    // 格式化数字
    const formatUsage = (value: number): string => {
        if (value >= 1000) {
            return `${(value / 1000).toFixed(2)}k`
        }
        return value.toFixed(1)
    }

    // 获取当前校区的用水量数据
    const fetchWaterUsageData = async () => {
        if (!currentCampus) return

        setLoading(true)
        setLoadingToday(true)
        setLoadingMonth(true)
        setLoadingWarnings(true)
        setError(null)

        try {
            // 使用 Promise.all 并行获取数据
            const {todayUsage: todayData, monthUsage: monthData} = await iotApi.getWaterUsage(currentCampus.schoolId)

            setTodayUsage(todayData.data ?? 0)
            setYesterdayUsage(todayData.yesterday || 0)
            setMonthUsage(monthData.data ?? 0)
            setLastMonthSameDay(monthData.lastMonthSameDay ?? 0)
        } catch (err) {
            console.error('获取用水量数据失败:', err)
            setError('获取用水量数据失败')
        } finally {
            setLoadingToday(false)
            setLoadingMonth(false)
            setLoading(false)

            // 从 iot-service 获取在线设备数量
            try {
                const onlineCount = await iotApi.getCampusOnlineDeviceCount(currentCampus.schoolId)
                setDeviceCount(onlineCount)
            } catch (err) {
                console.error('获取在线设备数量失败:', err)
                setDeviceCount(0)
            }

            // 获取离线率
            try {
                const rate = await iotApi.getOfflineRate()
                setOfflineRate(rate)
            } catch (err) {
                console.error('获取离线率失败:', err)
                setOfflineRate(0)
            }

            // 获取设备健康评分
            try {
                const score = await iotApi.getHealthyScore()
                setHealthyScore(score)
            } catch (err) {
                console.error('获取健康评分失败:', err)
                setHealthyScore(0)
            }

            // 获取校园告警
            try {
                const warningList = await repair.getCampusWarnings(currentCampus.schoolId)
                setWarnings(warningList || [])
                setAlertCount(warningList?.length || 0)
            } catch (err) {
                console.error('获取告警列表失败:', err)
                setWarnings([])
            } finally {
                setLoadingWarnings(false)
            }
        }

        // 获取离线设备列表
        const fetchOfflineDevices = async () => {
            if (!currentCampus) return

            setLoadingOffline(true)
            try {
                const offlineList = await iotApi.getOfflineDeviceList(currentCampus.schoolId)
                setOfflineDevices(offlineList)
            } catch (err) {
                console.error('获取离线设备列表失败:', err)
                setOfflineDevices([])
            } finally {
                setLoadingOffline(false)
            }
        }

        fetchOfflineDevices()
    }

    // 获取明日用水量预测
    const fetchPrediction = async (schoolId: number) => {
        setLoadingPrediction(true)
        try {
            // 调用预测接口（自动从 IoT-service 获取近七天数据）
            const result = await aiApi.predictTomorrowWaterUsage(schoolId)
            setPredictedTomorrowUsage(result?.usage ?? 0)
        } catch (err) {
            console.error('获取预测数据失败:', err)
            setPredictedTomorrowUsage(null)
        } finally {
            setLoadingPrediction(false)
        }
    }

    // 获取节水建议
    const fetchWaterSuggestion = async () => {
        try {
            const result = await aiApi.getWaterSavingSuggestions()
            setWaterSuggestion(result ?? null)
        } catch (err) {
            console.error('获取节水建议失败:', err)
            setWaterSuggestion(null)
        }
    }

    // 获取高峰用水时段
    const fetchHighUsageTimes = async (schoolId: number) => {
        try {
            const times = await iotApi.getHighWaterUsageTime(schoolId)
            setHighUsageTimes(times || [])
        } catch (err) {
            console.error('获取高峰用水时段失败:', err)
            setHighUsageTimes([])
        }
    }

    // 获取校区用水占比
    const fetchCampusRate = async () => {
        setLoadingCampusRate(true)
        try {
            const res = await iotApi.getCampusRate()
            console.log('校区占比API返回:', res)
            // API 直接返回数据对象 {1: 0.08, 2: 0.83, 3: 0.08}
            if (res && res[1] !== undefined) {
                const rateData = [
                    {name: '花园校区', value: Number((res[1] * 100).toFixed(1))},
                    {name: '龙子湖校区', value: Number((res[2] * 100).toFixed(1))},
                    {name: '江淮校区', value: Number((res[3] * 100).toFixed(1))}
                ]
                console.log('设置校区占比数据:', rateData)
                setCampusRate(rateData)
            }
        } catch (err) {
            console.error('获取校区占比失败:', err)
        } finally {
            setLoadingCampusRate(false)
        }
    }

    // 获取区域用水占比（宿舍、教学、实验）
    const fetchRegionRate = async () => {
        if (!currentCampus) return
        setLoadingRegionRate(true)
        try {
            // 区域: 1=教学楼, 2=实验楼, 3=宿舍楼
            const regions = [
                {id: 1, name: '教学楼'},
                {id: 2, name: '实验楼'},
                {id: 3, name: '宿舍楼'}
            ]

            const ratePromises = regions.map(async (region) => {
                const rate = await iotApi.getUsageRateInCampus(region.id, currentCampus.schoolId)
                return {
                    name: region.name,
                    value: Number((rate * 100).toFixed(1))
                }
            })

            const rateData = await Promise.all(ratePromises)
            console.log('区域占比数据:', rateData)
            setRegionRate(rateData)
        } catch (err) {
            console.error('获取区域占比失败:', err)
            // 设置默认值
            setRegionRate([
                {name: '宿舍楼', value: 0},
                {name: '教学楼', value: 0},
                {name: '实验楼', value: 0}
            ])
        } finally {
            setLoadingRegionRate(false)
        }
    }

    // 获取用水波动指数
    const fetchWaterSwings = async () => {
        setLoadingWaterSwings(true)
        try {
            const data = await iotApi.getWaterSwings()
            setWaterSwings(data)
        } catch (err) {
            console.error('获取用水波动指数失败:', err)
        } finally {
            setLoadingWaterSwings(false)
        }
    }

    // 获取夜间异常用水量
    const fetchUnNormalUsage = async () => {
        if (!currentCampus) return
        setLoadingUnNormal(true)
        try {
            const data = await iotApi.getUnNormalUsage(currentCampus.schoolId)
            setUnNormalUsage(data)
        } catch (err) {
            console.error('获取夜间异常用水量失败:', err)
        } finally {
            setLoadingUnNormal(false)
        }
    }

    // 获取本周用水趋势
    const fetchWeeklyTrends = async () => {
        if (!currentCampus) return
        setLoadingWaterSwings(true)
        try {
            const data = await iotApi.getWaterTrendsForTheWeek(currentCampus.schoolId)
            // 有数据时更新，否则重置为空数据状态
            if (data && data.length > 0) {
                setWeeklyUsageData(data)
            } else {
                setWeeklyUsageData([
                    {day: '周一', usage: 0},
                    {day: '周二', usage: 0},
                    {day: '周三', usage: 0},
                    {day: '周四', usage: 0},
                    {day: '周五', usage: 0},
                    {day: '周六', usage: 0},
                    {day: '周日', usage: 0}
                ])
            }
        } catch (err) {
            console.error('获取本周用水趋势失败:', err)
        } finally {
            setLoadingWaterSwings(false)
        }
    }

    // 打开个人中心
    const openProfileModal = () => {
        setEditingNickname(nickname || '')
        setOldPassword('')
        setNewPassword('')
        setProfileMessage(null)
        setShowProfileModal(true)
    }

    // 更新昵称
    const handleUpdateNickname = async () => {
        if (!editingNickname.trim()) {
            setProfileMessage({type: 'error', text: '昵称不能为空'})
            return
        }
        setProfileLoading(true)
        setProfileMessage(null)
        try {
            await authApi.updateNickname(editingNickname.trim(), uid || '')
            updateProfile(editingNickname.trim())
            setProfileMessage({type: 'success', text: '昵称更新成功'})
        } catch (error: any) {
            setProfileMessage({type: 'error', text: error.message || '更新失败'})
        } finally {
            setProfileLoading(false)
        }
    }

    // 更新密码
    const handleUpdatePassword = async () => {
        if (!oldPassword || !newPassword) {
            setProfileMessage({type: 'error', text: '请填写完整密码信息'})
            return
        }
        if (newPassword.length < 6) {
            setProfileMessage({type: 'error', text: '新密码至少6位'})
            return
        }
        setProfileLoading(true)
        setProfileMessage(null)
        try {
            await authApi.updatePassword({oldPassword, newPassword})
            setOldPassword('')
            setNewPassword('')
            setProfileMessage({type: 'success', text: '密码更新成功'})
        } catch (error: any) {
            setProfileMessage({type: 'error', text: error.message || '更新失败'})
        } finally {
            setProfileLoading(false)
        }
    }

    // 更新头像
    const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0]
        if (!file) return

        setProfileLoading(true)
        setProfileMessage(null)
        try {
            const uidStr = uid || ''
            await authApi.uploadAvatar(file, uidStr)
            // 获取新头像地址
            const res = await authApi.getAvatar(uidStr) as any
            if (res?.data) {
                updateProfile(undefined, res.data)
            }
            setProfileMessage({type: 'success', text: '头像更新成功'})
        } catch (error: any) {
            setProfileMessage({type: 'error', text: error.message || '更新失败'})
        } finally {
            setProfileLoading(false)
        }
    }

    // 获取天气数据
    const fetchWeather = async (lat: number, lon: number) => {
        setWeatherLoading(true)
        try {
            // 使用 Open-Meteo 免费天气 API（无需 API key，国内速度快）
            const res = await fetch(
                `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&timezone=Asia/Shanghai`
            )
            const data = await res.json()

            if (data.current) {
                const c = data.current
                // WMO 天气代码转换
                const weatherCode = c.weather_code
                const conditions: Record<number, string> = {
                    0: '晴', 1: '晴间多云', 2: '多云', 3: '阴',
                    45: '雾', 48: '霜雾',
                    51: '小毛毛雨', 53: '中雨', 55: '大雨',
                    61: '小雨', 63: '中雨', 65: '大雨',
                    71: '小雪', 73: '中雪', 75: '大雪',
                    80: '阵雨', 81: '阵雨', 82: '强阵雨',
                    95: '雷暴', 96: '雷暴+冰雹', 99: '强雷暴'
                }

                setWeather({
                    temp: Math.round(c.temperature_2m),
                    condition: conditions[weatherCode] || '未知',
                    humidity: c.relative_humidity_2m,
                    wind: Math.round(c.wind_speed_10m) + ' km/h'
                })
            }
        } catch (error) {
            console.error('获取天气失败:', error)
            setWeather(null)
        } finally {
            setWeatherLoading(false)
        }
    }

    // 获取楼宇实时数据
    const fetchBuildingStats = async () => {
        if (!currentCampus) return
        setLoadingBuildings(true)
        try {
            const buildingTypes = [
                {name: '宿舍楼', type: 1, buildings: 3},
                {name: '教学楼', type: 2, buildings: 2},
                {name: '实验楼', type: 3, buildings: 1}
            ]

            const stats: typeof buildingStats = []

            for (const building of buildingTypes) {
                try {
                    let totalFlow = 0
                    let totalPressure = 0
                    let totalOnline = 0
                    let totalDevices = 0
                    let validBuildings = 0

                    for (let b = 1; b <= building.buildings; b++) {
                        const deviceIds = [
                            generateDeviceId(currentCampus.schoolId, b, 1, 1),
                            generateDeviceId(currentCampus.schoolId, b, 2, 1),
                            generateDeviceId(currentCampus.schoolId, b, 3, 1),
                        ]

                        const status = await iotApi.getDeviceStatus(deviceIds)
                        const onlineIds = Object.entries(status).filter(([, v]) => v).map(([k]) => k)

                        if (onlineIds.length > 0) {
                            const data = await iotApi.getBatchFlow(onlineIds)
                            const onlineData = data.filter(d => d.status === 'online')
                            if (onlineData.length > 0) {
                                // 流量是总和，水压是平均
                                const buildingFlow = onlineData.reduce((s, d) => s + d.flow, 0)
                                const buildingPressure = onlineData.reduce((s, d) => s + (d.pressure || 0), 0) / onlineData.length
                                totalFlow += buildingFlow
                                totalPressure += buildingPressure
                                validBuildings++
                            }
                            totalOnline += onlineData.length
                        }
                        totalDevices += deviceIds.length
                    }

                    if (validBuildings > 0) {
                        stats.push({
                            name: building.name,
                            flow: totalFlow / building.buildings,
                            pressure: totalPressure / building.buildings,
                            status: '正常',
                            onlineCount: totalOnline,
                            totalCount: totalDevices
                        })
                    } else {
                        stats.push({
                            name: building.name,
                            flow: 0,
                            pressure: 0,
                            status: '离线',
                            onlineCount: 0,
                            totalCount: totalDevices
                        })
                    }
                } catch (e) {
                    console.error(`获取${building.name}数据失败:`, e)
                    stats.push({
                        name: building.name,
                        flow: 0,
                        pressure: 0,
                        status: '离线',
                        onlineCount: 0,
                        totalCount: 9
                    })
                }
            }

            setBuildingStats(stats)
        } catch (err) {
            console.error('获取楼宇数据失败:', err)
        } finally {
            setLoadingBuildings(false)
        }
    }

    // 清除单个告警
    const dismissWarning = async (id: string) => {
        try {
            await iotApi.dismissWarning([id])
            setWarnings(warnings.filter(w => w.id !== id))
            setAlertCount(Math.max(0, alertCount - 1))
        } catch (err) {
            console.error('清除告警失败:', err)
        }
    }

    // 清除所有告警
    const dismissAllWarnings = async () => {
        if (warnings.length === 0) return
        try {
            const ids = warnings.map(w => w.id)
            await iotApi.dismissWarning(ids)
            setWarnings([])
            setAlertCount(0)
        } catch (err) {
            console.error('清除所有告警失败:', err)
        }
    }

    // 发送聊天消息
    const handleSendMessage = async () => {
        if (!chatInput.trim() || chatLoading) return

        const userMessage = chatInput.trim()
        setChatInput('')
        setChatMessages(prev => [...prev, {role: 'user', content: userMessage}])
        setChatLoading(true)

        try {
            const response = await aiApi.chatWithAgent(userMessage)
            setChatMessages(prev => [...prev, {role: 'assistant', content: response || '抱歉，我暂时无法回答这个问题。'}])
        } catch (error: any) {
            console.error('聊天失败:', error)
            setChatMessages(prev => [...prev, {role: 'assistant', content: '抱歉，出了点问题，请稍后重试。'}])
        } finally {
            setChatLoading(false)
        }
    }

    // 聊天消息滚动到底部
    useEffect(() => {
        chatEndRef.current?.scrollIntoView({behavior: 'smooth'})
    }, [chatMessages])

    useEffect(() => {
        fetchWaterUsageData()
        fetchBuildingStats()
        fetchWaterSuggestion()
        fetchCampusRate()
        fetchWaterSwings()
        fetchUnNormalUsage()
        fetchWeeklyTrends()
        const campus = campuses.find(c => c.id === selectedCampus)
        if (campus) {
            fetchWeather(campus.lat, campus.lon)
            fetchPrediction(campus.schoolId)
            fetchHighUsageTimes(campus.schoolId)
            fetchRegionRate()
        }
    }, [selectedCampus])

    const menuItems = [
        {id: 'dashboard', label: '仪表盘', icon: LayoutDashboard, path: '/dashboard'},
        {id: 'monitoring', label: '实时监测', icon: Activity, path: '/monitoring'},
        {id: 'digital-twin', label: '数字孪生', icon: Map, path: '/digital-twin'},
        {id: 'repair', label: '报修管理', icon: Wrench, path: '/repair'},
        {id: 'reports', label: '数据报表', icon: FileText, path: '/reports'},
        {id: 'settings', label: '系统设置', icon: Settings, path: ''},
        {id: 'help', label: '帮助中心', icon: HelpCircle, path: '/help'},
    ]

    const handleMenuClick = (item: typeof menuItems[0]) => {
        if (item.path && item.path !== '/dashboard') {
            navigate(item.path)
        } else {
            setActiveMenu(item.id)
        }
    }

    return (
        <div className="h-screen bg-gray-50 flex overflow-hidden">
            {/* Sidebar */}
            <aside
                className={`${sidebarOpen ? 'w-64' : 'w-20'} bg-gradient-to-b from-primary-600 to-primary-800 shadow-xl transition-all duration-300 ease-in-out flex flex-col h-screen`}>
                {/* Sidebar Header */}
                <div className="p-4 border-b border-white/10">
                    <div className="flex items-center gap-3">
                        <div
                            className="flex items-center justify-center w-10 h-10 bg-white/20 backdrop-blur-sm rounded-xl flex-shrink-0">
                            <Droplets className="w-6 h-6 text-white"/>
                        </div>
                        {sidebarOpen && (
                            <h1 className="text-lg font-bold text-white">水务平台</h1>
                        )}
                    </div>
                </div>

                {/* Navigation Menu */}
                <nav className="flex-1 p-2 overflow-y-auto">
                    <ul className="space-y-1">
                        {menuItems.map((item) => {
                            const Icon = item.icon
                            return (
                                <li key={item.id}>
                                    <button
                                        onClick={() => handleMenuClick(item)}
                                        className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 ease-out ${
                                            activeMenu === item.id
                                                ? 'bg-white/20 text-white border border-white/30 shadow-lg'
                                                : 'text-white hover:bg-white/10 active:scale-95 hover:text-white'
                                        }`}
                                    >
                                        <Icon className="w-5 h-5 flex-shrink-0"/>
                                        {sidebarOpen && (
                                            <span className="font-medium">{item.label}</span>
                                        )}
                                    </button>
                                </li>
                            )
                        })}
                    </ul>

                    {/* Campus Selector in Sidebar */}
                    {sidebarOpen && (
                        <div className="border-t border-white/10 mt-4 pt-2">
                            <p className="px-2 py-2 text-xs font-medium text-white/80 uppercase">切换校区</p>
                            <div className="space-y-1">
                                {campuses.map((campus) => (
                                    <button
                                        key={campus.id}
                                        onClick={() => setSelectedCampus(campus.id)}
                                        className={`w-full flex items-center gap-2 px-3 py-2 rounded-lg transition-all duration-200 ease-out ${
                                            selectedCampus === campus.id
                                                ? 'bg-white/20 text-white border border-white/30'
                                                : 'text-white hover:bg-white/10 active:scale-95 hover:text-white border border-transparent'
                                        }`}
                                    >
                                        <div
                                            className={`w-2 h-2 rounded-full ${selectedCampus === campus.id ? 'bg-white' : 'bg-white/40'}`}></div>
                                        <div className="flex-1 text-left">
                                            <div className="text-sm font-medium">{campus.name}</div>
                                            <div className="text-xs text-white/80">{campus.code}</div>
                                        </div>
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}
                </nav>

                {/* Sidebar Footer */}
                <div className="p-2 border-t border-white/10 flex-shrink-0">
                    <div
                        className="flex items-center gap-3 px-4 py-3 rounded-xl hover:bg-white/10 active:scale-95 transition-all cursor-pointer">
                        {avatar ? (
                            <img src={avatar} alt="头像"
                                 className="w-9 h-9 rounded-full object-cover ring-2 ring-white/30"/>
                        ) : (
                            <div
                                className="w-9 h-9 bg-white/20 rounded-full flex items-center justify-center ring-2 ring-white/30">
                                <User className="w-4 h-4 text-white"/>
                            </div>
                        )}
                        {sidebarOpen && (
                            <div className="flex-1 min-w-0">
                                <p className="text-sm font-medium text-white truncate">
                                    {nickname || '用户'}
                                </p>
                                <p className="text-xs text-white truncate">
                                    UID: {uid || '未知'}
                                </p>
                            </div>
                        )}
                    </div>
                    {sidebarOpen && (
                        <div className="flex gap-2 mt-2">
                            <button
                                onClick={openProfileModal}
                                className="flex-1 flex items-center justify-center gap-2 px-4 py-2 text-white hover:bg-white/10 active:scale-95 hover:text-white rounded-xl transition-all duration-200 ease-out"
                            >
                                <User className="w-4 h-4"/>
                                <span className="text-sm font-medium">个人中心</span>
                            </button>
                            <button
                                onClick={handleLogout}
                                className="flex items-center justify-center px-3 text-white hover:text-red-400 hover:bg-red-500/20 rounded-xl transition-all duration-200 ease-out"
                            >
                                <LogOut className="w-4 h-4"/>
                            </button>
                        </div>
                    )}
                </div>
            </aside>

            {/* Main Content */}
            <div className="flex-1 flex flex-col bg-gray-50 dark:bg-gray-900">
                {/* Top Header */}
                <header className="bg-gradient-to-r from-primary-600 to-primary-800 shadow-lg">
                    <div className="px-6 py-4 flex items-center justify-between">
                        <div className="flex items-center gap-4">
                            <button
                                onClick={() => setSidebarOpen(!sidebarOpen)}
                                className="p-2 text-white/80 hover:text-white hover:bg-white/10 active:scale-95 rounded-xl transition-all"
                            >
                                {sidebarOpen ? <X className="w-5 h-5"/> : <Menu className="w-5 h-5"/>}
                            </button>
                            <h2 className="text-xl font-bold text-white">
                                {menuItems.find(item => item.id === activeMenu)?.label || '仪表盘'}
                            </h2>

                            {/* Current Campus Indicator */}
                            <div
                                className="flex items-center gap-2 px-3 py-1.5 bg-white/10 backdrop-blur-sm rounded-xl">
                                <div className="w-2 h-2 bg-white rounded-full"></div>
                                <span className="text-sm text-white/80">{currentCampus?.name}</span>
                            </div>

                            {/* 节水建议 */}
                            {waterSuggestion && (
                                <div
                                    className="flex items-center gap-2 px-3 py-1.5 bg-white/10 backdrop-blur-sm rounded-xl">
                                    <Lightbulb className="w-5 h-5 text-cyan-300"/>
                                    <span className="text-sm text-white whitespace-nowrap">{waterSuggestion}</span>
                                </div>
                            )}
                        </div>

                        <div className="flex items-center gap-4">
                            {/* Weather Info */}
                            {weather && (
                                <div
                                    className="flex items-center gap-3 px-3 py-1.5 bg-white/10 backdrop-blur-sm rounded-xl">
                                    <Sun className="w-5 h-5 text-yellow-300"/>
                                    <div className="flex items-center gap-2">
                                        <span className="text-lg font-bold text-white">{weather.temp}°C</span>
                                        <span className="text-xs text-white">{weather.condition}</span>
                                    </div>
                                    <div className="flex items-center gap-1 text-xs text-white/80">
                                        <span>💧 {weather.humidity}%</span>
                                        <span className="mx-1">|</span>
                                        <span>💨 {weather.wind}</span>
                                    </div>
                                </div>
                            )}
                            {weatherLoading && (
                                <div
                                    className="flex items-center gap-2 px-3 py-1.5 bg-white/10 backdrop-blur-sm rounded-xl">
                                    <RefreshCw className="w-4 h-4 text-white animate-spin"/>
                                    <span className="text-xs text-white">加载天气...</span>
                                </div>
                            )}

                            {/* AI 聊天助手按钮 */}
                            <button
                                onClick={() => setShowChatBot(true)}
                                className="flex items-center gap-2 px-4 py-2.5 bg-water hover:bg-water/80 active:scale-95 rounded-xl transition-all duration-200"
                                title="AI 助手"
                            >
                                <Bot className="w-5 h-5 text-white"/>
                                <span className="text-sm font-medium text-white">AI 用水助手</span>
                            </button>

                            {!sidebarOpen && (
                                <>
                                    <div className="flex items-center gap-2 text-sm text-white">
                                        <User className="w-4 h-4"/>
                                        <span>{nickname || '用户'}</span>
                                    </div>
                                    <button
                                        onClick={handleLogout}
                                        className="flex items-center gap-2 px-3 py-1.5 text-white/70 hover:text-white hover:bg-white/10 active:scale-95 rounded-xl transition-all duration-200"
                                    >
                                        <LogOut className="w-4 h-4"/>
                                    </button>
                                </>
                            )}
                        </div>
                    </div>
                </header>

                {/* Main Content Area */}
                <main className="flex-1 p-6 overflow-y-auto">
                    {/* 错误提示 */}
                    {error && (
                        <div
                            className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center justify-between">
                            <div className="flex items-center gap-2 text-red-700">
                                <AlertTriangle className="w-5 h-5"/>
                                <span>{error}</span>
                            </div>
                            <button
                                onClick={handleRefresh}
                                className="px-3 py-1 text-sm text-red-700 hover:bg-red-100 rounded transition-all duration-200"
                            >
                                重试
                            </button>
                        </div>
                    )}

                    {/* 刷新按钮 */}
                    <div className="flex justify-end mb-4">
                        <button
                            onClick={handleRefresh}
                            disabled={loading}
                            className="flex items-center gap-2 px-4 py-2 text-gray-600 hover:bg-gray-100 active:scale-95 rounded-lg transition-all duration-200 disabled:opacity-50"
                        >
                            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`}/>
                            <span className="text-sm">刷新数据</span>
                        </button>
                    </div>

                    {/* 卡片提示框 */}
                    {activeTooltip && (
                        <div
                            className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded-lg text-sm text-blue-700 flex items-start gap-2">
                            <HelpCircle className="w-4 h-4 flex-shrink-0 mt-0.5"/>
                            <span>{cardTooltips[activeTooltip]}</span>
                            <button
                                onClick={() => setActiveTooltip(null)}
                                className="ml-auto text-blue-400 hover:text-blue-600"
                            >
                                <X className="w-4 h-4"/>
                            </button>
                        </div>
                    )}

                    <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-8 gap-4 mb-8">
                        {/* 今日用水量 */}
                        <div className="glass-card rounded-2xl p-4 animate-slide-up" style={{animationDelay: '0ms'}}>
                            <div className="flex items-center justify-between mb-2">
                                <div className="p-2 bg-blue-600 rounded-lg">
                                    <Droplets className="w-4 h-4 text-white"/>
                                </div>
                                {loadingToday ? (
                                    <RefreshCw className="w-4 h-4 text-gray-400 animate-spin"/>
                                ) : (
                                    todayUsage > 0 && yesterdayUsage > 0 && (
                                        <div
                                            className={`flex items-center gap-1 text-sm ${todayChange.isPositive ? 'text-red-500' : 'text-green-500'}`}>
                                            {todayChange.isPositive ? <TrendingUp className="w-4 h-4"/> :
                                                <TrendingDown className="w-4"/>}
                                            <span>{todayChange.value.toFixed(1)}%</span>
                                        </div>
                                    )
                                )}
                            </div>
                            <p className="text-xs text-gray-500 dark:text-gray-400 flex items-center gap-1">
                                今日用水量
                                <CardTooltipIcon id="todayUsage"/>
                            </p>
                            <p className="text-xl font-bold text-gray-900 dark:text-white">
                                {loadingToday ? (
                                    <span className="text-gray-300">加载中...</span>
                                ) : (
                                    <span>{formatUsage(todayUsage)} m³</span>
                                )}
                            </p>
                            {!loadingToday && yesterdayUsage > 0 && (
                                <p className="text-xs text-gray-400 mt-1">昨日同期: {formatUsage(yesterdayUsage)} m³</p>
                            )}
                        </div>

                        {/* 本月用水量 */}
                        <div className="glass-card rounded-2xl p-4 animate-slide-up" style={{animationDelay: '0ms'}}>
                            <div className="flex items-center justify-between mb-2">
                                <div className="p-2 bg-purple-600 rounded-lg">
                                    <BarChart3 className="w-4 h-4 text-white"/>
                                </div>
                                {loadingMonth ? (
                                    <RefreshCw className="w-4 h-4 text-gray-400 animate-spin"/>
                                ) : (
                                    <div
                                        className={`flex items-center gap-1 text-sm ${monthChange.isPositive ? 'text-red-600' : 'text-white'}`}>
                                        {monthChange.isPositive ? <TrendingUp className="w-4 h-4"/> :
                                            <TrendingDown className="w-4 h-4"/>}
                                        <span>{monthChange.value.toFixed(1)}%</span>
                                    </div>
                                )}
                            </div>
                            <p className="text-xs text-gray-500 flex items-center gap-1">
                                本月用水量
                                <CardTooltipIcon id="monthUsage"/>
                            </p>
                            <p className="text-xl font-bold text-gray-900">
                                {loadingMonth ? (
                                    <span className="text-gray-300">加载中...</span>
                                ) : (
                                    <span>{formatUsage(monthUsage)} m³</span>
                                )}
                            </p>
                            {!loadingMonth && lastMonthSameDay > 0 && (
                                <p className="text-xs text-gray-400 mt-1">上月同期: {formatUsage(lastMonthSameDay)} m³</p>
                            )}
                        </div>

                        {/* 明日用水量预测 */}
                        <div className="glass-card rounded-2xl p-4 animate-slide-up" style={{animationDelay: '0ms'}}>
                            <div className="flex items-center justify-between mb-2">
                                <div className="p-2 bg-orange-500 rounded-lg">
                                    <TrendingUp className="w-4 h-4 text-white"/>
                                </div>
                                {loadingPrediction ? (
                                    <RefreshCw className="w-4 h-4 text-gray-400 animate-spin"/>
                                ) : predictedTomorrowUsage !== null ? (
                                    <span
                                        className="px-2 py-1 bg-green-100 text-green-700 text-xs font-medium rounded-full">
                  AI 预测
                </span>
                                ) : null}
                            </div>
                            <p className="text-xs text-gray-500 flex items-center gap-1">
                                明日用水预测
                                <CardTooltipIcon id="prediction"/>
                            </p>
                            <p className="text-lg font-bold text-gray-900">
                                {loadingPrediction ? (
                                    <span className="text-gray-300">预测中...</span>
                                ) : predictedTomorrowUsage !== null ? (
                                    <span>{formatUsage(predictedTomorrowUsage)} m³</span>
                                ) : (
                                    <span className="text-gray-400 text-sm">暂无数据</span>
                                )}
                            </p>
                            {!loadingPrediction && predictedTomorrowUsage !== null && (
                                <p className="text-xs text-gray-400 mt-1">基于近7天数据分析</p>
                            )}
                        </div>

                        {/* 用水波动指数 */}
                        <div className="glass-card rounded-2xl p-4 animate-slide-up" style={{animationDelay: '0ms'}}>
                            <div className="flex items-center justify-between mb-2">
                                <div className="p-2 bg-cyan-500 rounded-lg">
                                    <Activity className="w-4 h-4 text-white"/>
                                </div>
                                {loadingWaterSwings ? (
                                    <RefreshCw className="w-4 h-4 text-gray-400 animate-spin"/>
                                ) : (
                                    <span
                                        className="px-2 py-1 bg-cyan-100 text-cyan-700 text-xs font-medium rounded-full">
                  波动指数
                </span>
                                )}
                            </div>
                            <p className="text-xs text-gray-500 flex items-center gap-1">
                                校区用水波动
                                <CardTooltipIcon id="waterSwings"/>
                            </p>
                            <div className="mt-1 space-y-0.5">
                                {loadingWaterSwings ? (
                                    <span className="text-gray-300 text-xs">加载中...</span>
                                ) : waterSwings === null ? (
                                    <p className="text-xs text-gray-400">设备开启时间较短<br/>暂无法分析</p>
                                ) : (
                                    <>
                                        <p className="text-xs font-medium text-gray-900">花园: <span
                                            className="text-cyan-600">{waterSwings.school_1?.toFixed(1) ?? '--'}</span>
                                        </p>
                                        <p className="text-xs font-medium text-gray-900">龙子湖: <span
                                            className="text-cyan-600">{waterSwings.school_2?.toFixed(1) ?? '--'}</span>
                                        </p>
                                        <p className="text-xs font-medium text-gray-900">江淮: <span
                                            className="text-cyan-600">{waterSwings.school_3?.toFixed(1) ?? '--'}</span>
                                        </p>
                                    </>
                                )}
                            </div>
                        </div>

                        {/* 异常告警 */}
                        <div className="glass-card rounded-2xl p-4 animate-slide-up" style={{animationDelay: '0ms'}}>
                            <div className="flex items-center justify-between mb-2">
                                <div className="p-2 bg-yellow-500 rounded-lg">
                                    <AlertTriangle className="w-4 h-4 text-white"/>
                                </div>
                                <span className="text-sm text-red-600">+{alertCount}</span>
                            </div>
                            <p className="text-xs text-gray-500 flex items-center gap-1">
                                异常告警
                                <CardTooltipIcon id="alerts"/>
                            </p>
                            <p className="text-xl font-bold text-gray-900">
                                {loading ? '加载中...' : `${alertCount} 条`}
                            </p>
                        </div>

                        {/* 在线设备 */}
                        <div className="glass-card rounded-2xl p-4 animate-slide-up" style={{animationDelay: '0ms'}}>
                            <div className="flex items-center justify-between mb-2">
                                <div className="p-2 bg-green-600 rounded-lg">
                                    <User className="w-4 h-4 text-white"/>
                                </div>
                                {offlineRate > 0 && (
                                    <span
                                        className="px-2 py-1 bg-red-100 text-red-700 text-xs font-medium rounded-full">
                  离线率 {offlineRate.toFixed(1)}%
                </span>
                                )}
                            </div>
                            <p className="text-xs text-gray-500 flex items-center gap-1">
                                在线设备
                                <CardTooltipIcon id="devices"/>
                            </p>
                            <p className="text-xl font-bold text-gray-900">
                                {loading ? '加载中...' : `${deviceCount} 台`}
                            </p>
                            {loadingOffline ? (
                                <p className="text-xs text-gray-300 mt-1">离线设备加载中...</p>
                            ) : offlineDevices.length > 0 ? (
                                <p className="text-xs text-red-500 mt-1 flex items-center gap-1">
                                    <WifiOff className="w-3 h-3"/>
                                    离线 {offlineDevices.length} 台
                                </p>
                            ) : (
                                <p className="text-xs text-green-500 mt-1">设备全部在线</p>
                            )}
                        </div>

                        {/* 健康评分 */}
                        <div className="glass-card rounded-2xl p-4 animate-slide-up" style={{animationDelay: '0ms'}}>
                            <div className="flex items-center justify-between mb-2">
                                <div className="p-2 bg-emerald-600 rounded-lg">
                                    <Activity className="w-4 h-4 text-white"/>
                                </div>
                            </div>
                            <p className="text-xs text-gray-500 flex items-center gap-1">
                                设备健康评分
                                <CardTooltipIcon id="healthScore"/>
                            </p>
                            <p className="text-xl font-bold text-gray-900">
                                {loading ? '加载中...' : `${healthyScore.toFixed(1)}`}
                            </p>
                            <p className="text-xs text-gray-400 mt-1">满分: 100</p>
                        </div>

                        {/* 夜间异常用水量 */}
                        <div className="glass-card rounded-2xl p-4 animate-slide-up" style={{animationDelay: '0ms'}}>
                            <div className="flex items-center justify-between mb-2">
                                <div className="p-2 bg-red-500 rounded-lg">
                                    <AlertTriangle className="w-4 h-4 text-white"/>
                                </div>
                                {loadingUnNormal ? (
                                    <RefreshCw className="w-4 h-4 text-gray-400 animate-spin"/>
                                ) : unNormalUsage > 0 ? (
                                    <span
                                        className="px-2 py-1 bg-red-100 text-red-700 text-xs font-medium rounded-full">
                  异常
                </span>
                                ) : (
                                    <span
                                        className="px-2 py-1 bg-green-100 text-green-700 text-xs font-medium rounded-full">
                  正常
                </span>
                                )}
                            </div>
                            <p className="text-xs text-gray-500">
                                夜间异常用水量
                            </p>
                            <p className="text-xl font-bold text-gray-900">
                                {loadingUnNormal ? (
                                    <span className="text-gray-300">加载中...</span>
                                ) : (
                                    <span>{formatUsage(unNormalUsage)} m³</span>
                                )}
                            </p>
                            <p className="text-xs text-gray-400 mt-1">深夜 0:00-5:00 用水</p>
                        </div>
                    </div>

                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                        {/* 高峰用水时段 */}
                        <div className="glass-card rounded-2xl p-4 animate-slide-up" style={{animationDelay: '0ms'}}>
                            <div className="flex items-center justify-between mb-4">
                                <h2 className="text-lg font-semibold text-gray-900">高峰用水时段</h2>
                            </div>
                            {highUsageTimes.length > 0 ? (
                                <div className="space-y-3">
                                    {highUsageTimes.slice(0, 3).map((time, idx) => (
                                        <div key={idx}
                                             className="p-4 rounded-lg bg-orange-50 border-l-4 border-orange-500">
                                            <div className="flex items-center gap-2">
                                                <Activity className="w-4 h-4 text-orange-600"/>
                                                <span className="font-medium text-gray-900">
                        {new Date(time).toLocaleTimeString('zh-CN', {hour: '2-digit', minute: '2-digit'})}
                      </span>
                                            </div>
                                            <p className="text-sm text-gray-600 mt-1">用水量较高</p>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <div className="text-center py-8 text-gray-400">暂无数据</div>
                            )}
                        </div>

                        {/* 最近告警 */}
                        <div className="glass-card rounded-2xl p-4 animate-slide-up" style={{animationDelay: '0ms'}}>
                            <div className="flex items-center justify-between mb-4">
                                <h2 className="text-lg font-semibold text-gray-900">最近告警</h2>
                                <div className="flex items-center gap-2">
                                    {warnings.length > 0 && (
                                        <button
                                            onClick={dismissAllWarnings}
                                            className="px-2 py-1 text-xs text-gray-500 hover:text-red-600 hover:bg-red-50 rounded border border-gray-200"
                                        >
                                            清除所有
                                        </button>
                                    )}
                                    <span
                                        className="px-2 py-1 bg-yellow-100 text-yellow-700 text-xs font-medium rounded-full">
                  {warnings.length} 条
                </span>
                                </div>
                            </div>
                            {warnings.length > 2 && (
                                <p className="text-xs text-gray-400 mb-3">仅展示最近2条告警</p>
                            )}
                            <div className="space-y-3">
                                {loadingWarnings ? (
                                    <div className="flex items-center justify-center py-8">
                                        <RefreshCw className="w-6 h-6 text-gray-400 animate-spin"/>
                                    </div>
                                ) : warnings.length > 0 ? (
                                    warnings.slice(0, 2).map((warning, idx) => (
                                        <div
                                            key={idx}
                                            className={`p-4 rounded-lg border-l-4 ${
                                                warning.eventLevel === 'WARN' || warning.eventLevel === '1'
                                                    ? 'bg-yellow-50 border-yellow-500'
                                                    : 'bg-red-50 border-red-500'
                                            }`}
                                        >
                                            <div className="flex items-start justify-between">
                                                <div className="flex items-center gap-2">
                                                    <AlertTriangle
                                                        className={`w-4 h-4 flex-shrink-0 ${
                                                            warning.eventLevel === 'WARN' || warning.eventLevel === '1'
                                                                ? 'text-white'
                                                                : 'text-red-600'
                                                        }`}
                                                    />
                                                    <span className="font-medium text-gray-900 text-sm">
                          {deviceTypeMap[warning.deviceType] || warning.deviceType}
                        </span>
                                                </div>
                                                <span className={`px-2 py-0.5 text-xs rounded ${
                                                    warning.eventLevel === 'WARN' || warning.eventLevel === '1'
                                                        ? 'bg-yellow-200 text-yellow-800'
                                                        : 'bg-red-200 text-red-800'
                                                }`}>
                        {warning.eventLevel === 'WARN' || warning.eventLevel === '1' ? '警告' : '严重'}
                      </span>
                                            </div>
                                            <p className="text-sm text-gray-600 mt-2 ml-6">{warning.eventDesc}</p>
                                            <div className="flex items-center gap-4 mt-2 ml-6 text-xs text-gray-400">
                      <span className="font-mono bg-gray-100 px-1.5 py-0.5 rounded">
                        {warning.deviceCode}
                      </span>
                                            </div>
                                            <div className="flex items-center gap-1 mt-1 ml-6 text-xs text-gray-400">
                                                <svg className="w-3 h-3" fill="none" stroke="currentColor"
                                                     viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                                                          d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
                                                </svg>
                                                {warning.eventTime ? new Date(warning.eventTime).toLocaleString('zh-CN') : ''}
                                            </div>
                                            <button
                                                onClick={() => dismissWarning(warning.id)}
                                                className="ml-auto mt-2 px-2 py-1 text-xs text-gray-500 hover:text-red-600 hover:bg-red-50 rounded border border-gray-200"
                                            >
                                                清除
                                            </button>
                                        </div>
                                    ))
                                ) : (
                                    <div className="text-center py-8 text-gray-400">
                                        <svg className="w-12 h-12 mx-auto mb-2 text-gray-300" fill="none"
                                             stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                                                  d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                                        </svg>
                                        <p>暂无告警</p>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>

                    {/* 离线设备列表 */}
                    {!loadingOffline && offlineDevices.length > 0 && (
                        <div className="mt-6 glass-card rounded-2xl p-6">
                            <div className="flex items-center gap-2 mb-4">
                                <WifiOff className="w-5 h-5 text-red-500"/>
                                <h2 className="text-lg font-semibold text-gray-900">离线设备列表</h2>
                                <span
                                    className="px-2 py-0.5 bg-red-100 text-red-700 text-sm rounded-full">{offlineDevices.length}</span>
                            </div>
                            <div className="flex flex-wrap gap-2">
                                {offlineDevices.map((deviceId, index) => (
                                    <span
                                        key={index}
                                        className="px-3 py-1.5 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg font-mono"
                                    >
                  {deviceId}
                </span>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* 图表区域 */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-6">
                        {/* 用水趋势图 */}
                        <div className="glass-card rounded-2xl p-4 animate-slide-up" style={{animationDelay: '0ms'}}>
                            <h2 className="text-lg font-semibold text-gray-900 mb-4">历史用水数据</h2>
                            {weeklyUsageData.every(d => d.usage === 0) ? (
                                <div className="h-[280px] flex items-center justify-center text-gray-400">
                                    暂无数据
                                </div>
                            ) : (
                                <ResponsiveContainer width="100%" height={280}>
                                    <LineChart data={weeklyUsageData}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#d1d5db"/>
                                        <XAxis dataKey="day" tick={{fontSize: 12}}/>
                                        <YAxis tick={{fontSize: 12}} unit="m³"/>
                                        <Tooltip
                                            formatter={(value: number | undefined) => value !== undefined ? [`${value} m³`, '用水量'] : ['无数据', '用水量']}
                                            contentStyle={{
                                                borderRadius: '8px',
                                                border: 'none',
                                                boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
                                            }}
                                        />
                                        <Line type="monotone" dataKey="usage" stroke="#1d4ed8" strokeWidth={3}
                                              dot={{fill: '#1d4ed8', r: 5}}/>
                                    </LineChart>
                                </ResponsiveContainer>
                            )}
                        </div>

                        {/* 各校区用水占比 */}
                        <div className="glass-card rounded-2xl p-4 animate-slide-up" style={{animationDelay: '0ms'}}>
                            <h2 className="text-lg font-semibold text-gray-900 mb-4">花园、龙子湖、江淮三校区用水占比</h2>
                            {loadingCampusRate ? (
                                <div className="h-[280px] flex items-center justify-center">
                                    <div
                                        className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
                                </div>
                            ) : (
                                <ResponsiveContainer width="100%" height={280}>
                                    <BarChart data={campusUsageData} layout="vertical">
                                        <CartesianGrid strokeDasharray="3 3" stroke="#d1d5db"/>
                                        <XAxis type="number" tick={{fontSize: 12}} unit="%" domain={[0, 100]}/>
                                        <YAxis dataKey="name" type="category" tick={{fontSize: 12}} width={60}/>
                                        <Tooltip
                                            formatter={(value: number | undefined) => value !== undefined ? [`${value.toFixed(1)}%`, '用水占比'] : ['无数据', '用水占比']}
                                            contentStyle={{
                                                borderRadius: '8px',
                                                border: 'none',
                                                boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
                                            }}
                                        />
                                        <Bar dataKey="usage" fill="#15803d" radius={[0, 4, 4, 0]}/>
                                    </BarChart>
                                </ResponsiveContainer>
                            )}
                        </div>

                        {/* 各区域用水占比 */}
                        <div className="glass-card rounded-2xl p-4 animate-slide-up" style={{animationDelay: '0ms'}}>
                            <h2 className="text-lg font-semibold text-gray-900 mb-4">各校园区域用水占比</h2>
                            {loadingRegionRate ? (
                                <div className="h-[280px] flex items-center justify-center">
                                    <div
                                        className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
                                </div>
                            ) : (
                                <ResponsiveContainer width="100%" height={280}>
                                    <BarChart data={regionRate} layout="vertical">
                                        <CartesianGrid strokeDasharray="3 3" stroke="#d1d5db"/>
                                        <XAxis type="number" tick={{fontSize: 12}} unit="%" domain={[0, 100]}/>
                                        <YAxis dataKey="name" type="category" tick={{fontSize: 12}} width={60}/>
                                        <Tooltip
                                            formatter={(value: number | undefined) => value !== undefined ? [`${value.toFixed(1)}%`, '用水占比'] : ['无数据', '用水占比']}
                                            contentStyle={{
                                                borderRadius: '8px',
                                                border: 'none',
                                                boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
                                            }}
                                        />
                                        <Bar dataKey="value" fill="#7c3aed" radius={[0, 4, 4, 0]}/>
                                    </BarChart>
                                </ResponsiveContainer>
                            )}
                        </div>
                    </div>
                </main>

                {/* Profile Modal */}
                {showProfileModal && (
                    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                        <div className="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4">
                            <div className="flex items-center justify-between p-6 border-b border-gray-200">
                                <h2 className="text-xl font-semibold text-gray-900">个人中心</h2>
                                <button
                                    onClick={() => setShowProfileModal(false)}
                                    className="p-2 hover:bg-gray-100 active:scale-95 rounded-lg"
                                >
                                    <X className="w-5 h-5 text-gray-500"/>
                                </button>
                            </div>

                            <div className="p-6">
                                {/* Avatar */}
                                <div className="flex flex-col items-center mb-6">
                                    <div className="relative">
                                        {avatar ? (
                                            <img src={avatar} alt="头像"
                                                 className="w-20 h-20 rounded-full object-cover border-4 border-gray-100"/>
                                        ) : (
                                            <div
                                                className="w-20 h-20 rounded-full bg-gray-200 flex items-center justify-center border-4 border-gray-100">
                                                <User className="w-10 h-10 text-gray-400"/>
                                            </div>
                                        )}
                                        <label
                                            className="absolute bottom-0 right-0 p-1.5 bg-primary-600 text-white rounded-full cursor-pointer hover:bg-primary-700">
                                            <Camera className="w-4 h-4"/>
                                            <input
                                                type="file"
                                                accept="image/*"
                                                onChange={handleAvatarChange}
                                                className="hidden"
                                                disabled={profileLoading}
                                            />
                                        </label>
                                    </div>
                                    <p className="mt-2 text-sm text-gray-500">点击更换头像</p>
                                </div>

                                {/* Tabs */}
                                <div className="flex border-b border-gray-200 mb-4">
                                    <button
                                        onClick={() => {
                                            setProfileTab('info');
                                            setProfileMessage(null)
                                        }}
                                        className={`flex-1 pb-3 text-sm font-medium ${
                                            profileTab === 'info'
                                                ? 'text-primary-600 border-b-2 border-primary-600'
                                                : 'text-gray-500 hover:text-gray-700'
                                        }`}
                                    >
                                        昵称
                                    </button>
                                    <button
                                        onClick={() => {
                                            setProfileTab('password');
                                            setProfileMessage(null)
                                        }}
                                        className={`flex-1 pb-3 text-sm font-medium ${
                                            profileTab === 'password'
                                                ? 'text-primary-600 border-b-2 border-primary-600'
                                                : 'text-gray-500 hover:text-gray-700'
                                        }`}
                                    >
                                        密码
                                    </button>
                                </div>

                                {/* Message */}
                                {profileMessage && (
                                    <div className={`mb-4 p-3 rounded-lg text-sm ${
                                        profileMessage.type === 'success'
                                            ? 'bg-green-50 text-green-700 border border-green-200'
                                            : 'bg-red-50 text-red-700 border border-red-200'
                                    }`}>
                                        {profileMessage.text}
                                    </div>
                                )}

                                {/* Tab Content */}
                                {profileTab === 'info' && (
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-2">昵称</label>
                                        <input
                                            type="text"
                                            value={editingNickname}
                                            onChange={(e) => setEditingNickname(e.target.value)}
                                            placeholder="请输入新昵称"
                                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
                                        />
                                        <button
                                            onClick={handleUpdateNickname}
                                            disabled={profileLoading}
                                            className="w-full mt-4 flex items-center justify-center gap-2 px-4 py-2.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
                                        >
                                            {profileLoading ? <RefreshCw className="w-4 h-4 animate-spin"/> :
                                                <Check className="w-4 h-4"/>}
                                            保存昵称
                                        </button>
                                    </div>
                                )}

                                {profileTab === 'password' && (
                                    <div className="space-y-4">
                                        <div>
                                            <label
                                                className="block text-sm font-medium text-gray-700 mb-2">当前密码</label>
                                            <div className="relative">
                                                <input
                                                    type={showPassword ? 'text' : 'password'}
                                                    value={oldPassword}
                                                    onChange={(e) => setOldPassword(e.target.value)}
                                                    placeholder="请输入当前密码"
                                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none pr-10"
                                                />
                                                <button
                                                    type="button"
                                                    onClick={() => setShowPassword(!showPassword)}
                                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                                                >
                                                    {showPassword ? <EyeOff className="w-5 h-5"/> :
                                                        <Eye className="w-5 h-5"/>}
                                                </button>
                                            </div>
                                        </div>
                                        <div>
                                            <label
                                                className="block text-sm font-medium text-gray-700 mb-2">新密码</label>
                                            <input
                                                type={showPassword ? 'text' : 'password'}
                                                value={newPassword}
                                                onChange={(e) => setNewPassword(e.target.value)}
                                                placeholder="请输入新密码（至少6位）"
                                                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
                                            />
                                        </div>
                                        <button
                                            onClick={handleUpdatePassword}
                                            disabled={profileLoading}
                                            className="w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
                                        >
                                            {profileLoading ? <RefreshCw className="w-4 h-4 animate-spin"/> :
                                                <Check className="w-4 h-4"/>}
                                            修改密码
                                        </button>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                )}

                {/* AI 聊天助手弹窗 */}
                {showChatBot && (
                    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                        <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg mx-4 h-[600px] flex flex-col">
                            {/* 聊天头部 */}
                            <div
                                className="flex items-center justify-between p-4 border-b border-gray-200 bg-gradient-to-r from-primary-600 to-primary-800 rounded-t-2xl">
                                <div className="flex items-center gap-3">
                                    <div className="p-2 bg-white/20 rounded-xl">
                                        <Bot className="w-6 h-6 text-white"/>
                                    </div>
                                    <div>
                                        <h2 className="text-lg font-semibold text-white">AI 用水助手</h2>
                                        <p className="text-xs text-white/80">可查询各校区用水数据</p>
                                    </div>
                                </div>
                                <button
                                    onClick={() => setShowChatBot(false)}
                                    className="p-2 hover:bg-white/10 active:scale-95 rounded-lg transition-all"
                                >
                                    <X className="w-5 h-5 text-white"/>
                                </button>
                            </div>

                            {/* 聊天消息区域 */}
                            <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-gray-50">
                                {chatMessages.map((msg, idx) => (
                                    <div
                                        key={idx}
                                        className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
                                    >
                                        <div
                                            className={`max-w-[80%] p-3 rounded-2xl ${
                                                msg.role === 'user'
                                                    ? 'bg-primary-600 text-white rounded-br-md'
                                                    : 'bg-white border border-gray-200 text-gray-800 rounded-bl-md'
                                            }`}
                                        >
                                            <div className="flex items-start gap-2">
                                                {msg.role === 'assistant' && (
                                                    <Bot className="w-4 h-4 text-primary-600 mt-0.5 flex-shrink-0"/>
                                                )}
                                                <p className="text-sm whitespace-pre-wrap">{msg.content}</p>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                                {chatLoading && (
                                    <div className="flex justify-start">
                                        <div className="bg-white border border-gray-200 p-3 rounded-2xl rounded-bl-md">
                                            <div className="flex items-center gap-2">
                                                <Loader2 className="w-4 h-4 text-primary-600 animate-spin"/>
                                                <span className="text-sm text-gray-500">AI 正在思考...</span>
                                            </div>
                                        </div>
                                    </div>
                                )}
                                <div ref={chatEndRef}/>
                            </div>

                            {/* 快捷问题提示 */}
                            {chatMessages.length === 1 && (
                                <div className="px-4 pb-2 flex flex-wrap gap-2">
                                    <button
                                        onClick={() => {
                                            setChatInput('龙子湖校区今天用水量是多少？');
                                            handleSendMessage()
                                        }}
                                        className="px-3 py-1.5 text-xs bg-gray-100 hover:bg-gray-200 rounded-full transition-colors"
                                        disabled={chatLoading}
                                    >
                                        今日用水量
                                    </button>
                                    <button
                                        onClick={() => {
                                            setChatInput('各校区用水占比是多少？');
                                            handleSendMessage()
                                        }}
                                        className="px-3 py-1.5 text-xs bg-gray-100 hover:bg-gray-200 rounded-full transition-colors"
                                        disabled={chatLoading}
                                    >
                                        用水占比
                                    </button>
                                    <button
                                        onClick={() => {
                                            setChatInput('设备离线率是多少？');
                                            handleSendMessage()
                                        }}
                                        className="px-3 py-1.5 text-xs bg-gray-100 hover:bg-gray-200 rounded-full transition-colors"
                                        disabled={chatLoading}
                                    >
                                        设备离线率
                                    </button>
                                    <button
                                        onClick={() => {
                                            setChatInput('水质合格率是多少？');
                                            handleSendMessage()
                                        }}
                                        className="px-3 py-1.5 text-xs bg-gray-100 hover:bg-gray-200 rounded-full transition-colors"
                                        disabled={chatLoading}
                                    >
                                        水质合格率
                                    </button>
                                </div>
                            )}

                            {/* 输入区域 */}
                            <div className="p-4 border-t border-gray-200 bg-white rounded-b-2xl">
                                <div className="flex items-center gap-2">
                                    <input
                                        type="text"
                                        value={chatInput}
                                        onChange={(e) => setChatInput(e.target.value)}
                                        onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSendMessage()}
                                        placeholder="请输入你的问题..."
                                        disabled={chatLoading}
                                        className="flex-1 px-4 py-2.5 border border-gray-300 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none disabled:bg-gray-100"
                                    />
                                    <button
                                        onClick={handleSendMessage}
                                        disabled={chatLoading || !chatInput.trim()}
                                        className="p-2.5 bg-primary-600 text-white rounded-xl hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed active:scale-95 transition-all"
                                    >
                                        {chatLoading ? (
                                            <Loader2 className="w-5 h-5 animate-spin"/>
                                        ) : (
                                            <Send className="w-5 h-5"/>
                                        )}
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    )
}
