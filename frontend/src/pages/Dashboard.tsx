import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { Droplets, LogOut, User, BarChart3, AlertTriangle, Settings, LayoutDashboard, Activity, Map, FileText, HelpCircle, Menu, X, RefreshCw, TrendingUp, TrendingDown, WifiOff, Camera, Eye, EyeOff, Check } from 'lucide-react'
import { useState, useEffect } from 'react'
import { iotApi, generateDeviceId } from '@/api/iot'
import { authApi } from '@/api/auth'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, LineChart, Line } from 'recharts'

export default function Dashboard() {
  const navigate = useNavigate()
  const { clearAuth, uid, nickname, avatar, updateProfile } = useAuthStore()
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

  const campuses = [
    { id: 'huayuan', name: '花园校区', code: 'HY', schoolId: 1 },
    { id: 'longzi', name: '龙子湖校区', code: 'LZ', schoolId: 2 },
    { id: 'jianghuai', name: '江淮校区', code: 'JH', schoolId: 3 }
  ]

  const [todayUsage, setTodayUsage] = useState<number>(0)
  const [yesterdayUsage, setYesterdayUsage] = useState<number>(0)
  const [monthUsage, setMonthUsage] = useState<number>(0)
  const [lastMonthSameDay, setLastMonthSameDay] = useState<number>(0)
  const [buildingStats, setBuildingStats] = useState<{name: string; flow: number; pressure: number; status: string; onlineCount: number; totalCount: number}[]>([])
  const [loadingBuildings, setLoadingBuildings] = useState(false)
  const [alertCount, setAlertCount] = useState<number>(0)
  const [deviceCount, setDeviceCount] = useState<number>(0)
  const [offlineDevices, setOfflineDevices] = useState<string[]>([])
  const [healthyScore, setHealthyScore] = useState<number>(0)
  const [loading, setLoading] = useState<boolean>(true)
  
  // 图表数据 - 模拟近7天用水趋势
  const [weeklyUsageData] = useState([
    { day: '周一', usage: 120 },
    { day: '周二', usage: 135 },
    { day: '周三', usage: 128 },
    { day: '周四', usage: 142 },
    { day: '周五', usage: 156 },
    { day: '周六', usage: 98 },
    { day: '周日', usage: 105 }
  ])
  
  // 各校区用水量对比
  const campusUsageData = campuses.map(c => ({
    name: c.name.replace('校区', ''),
    usage: Math.random() * 500 + 200
  }))
  const [loadingToday, setLoadingToday] = useState<boolean>(true)
  const [loadingMonth, setLoadingMonth] = useState<boolean>(true)
  const [loadingOffline, setLoadingOffline] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)

  const handleLogout = () => {
    clearAuth()
    navigate('/login')
  }

  const currentCampus = campuses.find(c => c.id === selectedCampus)

  // 计算变化率
  const calculateChange = (current: number, previous: number): { value: number; isPositive: boolean } => {
    if (previous === 0) return { value: 0, isPositive: true }
    const change = ((current - previous) / previous) * 100
    return { value: Math.abs(change), isPositive: change >= 0 }
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
    setError(null)
    
    try {
      // 使用 Promise.all 并行获取数据
      const { todayUsage: todayData, monthUsage: monthData } = await iotApi.getWaterUsage(currentCampus.schoolId)
      
      setTodayUsage(todayData.data)
      setYesterdayUsage(todayData.yesterday || 0)
      setMonthUsage(monthData.data)
      setLastMonthSameDay(monthData.lastMonthSameDay || 0)
    } catch (err) {
      console.error('获取用水量数据失败:', err)
      setError('获取用水量数据失败')
    } finally {
      setLoadingToday(false)
      setLoadingMonth(false)
      setLoading(false)
      // 模拟告警数据，实际应该从 API 获取
      setAlertCount(Math.floor(Math.random() * 5))
      
      // 从 iot-service 获取在线设备数量
      try {
        const onlineCount = await iotApi.getCampusOnlineDeviceCount(currentCampus.schoolId)
        setDeviceCount(onlineCount)
      } catch (err) {
        console.error('获取在线设备数量失败:', err)
        setDeviceCount(0)
      }

      // 获取设备健康评分
      try {
        const score = await iotApi.getHealthyScore()
        setHealthyScore(score)
      } catch (err) {
        console.error('获取健康评分失败:', err)
        setHealthyScore(0)
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

  // 手动刷新
  const handleRefresh = () => {
    fetchWaterUsageData()
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
      setProfileMessage({ type: 'error', text: '昵称不能为空' })
      return
    }
    setProfileLoading(true)
    setProfileMessage(null)
    try {
      await authApi.updateNickname({ nickname: editingNickname.trim() })
      updateProfile(editingNickname.trim())
      setProfileMessage({ type: 'success', text: '昵称更新成功' })
    } catch (error: any) {
      setProfileMessage({ type: 'error', text: error.message || '更新失败' })
    } finally {
      setProfileLoading(false)
    }
  }

  // 更新密码
  const handleUpdatePassword = async () => {
    if (!oldPassword || !newPassword) {
      setProfileMessage({ type: 'error', text: '请填写完整密码信息' })
      return
    }
    if (newPassword.length < 6) {
      setProfileMessage({ type: 'error', text: '新密码至少6位' })
      return
    }
    setProfileLoading(true)
    setProfileMessage(null)
    try {
      await authApi.updatePassword({ oldPassword, newPassword })
      setOldPassword('')
      setNewPassword('')
      setProfileMessage({ type: 'success', text: '密码更新成功' })
    } catch (error: any) {
      setProfileMessage({ type: 'error', text: error.message || '更新失败' })
    } finally {
      setProfileLoading(false)
    }
  }

  // 更新头像
  const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    
    // Convert to base64
    const reader = new FileReader()
    reader.onload = async () => {
      const base64 = reader.result as string
      setProfileLoading(true)
      setProfileMessage(null)
      try {
        await authApi.updateAvatar({ avatar: base64 })
        updateProfile(undefined, base64)
        setProfileMessage({ type: 'success', text: '头像更新成功' })
      } catch (error: any) {
        setProfileMessage({ type: 'error', text: error.message || '更新失败' })
      } finally {
        setProfileLoading(false)
      }
    }
    reader.readAsDataURL(file)
  }

  // 获取楼宇实时数据
  const fetchBuildingStats = async () => {
    if (!currentCampus) return
    setLoadingBuildings(true)
    try {
      const buildingTypes = [
        { name: '宿舍楼', type: 1, buildings: 3 },
        { name: '教学楼', type: 2, buildings: 2 },
        { name: '实验楼', type: 3, buildings: 1 }
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
            stats.push({ name: building.name, flow: 0, pressure: 0, status: '离线', onlineCount: 0, totalCount: totalDevices })
          }
        } catch (e) {
          console.error(`获取${building.name}数据失败:`, e)
          stats.push({ name: building.name, flow: 0, pressure: 0, status: '离线', onlineCount: 0, totalCount: 9 })
        }
      }
      
      setBuildingStats(stats)
    } catch (err) {
      console.error('获取楼宇数据失败:', err)
    } finally {
      setLoadingBuildings(false)
    }
  }

  useEffect(() => {
    fetchWaterUsageData()
    fetchBuildingStats()
  }, [selectedCampus])

  const menuItems = [
    { id: 'dashboard', label: '仪表盘', icon: LayoutDashboard, path: '/dashboard' },
    { id: 'monitoring', label: '实时监测', icon: Activity, path: '/monitoring' },
    { id: 'map', label: '数字孪生', icon: Map, path: '' },
    { id: 'reports', label: '数据报表', icon: FileText, path: '' },
    { id: 'settings', label: '系统设置', icon: Settings, path: '' },
    { id: 'help', label: '帮助中心', icon: HelpCircle, path: '' },
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
      <aside className={`${sidebarOpen ? 'w-64' : 'w-20'} bg-white shadow-lg transition-all duration-300 flex flex-col h-screen`}>
        {/* Sidebar Header */}
        <div className="p-4 border-b border-gray-200">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 bg-primary-100 rounded-lg flex-shrink-0">
              <Droplets className="w-6 h-6 text-primary-600" />
            </div>
            {sidebarOpen && (
              <h1 className="text-lg font-bold text-gray-900">水务平台</h1>
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
                    className={`w-full flex items-center gap-2 px-3 py-2 rounded-lg transition-all duration-200 ${
                      activeMenu === item.id
                        ? 'bg-primary-50 text-primary-600 border-r-2 border-primary-600'
                        : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                    }`}
                  >
                    <Icon className="w-5 h-5 flex-shrink-0" />
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
            <div className="border-t border-gray-200">
              <p className="px-2 py-2 text-xs font-medium text-gray-400 uppercase">切换校区</p>
              <div className="space-y-1">
                {campuses.map((campus) => (
                  <button
                    key={campus.id}
                    onClick={() => setSelectedCampus(campus.id)}
                    className={`w-full flex items-center gap-2 px-3 py-2 rounded-lg transition-all duration-200 ${
                      selectedCampus === campus.id
                        ? 'bg-primary-50 text-primary-700 border border-primary-200'
                        : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900 border border-transparent'
                    }`}
                  >
                    <div className={`w-2 h-2 rounded-full ${selectedCampus === campus.id ? 'bg-primary-600' : 'bg-gray-300'}`}></div>
                    <div className="flex-1 text-left">
                      <div className="text-sm font-medium">{campus.name}</div>
                      <div className="text-xs text-gray-400">{campus.code}</div>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          )}
        </nav>

        {/* Sidebar Footer */}
        <div className="p-2 border-t border-gray-200 flex-shrink-0">
          <div className="flex items-center gap-3 px-4 py-3">
            {avatar ? (
              <img src={avatar} alt="头像" className="w-8 h-8 rounded-full object-cover" />
            ) : (
              <div className="w-8 h-8 bg-gray-200 rounded-full flex items-center justify-center">
                <User className="w-4 h-4 text-gray-600" />
              </div>
            )}
            {sidebarOpen && (
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">
                  {nickname || '用户'}
                </p>
                <p className="text-xs text-gray-500 truncate">
                  UID: {uid || '未知'}
                </p>
              </div>
            )}
          </div>
          {sidebarOpen && (
            <div className="flex gap-2 mt-2">
              <button
                onClick={openProfileModal}
                className="flex-1 flex items-center justify-center gap-2 px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-all duration-200"
              >
                <User className="w-4 h-4" />
                <span className="text-sm font-medium">个人中心</span>
              </button>
              <button
                onClick={handleLogout}
                className="flex items-center justify-center px-3 text-gray-600 hover:text-red-600 hover:bg-red-50 rounded-lg transition-all duration-200"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          )}
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col">
        {/* Top Header */}
        <header className="bg-white shadow-sm">
          <div className="px-6 py-4 flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={() => setSidebarOpen(!sidebarOpen)}
                className="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
              >
                {sidebarOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
              </button>
              <h2 className="text-xl font-semibold text-gray-900">
                {menuItems.find(item => item.id === activeMenu)?.label || '仪表盘'}
              </h2>
              
              {/* Current Campus Indicator */}
              <div className="flex items-center gap-2 px-3 py-1.5 bg-gray-100 rounded-lg">
                <div className="w-2 h-2 bg-primary-600 rounded-full"></div>
                <span className="text-sm text-gray-600">{currentCampus?.name}</span>
              </div>
            </div>
            
            {!sidebarOpen && (
              <div className="flex items-center gap-4">
                <div className="flex items-center gap-2 text-sm text-gray-600">
                  <User className="w-4 h-4" />
                  <span>{nickname || '用户'}</span>
                  <span className="text-gray-400">|</span>
                  <span className="text-xs text-gray-500">UID: {uid || '未知'}</span>
                </div>
                <button
                  onClick={handleLogout}
                  className="flex items-center gap-2 px-4 py-2 text-gray-600 hover:text-gray-900 transition-colors"
                >
                  <LogOut className="w-5 h-5" />
                  <span>退出登录</span>
                </button>
              </div>
            )}
          </div>
        </header>

        {/* Main Content Area */}
        <main className="flex-1 p-6 overflow-y-auto">
          {/* 错误提示 */}
          {error && (
            <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center justify-between">
              <div className="flex items-center gap-2 text-red-700">
                <AlertTriangle className="w-5 h-5" />
                <span>{error}</span>
              </div>
              <button
                onClick={handleRefresh}
                className="px-3 py-1 text-sm text-red-700 hover:bg-red-100 rounded transition-colors"
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
              className="flex items-center gap-2 px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors disabled:opacity-50"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
              <span className="text-sm">刷新数据</span>
            </button>
          </div>
          
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6 mb-8">
          {/* 今日用水量 */}
          <div className="bg-white rounded-xl p-6 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-blue-100 rounded-lg">
                <Droplets className="w-6 h-6 text-blue-600" />
              </div>
              {loadingToday ? (
                <RefreshCw className="w-4 h-4 text-gray-400 animate-spin" />
              ) : (
                <div className={`flex items-center gap-1 text-sm ${todayChange.isPositive ? 'text-red-600' : 'text-green-600'}`}>
                  {todayChange.isPositive ? <TrendingUp className="w-4 h-4" /> : <TrendingDown className="w-4 h-4" />}
                  <span>{todayChange.value.toFixed(1)}%</span>
                </div>
              )}
            </div>
            <p className="text-sm text-gray-500">今日用水量</p>
            <p className="text-2xl font-bold text-gray-900">
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
          <div className="bg-white rounded-xl p-6 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-purple-100 rounded-lg">
                <BarChart3 className="w-6 h-6 text-purple-600" />
              </div>
              {loadingMonth ? (
                <RefreshCw className="w-4 h-4 text-gray-400 animate-spin" />
              ) : (
                <div className={`flex items-center gap-1 text-sm ${monthChange.isPositive ? 'text-red-600' : 'text-green-600'}`}>
                  {monthChange.isPositive ? <TrendingUp className="w-4 h-4" /> : <TrendingDown className="w-4 h-4" />}
                  <span>{monthChange.value.toFixed(1)}%</span>
                </div>
              )}
            </div>
            <p className="text-sm text-gray-500">本月用水量</p>
            <p className="text-2xl font-bold text-gray-900">
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

          {/* 异常告警 */}
          <div className="bg-white rounded-xl p-6 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-yellow-100 rounded-lg">
                <AlertTriangle className="w-6 h-6 text-yellow-600" />
              </div>
              <span className="text-sm text-red-600">+{alertCount}</span>
            </div>
            <p className="text-sm text-gray-500">异常告警</p>
            <p className="text-2xl font-bold text-gray-900">
              {loading ? '加载中...' : `${alertCount} 条`}
            </p>
          </div>

          {/* 在线设备 */}
          <div className="bg-white rounded-xl p-6 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-green-100 rounded-lg">
                <User className="w-6 h-6 text-green-600" />
              </div>
            </div>
            <p className="text-sm text-gray-500">在线设备</p>
            <p className="text-2xl font-bold text-gray-900">
              {loading ? '加载中...' : `${deviceCount} 台`}
            </p>
            {loadingOffline ? (
              <p className="text-xs text-gray-300 mt-1">离线设备加载中...</p>
            ) : offlineDevices.length > 0 ? (
              <p className="text-xs text-red-500 mt-1 flex items-center gap-1">
                <WifiOff className="w-3 h-3" />
                离线 {offlineDevices.length} 台
              </p>
            ) : (
              <p className="text-xs text-green-500 mt-1">设备全部在线</p>
            )}
          </div>

          {/* 健康评分 */}
          <div className="bg-white rounded-xl p-6 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-emerald-100 rounded-lg">
                <Activity className="w-6 h-6 text-emerald-600" />
              </div>
            </div>
            <p className="text-sm text-gray-500">设备健康评分</p>
            <p className="text-2xl font-bold text-gray-900">
              {loading ? '加载中...' : `${healthyScore.toFixed(1)}`}
            </p>
            <p className="text-xs text-gray-400 mt-1">满分: 100</p>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-white rounded-xl p-6 shadow-sm">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-semibold text-gray-900">实时监测</h2>
              <button className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
                <Settings className="w-5 h-5 text-gray-400" />
              </button>
            </div>
            <div className="space-y-4">
              {loadingBuildings ? (
                <div className="flex items-center justify-center py-8">
                  <RefreshCw className="w-6 h-6 text-gray-400 animate-spin" />
                </div>
              ) : buildingStats.length > 0 ? (
                buildingStats.map((building, idx) => (
                  <div key={idx} className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                    <div>
                      <p className="font-medium text-gray-900">{building.name}</p>
                      <p className="text-sm text-gray-500">
                        水压 {building.pressure.toFixed(2)} MPa • 流量 {building.flow.toFixed(2)} L/s
                      </p>
                    </div>
                    <span className={`px-3 py-1 text-sm rounded-full ${
                      building.status === '正常' 
                        ? 'bg-green-100 text-green-700' 
                        : 'bg-red-100 text-red-700'
                    }`}>
                      {building.status}
                    </span>
                  </div>
                ))
              ) : (
                <div className="text-center py-8 text-gray-400">暂无数据</div>
              )}
            </div>
          </div>

          <div className="bg-white rounded-xl p-6 shadow-sm">
            <h2 className="text-lg font-semibold text-gray-900 mb-6">最近告警</h2>
            <div className="space-y-4">
              <div className="flex items-start gap-4 p-4 border border-red-200 bg-red-50 rounded-lg">
                <AlertTriangle className="w-5 h-5 text-red-500 mt-0.5" />
                <div>
                  <p className="font-medium text-gray-900">漏水检测 - 宿舍楼 C 区</p>
                  <p className="text-sm text-gray-500 mt-1">检测到异常用水模式，可能存在管道漏水</p>
                  <p className="text-xs text-gray-400 mt-2">2026-02-19 14:30</p>
                </div>
              </div>
              <div className="flex items-start gap-4 p-4 border border-yellow-200 bg-yellow-50 rounded-lg">
                <AlertTriangle className="w-5 h-5 text-yellow-500 mt-0.5" />
                <div>
                  <p className="font-medium text-gray-900">水压异常 - 体育馆</p>
                  <p className="text-sm text-gray-500 mt-1">水压低于正常阈值</p>
                  <p className="text-xs text-gray-400 mt-2">2026-02-19 10:15</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* 离线设备列表 */}
        {!loadingOffline && offlineDevices.length > 0 && (
          <div className="mt-6 bg-white rounded-xl p-6 shadow-sm">
            <div className="flex items-center gap-2 mb-4">
              <WifiOff className="w-5 h-5 text-red-500" />
              <h2 className="text-lg font-semibold text-gray-900">离线设备列表</h2>
              <span className="px-2 py-0.5 bg-red-100 text-red-700 text-sm rounded-full">{offlineDevices.length}</span>
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
          <div className="bg-white rounded-xl p-6 shadow-sm">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">本周用水趋势</h2>
            <ResponsiveContainer width="100%" height={280}>
              <LineChart data={weeklyUsageData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="day" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} unit="m³" />
                <Tooltip 
                  formatter={(value: number | undefined) => value !== undefined ? [`${value} m³`, '用水量'] : ['无数据', '用水量']}
                  contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }}
                />
                <Line type="monotone" dataKey="usage" stroke="#3b82f6" strokeWidth={2} dot={{ fill: '#3b82f6', r: 4 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>

          {/* 各校区用水量对比 */}
          <div className="bg-white rounded-xl p-6 shadow-sm">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">各校区用水量对比</h2>
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={campusUsageData} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis type="number" tick={{ fontSize: 12 }} unit="m³" />
                <YAxis dataKey="name" type="category" tick={{ fontSize: 12 }} width={60} />
                <Tooltip 
                  formatter={(value: number | undefined) => value !== undefined ? [`${value.toFixed(1)} m³`, '用水量'] : ['无数据', '用水量']}
                  contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }}
                />
                <Bar dataKey="usage" fill="#22c55e" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
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
                className="p-2 hover:bg-gray-100 rounded-lg"
              >
                <X className="w-5 h-5 text-gray-500" />
              </button>
            </div>

            <div className="p-6">
              {/* Avatar */}
              <div className="flex flex-col items-center mb-6">
                <div className="relative">
                  {avatar ? (
                    <img src={avatar} alt="头像" className="w-20 h-20 rounded-full object-cover border-4 border-gray-100" />
                  ) : (
                    <div className="w-20 h-20 rounded-full bg-gray-200 flex items-center justify-center border-4 border-gray-100">
                      <User className="w-10 h-10 text-gray-400" />
                    </div>
                  )}
                  <label className="absolute bottom-0 right-0 p-1.5 bg-primary-600 text-white rounded-full cursor-pointer hover:bg-primary-700">
                    <Camera className="w-4 h-4" />
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
                  onClick={() => { setProfileTab('info'); setProfileMessage(null) }}
                  className={`flex-1 pb-3 text-sm font-medium ${
                    profileTab === 'info'
                      ? 'text-primary-600 border-b-2 border-primary-600'
                      : 'text-gray-500 hover:text-gray-700'
                  }`}
                >
                  昵称
                </button>
                <button
                  onClick={() => { setProfileTab('password'); setProfileMessage(null) }}
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
                    {profileLoading ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
                    保存昵称
                  </button>
                </div>
              )}

              {profileTab === 'password' && (
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">当前密码</label>
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
                        {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                      </button>
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">新密码</label>
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
                    {profileLoading ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
                    修改密码
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
      </div>
    </div>
  )
}
