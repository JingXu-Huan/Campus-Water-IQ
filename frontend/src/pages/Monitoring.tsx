import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { iotApi, generateDeviceId, generateWaterQualitySensorId, parseDeviceCode, BuildingInfo, BuildingType, DeviceFlowData, WaterQualityData, getBuildingConfig, getBuildingType } from '@/api/iot'
import { Droplets, User, Menu, X, Activity, Building2, Building, Home, RefreshCw, XCircle, LayoutDashboard, Waves, FlaskConical, Gauge, Wifi, WifiOff } from 'lucide-react'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'

// 校区映射
const CAMPUS_MAP: Record<number, { name: string; code: string }> = {
  1: { name: '花园校区', code: 'HY' },
  2: { name: '龙子湖校区', code: 'LZ' },
  3: { name: '江淮校区', code: 'JH' }
}

// 楼宇类型配置
const BUILDING_TYPE_CONFIG: Record<BuildingType, { name: string; icon: any; color: string }> = {
  education: { name: '教学楼', icon: Building2, color: 'blue' },
  experiment: { name: '实验楼', icon: Building, color: 'purple' },
  dormitory: { name: '宿舍楼', icon: Home, color: 'green' }
}

// 动态获取楼宇配置
const getBuildingsByCampus = async (campusNo: number) => {
  const config = await getBuildingConfig()
  const { educationStart, experimentStart, totalBuildings, floors, rooms } = config
  const campusName = CAMPUS_MAP[campusNo]?.name || '未知'
  const buildings: BuildingInfo[] = []
  
  for (let i = 1; i <= totalBuildings; i++) {
    const type = getBuildingType(i, educationStart, experimentStart)
    const typeConfig = BUILDING_TYPE_CONFIG[type]
    buildings.push({
      id: `${type}-${campusNo}-${i}`,
      type,
      name: `${campusName}${typeConfig.name}${i}号`,
      buildingNo: i,
      startIndex: i
    })
  }
  
  return { buildings, floors, rooms }
}

// 生成某楼栋的所有设备ID
const generateBuildingDeviceIds = (campusNo: number, buildingNo: number, floorCount: number = 6, unitsPerFloor: number = 4): string[] => {
  const deviceIds: string[] = []
  for (let floor = 1; floor <= floorCount; floor++) {
    for (let unit = 1; unit <= unitsPerFloor; unit++) {
      deviceIds.push(generateDeviceId(campusNo, buildingNo, floor, unit))
    }
  }
  return deviceIds
}

// 圆形进度条 - 流量最大0.6L/s
// 颜色: 绿色(0-50%) 黄色(50-80%) 红色(80-100%)
function GaugeMeter({ value, max = 0.6, size = 180, color = '#22c55e' }: { value: number; max?: number; size?: number; color?: string }) {
  const percentage = Math.min(Math.max(value / max, 0), 1)
  
  // 根据百分比动态变色
  let progressColor = color
  if (color === '#3b82f6' || color === '#22c55e') { // 流量用蓝绿渐变色
    if (percentage > 0.8) progressColor = '#ef4444'      // 红色-超负荷
    else if (percentage > 0.5) progressColor = '#f59e0b'  // 黄色-预警
    else progressColor = '#22c55e'                       // 绿色-正常
  }
  
  const radius = (size - 24) / 2
  const circumference = 2 * Math.PI * radius
  return (
    <div className="relative" style={{ width: size, height: size }}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        <circle cx={size/2} cy={size/2} r={radius} fill="none" stroke="#e5e7eb" strokeWidth="12" />
        <circle cx={size/2} cy={size/2} r={radius} fill="none" stroke={progressColor} strokeWidth="12"
          strokeLinecap="round" strokeDasharray={circumference} strokeDashoffset={circumference * (1 - percentage)}
          transform={`rotate(-90 ${size/2} ${size/2})`} className="transition-all duration-500" />
      </svg>
    </div>
  )
}

export default function Monitoring() {
  const navigate = useNavigate()
  const { uid, nickname, avatar } = useAuthStore()
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [selectedCampus, setSelectedCampus] = useState(2) // 默认龙子湖
  const [selectedBuilding, setSelectedBuilding] = useState<BuildingInfo | null>(null)
  const [deviceData, setDeviceData] = useState<DeviceFlowData[]>([])
  const [waterQualityData, setWaterQualityData] = useState<WaterQualityData[]>([])
  const [loading, setLoading] = useState(false)
  const [lastUpdate, setLastUpdate] = useState<Date>(new Date())
  const [selectedDevice, setSelectedDevice] = useState<DeviceFlowData | null>(null)
  
  const campuses = [
    { id: 1, name: '花园校区', code: 'HY' },
    { id: 2, name: '龙子湖校区', code: 'LZ' },
    { id: 3, name: '江淮校区', code: 'JH' }
  ]
  
  const [buildings, setBuildings] = useState<BuildingInfo[]>([])
  const [buildingConfig, setBuildingConfig] = useState({ floors: 6, rooms: 10 })
  const [loadingBuildings, setLoadingBuildings] = useState(true)
  const [waterQualityScore, setWaterQualityScore] = useState<number>(0)
  
  // 切换校区时加载楼宇配置
  useEffect(() => {
    const loadBuildings = async () => {
      setLoadingBuildings(true)
      try {
        const { buildings: buildingList, floors, rooms } = await getBuildingsByCampus(selectedCampus)
        setBuildings(buildingList)
        setBuildingConfig({ floors: floors || 6, rooms: rooms || 10 })
      } catch (error) {
        console.error('加载楼宇配置失败:', error)
      } finally {
        setLoadingBuildings(false)
      }
    }
    loadBuildings()
  }, [selectedCampus])
  
  // 获取选中楼宇的设备数据
  const fetchBuildingData = useCallback(async () => {
    if (!selectedBuilding) return
    
    setLoading(true)
    try {
      const deviceIds = generateBuildingDeviceIds(
        selectedCampus, 
        selectedBuilding.buildingNo,
        buildingConfig.floors,
        buildingConfig.rooms
      )
      
      // 先获取流量设备数据
      const flowData = await iotApi.getBatchFlow(deviceIds)
      setDeviceData(flowData)
      
      // 从流量设备数据中提取实际存在的楼层
      const actualFloors = [...new Set(flowData.map(d => parseDeviceCode(d.deviceId)?.floorNo).filter(Boolean))]
      const waterQualitySensorIds = actualFloors.map(floorNo => 
        generateWaterQualitySensorId(selectedCampus, selectedBuilding.buildingNo, parseInt(floorNo!))
      )
      
      // 并行获取水质数据和每个楼层的水质分数
      const waterQualityResults = await Promise.all([
        iotApi.getBatchWaterQuality(waterQualitySensorIds),
        ...waterQualitySensorIds.map(id => iotApi.getWaterQuality(id))
      ])
      
      const waterQualityData = waterQualityResults[0] as WaterQualityData[]
      const waterQualityScores = waterQualityResults.slice(1) as number[]
      
      // 将水质分数附加到水质数据中
      waterQualityData.forEach((wq, index) => {
        wq.score = waterQualityScores[index]
      })
      
      setWaterQualityData(waterQualityData)
      
      // 计算平均水质分数
      const validScores = waterQualityScores.filter(s => s > 0)
      const avgScore = validScores.length > 0 ? validScores.reduce((a, b) => a + b, 0) / validScores.length : 0
      setWaterQualityScore(avgScore)
      
      setLastUpdate(new Date())
    } catch (error) {
      console.error('获取设备数据失败:', error)
    } finally {
      setLoading(false)
    }
  }, [selectedBuilding, selectedCampus, buildingConfig])
  
  useEffect(() => {
    if (selectedBuilding) {
      fetchBuildingData()
      // 每30秒自动刷新
      const interval = setInterval(fetchBuildingData, 30000)
      return () => clearInterval(interval)
    }
  }, [selectedBuilding, fetchBuildingData])
  
  // 计算统计数据
  const onlineCount = deviceData.filter(d => d.status === 'online').length
  const offlineCount = deviceData.filter(d => d.status === 'offline').length
  const totalFlow = deviceData.reduce((sum, d) => sum + d.flow, 0)
  const avgFlow = onlineCount > 0 ? deviceData.filter(d => d.status === 'online').reduce((sum, d) => sum + d.flow, 0) / onlineCount : 0
  const avgPressure = onlineCount > 0 ? deviceData.filter(d => d.status === 'online').reduce((s, d) => s + (d.pressure || 0), 0) / onlineCount : 0
  const avgTemperature = onlineCount > 0 ? deviceData.filter(d => d.status === 'online').reduce((s, d) => s + (d.temperature || 0), 0) / onlineCount : 0
  
  // 计算最大流量范围: [0, 所有用水单元 * 0.5]
  const maxFlowRange = buildingConfig.floors * buildingConfig.rooms * 0.5
  
  // 按楼层分组统计
  const floorGroups = deviceData.reduce((acc, device) => {
    const info = parseDeviceCode(device.deviceId)
    const floor = info?.floorNo ? parseInt(info.floorNo) : 1
    if (!acc[floor]) acc[floor] = { devices: [], online: 0, totalFlow: 0, avgPressure: 0, avgTemp: 0, waterQuality: null as WaterQualityData | null }
    acc[floor].devices.push(device)
    if (device.status === 'online') {
      acc[floor].online++
      acc[floor].totalFlow += device.flow
      acc[floor].avgPressure += (device.pressure || 0)
      acc[floor].avgTemp += (device.temperature || 0)
    }
    return acc
  }, {} as Record<number, { devices: typeof deviceData; online: number; totalFlow: number; avgPressure: number; avgTemp: number; waterQuality: WaterQualityData | null }>)
  
  // 将水质数据合并到楼层数据中
  waterQualityData.forEach(wq => {
    const info = parseDeviceCode(wq.deviceId)
    if (info?.floorNo) {
      const floor = parseInt(info.floorNo)
      if (floorGroups[floor]) {
        floorGroups[floor].waterQuality = wq
      }
    }
  })
  
  const floors = Object.entries(floorGroups)
    .map(([floor, data]) => ({
      floor: parseInt(floor),
      ...data,
      avgFlow: data.online > 0 ? data.totalFlow / data.online : 0,
      avgPressure: data.online > 0 ? data.avgPressure / data.online : 0,
      avgTemp: data.online > 0 ? data.avgTemp / data.online : 0
    }))
    .sort((a, b) => b.floor - a.floor)
  
  return (
    <div className="h-screen bg-gray-50 flex overflow-hidden">
      {/* Sidebar */}
      <aside className={`${sidebarOpen ? 'w-64' : 'w-20'} bg-gradient-to-b from-primary-600 to-primary-800 shadow-xl transition-all duration-300 flex flex-col h-screen`}>
        {/* Logo区域 */}
        <div className="p-4 border-b border-white/10 flex-shrink-0">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 bg-white/20 backdrop-blur-sm rounded-xl">
              <Droplets className="w-6 h-6 text-white" />
            </div>
            {sidebarOpen && (
              <h1 className="text-lg font-bold text-white">水务平台</h1>
            )}
          </div>
        </div>

        {/* 返回主界面 */}
        <div className="p-2 border-b border-white/10 flex-shrink-0">
          <button onClick={() => navigate('/dashboard')} className={`flex items-center gap-2 px-3 py-2.5 rounded-xl text-white/80 hover:bg-white/10 hover:text-white transition-all text-sm ${sidebarOpen ? 'w-full' : 'mx-auto justify-center'}`}>
            <LayoutDashboard className="w-5 h-5" />
            {sidebarOpen && <span>返回主界面</span>}
          </button>
        </div>

        {/* 校区选择 */}
        <nav className="flex-1 p-2 overflow-y-auto">
          <p className="px-4 mb-2 text-xs font-medium text-white/40 uppercase">切换校区</p>
          <div className="space-y-1 mb-6">
            {campuses.map((campus) => (
              <button
                key={campus.id}
                onClick={() => { setSelectedCampus(campus.id); setSelectedBuilding(null) }}
                className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-xl transition-all ${
                  selectedCampus === campus.id
                    ? 'bg-white/20 text-white border border-white/30'
                    : 'text-white/70 hover:bg-white/10 hover:text-white border border-transparent'
                }`}
              >
                <div className={`w-2 h-2 rounded-full ${selectedCampus === campus.id ? 'bg-white' : 'bg-white/40'}`} />
                {sidebarOpen && (
                  <div className="flex-1 text-left">
                    <div className="text-sm font-medium">{campus.name}</div>
                    <div className="text-xs text-white/50">{campus.code}</div>
                  </div>
                )}
              </button>
            ))}
          </div>
          
          {/* 楼宇列表 */}
          <p className="px-4 mb-2 text-xs font-medium text-white/40 uppercase">楼宇列表</p>
          {loadingBuildings ? (
            <div className="px-4 py-8 text-center">
              <RefreshCw className="w-5 h-5 text-white/40 animate-spin mx-auto" />
              <p className="text-xs text-white/50 mt-2">加载中...</p>
            </div>
          ) : (
            <div className="space-y-1">
              {buildings.map((building) => {
              const config = BUILDING_TYPE_CONFIG[building.type]
              const Icon = config.icon
              return (
                <button
                  key={building.id}
                  onClick={() => setSelectedBuilding(building)}
                  className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-xl transition-all ${
                    selectedBuilding?.id === building.id
                      ? 'bg-white/20 text-white border border-white/30'
                      : 'text-white/70 hover:bg-white/10 hover:text-white border border-transparent'
                  }`}
                >
                  <Icon className={`w-4 h-4`} />
                  {sidebarOpen && (
                    <span className="text-sm truncate">{building.name}</span>
                  )}
                </button>
              )
            })}
          </div>
          )}
        </nav>

        {/* User Footer */}
        <div className="p-2 border-t border-white/10 flex-shrink-0">
          <div className="flex items-center gap-3 px-4 py-3 rounded-xl hover:bg-white/10 transition-all cursor-pointer">
            {avatar ? (
              <img src={avatar} alt="头像" className="w-9 h-9 rounded-full object-cover ring-2 ring-white/30" />
            ) : (
              <div className="w-9 h-9 bg-white/20 rounded-full flex items-center justify-center ring-2 ring-white/30">
                <User className="w-4 h-4 text-white" />
              </div>
            )}
            {sidebarOpen && (
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-white truncate">{nickname || '用户'}</p>
                <p className="text-xs text-white/60 truncate">UID: {uid || '未知'}</p>
              </div>
            )}
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col bg-gray-50">
        <header className="bg-gradient-to-r from-primary-600 to-primary-800 shadow-lg">
          <div className="px-6 py-4 flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={() => setSidebarOpen(!sidebarOpen)}
                className="p-2 text-white/80 hover:text-white hover:bg-white/10 rounded-xl transition-all"
              >
                {sidebarOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
              </button>
              <h2 className="text-xl font-bold text-white">实时监测</h2>
              <div className="flex items-center gap-2 px-3 py-1.5 bg-white/10 backdrop-blur-sm rounded-xl">
                <Activity className="w-4 h-4 text-white/80" />
                <span className="text-sm text-white/80">实时</span>
              </div>
            </div>
            <div className="flex items-center gap-4">
              <span className="text-sm text-gray-500">
                最后更新: {lastUpdate.toLocaleTimeString()}
              </span>
              <button
                onClick={fetchBuildingData}
                disabled={loading || !selectedBuilding}
                className="flex items-center gap-2 px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg disabled:opacity-50"
              >
                <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
                刷新
              </button>
            </div>
          </div>
        </header>

        <main className="flex-1 p-6 overflow-y-auto">
          {!selectedBuilding ? (
            <div className="flex items-center justify-center h-full">
              <div className="text-center">
                <Building2 className="w-16 h-16 text-gray-300 mx-auto mb-4" />
                <p className="text-gray-500">请从左侧选择楼宇</p>
              </div>
            </div>
          ) : (
            <div className="space-y-6">
              {/* 六个监测卡片 */}
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-4">
                {/* 总流量 */}
                <div className="bg-white rounded-xl p-5 shadow-sm hover:shadow-md transition-shadow">
                  <div className="flex items-center gap-3 mb-3">
                    <div className="p-2.5 bg-blue-100 rounded-xl">
                      <Droplets className="w-5 h-5 text-blue-600" />
                    </div>
                    <span className="text-sm font-medium text-gray-700">总流量</span>
                  </div>
                  <p className="text-2xl font-bold text-gray-900">{totalFlow.toFixed(2)} <span className="text-sm font-normal text-gray-400">L/s</span></p>
                  <div className="mt-2">
                    <GaugeMeter value={totalFlow} max={maxFlowRange} size={60} color="#3b82f6" />
                  </div>
                </div>

                {/* 平均流量 */}
                <div className="bg-white rounded-xl p-5 shadow-sm hover:shadow-md transition-shadow">
                  <div className="flex items-center gap-3 mb-3">
                    <div className="p-2.5 bg-purple-100 rounded-xl">
                      <Activity className="w-5 h-5 text-purple-600" />
                    </div>
                    <span className="text-sm font-medium text-gray-700">平均流量</span>
                  </div>
                  <p className="text-2xl font-bold text-gray-900">{avgFlow.toFixed(2)} <span className="text-sm font-normal text-gray-400">L/s</span></p>
                  <div className="mt-2">
                    <GaugeMeter value={avgFlow} max={0.5} size={60} color="#8b5cf6" />
                  </div>
                </div>

                {/* 平均水压 */}
                <div className="bg-white rounded-xl p-5 shadow-sm hover:shadow-md transition-shadow">
                  <div className="flex items-center gap-3 mb-3">
                    <div className="p-2.5 bg-cyan-100 rounded-xl">
                      <Gauge className="w-5 h-5 text-cyan-600" />
                    </div>
                    <span className="text-sm font-medium text-gray-700">平均水压</span>
                  </div>
                  <p className="text-2xl font-bold text-gray-900">{avgPressure.toFixed(2)} <span className="text-sm font-normal text-gray-400">MPa</span></p>
                  <div className="mt-2">
                    <GaugeMeter value={avgPressure} max={Math.max(avgPressure * 1.5, 1)} size={60} color="#06b6d4" />
                  </div>
                </div>

                {/* 平均水温 */}
                <div className="bg-white rounded-xl p-5 shadow-sm hover:shadow-md transition-shadow">
                  <div className="flex items-center gap-3 mb-3">
                    <div className="p-2.5 bg-orange-100 rounded-xl">
                      <Activity className="w-5 h-5 text-orange-600" />
                    </div>
                    <span className="text-sm font-medium text-gray-700">平均水温</span>
                  </div>
                  <p className="text-2xl font-bold text-gray-900">{avgTemperature.toFixed(2)} <span className="text-sm font-normal text-gray-400">°C</span></p>
                  <div className="mt-2">
                    <GaugeMeter value={avgTemperature} max={Math.max(avgTemperature * 1.5, 30)} size={60} color="#f97316" />
                  </div>
                </div>

                {/* 水质分数 */}
                <div className="bg-white rounded-xl p-5 shadow-sm hover:shadow-md transition-shadow">
                  <div className="flex items-center gap-3 mb-3">
                    <div className={`p-2.5 rounded-xl ${
                      waterQualityScore >= 90 ? 'bg-gradient-to-br from-green-400 to-green-600' :
                      waterQualityScore >= 70 ? 'bg-gradient-to-br from-yellow-400 to-yellow-600' :
                      'bg-gradient-to-br from-red-400 to-red-600'
                    }`}>
                      <Waves className="w-5 h-5 text-white" />
                    </div>
                    <span className="text-sm font-medium text-gray-700">水质分数</span>
                  </div>
                  <p className={`text-2xl font-bold ${
                    waterQualityScore >= 90 ? 'text-green-600' :
                    waterQualityScore >= 70 ? 'text-yellow-600' :
                    'text-red-600'
                  }`}>{waterQualityScore.toFixed(0)} <span className="text-sm font-normal text-gray-400">分</span></p>
                  <div className="mt-2">
                    <GaugeMeter value={waterQualityScore} max={100} size={60} color={
                      waterQualityScore >= 90 ? '#22c55e' :
                      waterQualityScore >= 70 ? '#eab308' :
                      '#ef4444'
                    } />
                  </div>
                </div>

                {/* 设备在线状态 */}
                <div className="bg-white rounded-xl p-5 shadow-sm hover:shadow-md transition-shadow">
                  <div className="flex items-center gap-3 mb-3">
                    <div className={`p-2.5 rounded-xl ${
                      onlineCount > 0 ? 'bg-gradient-to-br from-green-400 to-green-600' : 'bg-gray-100'
                    }`}>
                      {onlineCount > 0 ? <Wifi className="w-5 h-5 text-white" /> : <WifiOff className="w-5 h-5 text-gray-400" />}
                    </div>
                    <span className="text-sm font-medium text-gray-700">在线设备</span>
                  </div>
                  <div className="flex items-baseline gap-2">
                    <p className="text-2xl font-bold text-green-600">{onlineCount}</p>
                    <span className="text-gray-400">/</span>
                    <p className="text-2xl font-bold text-gray-900">{deviceData.length}</p>
                    <span className="text-sm text-gray-400">台</span>
                  </div>
                  <div className="mt-2 flex items-center gap-2">
                    <div className="flex-1 h-2 bg-gray-100 rounded-full overflow-hidden">
                      <div 
                        className="h-full bg-gradient-to-r from-green-400 to-green-600 transition-all duration-500" 
                        style={{ width: `${deviceData.length > 0 ? (onlineCount / deviceData.length) * 100 : 0}%` }}
                      />
                    </div>
                    <span className="text-xs text-gray-500">
                      {deviceData.length > 0 ? Math.round((onlineCount / deviceData.length) * 100) : 0}%
                    </span>
                  </div>
                </div>
              </div>

              {/* 图表区域 */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                {/* 各楼层流量对比图 */}
                <div className="bg-white rounded-xl p-4 shadow-sm">
                  <h3 className="text-lg font-semibold text-gray-900 mb-3">各楼层流量对比</h3>
                  <ResponsiveContainer width="100%" height={180}>
                    <BarChart data={floors.map(f => ({ name: `${f.floor}F`, flow: f.totalFlow }))}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                      <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                      <YAxis tick={{ fontSize: 10 }} unit="L/s" />
                      <Tooltip 
                        formatter={(value: number | undefined) => value !== undefined ? [`${value.toFixed(2)} L/s`, '流量'] : ['无数据', '流量']}
                        contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }}
                      />
                      <Bar dataKey="flow" fill="#3b82f6" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>

                {/* 各楼层水质分数对比图 */}
                <div className="bg-white rounded-xl p-4 shadow-sm">
                  <h3 className="text-lg font-semibold text-gray-900 mb-3">各楼层水质分数</h3>
                  <ResponsiveContainer width="100%" height={180}>
                    <BarChart data={floors.map(f => ({ 
                      name: `${f.floor}F`, 
                      score: f.waterQuality?.score || 0 
                    }))}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                      <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                      <YAxis domain={[0, 100]} tick={{ fontSize: 10 }} />
                      <Tooltip 
                        formatter={(value: number | undefined) => value !== undefined && value > 0 ? [`${value.toFixed(0)} 分`, '水质分数'] : ['无数据', '水质分数']}
                        contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }}
                      />
                      <Bar dataKey="score" fill="#10b981" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </div>

              {/* 房间设备列表 */}
              <div className="bg-white rounded-xl p-6 shadow-sm">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-semibold text-gray-900">各房间数据详情</h3>
                  <div className="flex items-center gap-4 text-sm">
                    <span className="flex items-center gap-1">
                      <span className="w-2 h-2 bg-green-500 rounded-full"></span>
                      <span className="text-gray-500">在线 {onlineCount}</span>
                    </span>
                    <span className="flex items-center gap-1">
                      <span className="w-2 h-2 bg-red-500 rounded-full"></span>
                      <span className="text-gray-500">离线 {offlineCount}</span>
                    </span>
                  </div>
                </div>
                {loading ? (
                  <div className="flex items-center justify-center h-40">
                    <RefreshCw className="w-8 h-8 text-gray-400 animate-spin" />
                  </div>
                ) : (
                  <div className="space-y-6">
                    {floors.map((floor) => (
                      <div key={floor.floor} className="border border-gray-200 rounded-xl overflow-hidden">
                        {/* 楼层标题 */}
                        <div className="bg-gradient-to-r from-gray-50 to-gray-100 px-4 py-3 flex items-center justify-between flex-wrap gap-2">
                          <div className="flex items-center gap-3">
                            <span className="w-8 h-8 bg-primary-100 text-primary-700 rounded-lg flex items-center justify-center font-bold">
                              {floor.floor}F
                            </span>
                            <span className="text-sm text-gray-600">
                              {floor.online}/{floor.devices.length} 在线
                            </span>
                          </div>
                          <div className="flex items-center gap-6 text-sm flex-wrap">
                            <span className="text-blue-600 flex items-center gap-1" title="流量">
                              <Droplets className="w-3.5 h-3.5" />
                              <span className="font-semibold">{floor.avgFlow.toFixed(2)}</span> L/s
                            </span>
                            <span className="text-cyan-600 flex items-center gap-1" title="水压">
                              <Gauge className="w-3.5 h-3.5" />
                              <span className="font-semibold">{floor.avgPressure.toFixed(2)}</span> MPa
                            </span>
                            <span className="text-orange-600 flex items-center gap-1" title="水温">
                              <Activity className="w-3.5 h-3.5" />
                              <span className="font-semibold">{floor.avgTemp.toFixed(2)}</span> °C
                            </span>
                            {/* 水质传感器数据 */}
                            {floor.waterQuality && floor.waterQuality.status === 'online' && (
                              <>
                                <span className="text-emerald-600" title="浊度 NTU">
                                  <Waves className="inline w-3.5 h-3.5 mr-0.5" />
                                  <span className="font-semibold">{typeof floor.waterQuality.turbidity === 'number' ? floor.waterQuality.turbidity.toFixed(2) : '0.00'}</span> NTU
                                </span>
                                <span className="text-purple-600" title="pH值">
                                  <FlaskConical className="inline w-3.5 h-3.5 mr-0.5" />
                                  <span className="font-semibold">{typeof floor.waterQuality.ph === 'number' ? floor.waterQuality.ph.toFixed(2) : '0.00'}</span> pH
                                </span>
                                <span className="text-teal-600" title="含氯量 mg/L">
                                  <FlaskConical className="inline w-3.5 h-3.5 mr-0.5" />
                                  <span className="font-semibold">{typeof floor.waterQuality.chlorine === 'number' ? floor.waterQuality.chlorine.toFixed(2) : '0.00'}</span> mg/L
                                </span>
                              </>
                            )}
                          </div>
                        </div>
                        {/* 楼层内设备 */}
                        <div className="grid grid-cols-2 md:grid-cols-5 gap-2 p-3">
                          {floor.devices.map((device) => (
                            <div
                              key={device.deviceId}
                              onClick={() => device.status === 'online' && setSelectedDevice(device)}
                              className={`p-3 rounded-lg border transition-all cursor-pointer ${
                                device.status === 'online' 
                                  ? 'bg-white border-gray-100 hover:border-blue-300 hover:shadow-md' 
                                  : 'bg-gray-50 border-gray-100 opacity-60 cursor-default'
                              }`}
                            >
                              <div className="text-center">
                                <p className="text-xs font-semibold text-gray-700">
                                  {parseDeviceCode(device.deviceId)?.unitNo}
                                </p>
                                <p className="text-[10px] text-gray-400 mb-1">单元</p>
                                {device.status === 'online' ? (
                                  <>
                                    <p className="text-lg font-bold text-blue-600">{device.flow.toFixed(2)}</p>
                                    <p className="text-[10px] text-gray-400">L/s</p>
                                    <div className="mt-1 flex justify-center gap-1 text-[10px]">
                                      <span className="text-cyan-500">{device.pressure?.toFixed(2) || '--'} MPa</span>
                                      <span className="text-orange-500">{device.temperature?.toFixed(2) || '--'}°C</span>
                                    </div>
                                  </>
                                ) : (
                                  <div className="flex items-center justify-center gap-1 text-red-400 text-xs py-2">
                                    <XCircle className="w-3 h-3" />
                                    <span>离线</span>
                                  </div>
                                )}
                              </div>
                            </div>
                          ))}
                        </div>
                        {/* 水质传感器详情 */}
                        {floor.waterQuality && (
                          <div className="border-t border-gray-100 p-3 bg-gradient-to-r from-emerald-50 to-cyan-50">
                            <div className="flex items-center justify-between mb-2">
                              <div className="flex items-center gap-2">
                                <Waves className="w-4 h-4 text-emerald-600" />
                                <span className="text-sm font-medium text-gray-700">水质传感器</span>
                                <span className={`w-2 h-2 rounded-full ${floor.waterQuality.status === 'online' ? 'bg-green-500' : 'bg-red-500'}`}></span>
                              </div>
                              {/* 水质分数 */}
                              {floor.waterQuality.score !== undefined && floor.waterQuality.score > 0 && (
                                <div className={`px-2 py-0.5 rounded-full text-xs font-bold ${
                                  floor.waterQuality.score >= 90 ? 'bg-green-100 text-green-700' :
                                  floor.waterQuality.score >= 70 ? 'bg-yellow-100 text-yellow-700' :
                                  'bg-red-100 text-red-700'
                                }`}>
                                  水质分数: {floor.waterQuality.score.toFixed(0)}
                                </div>
                              )}
                            </div>
                            {floor.waterQuality && floor.waterQuality.status === 'online' ? (
                              <div className="grid grid-cols-3 gap-2 text-center text-sm">
                                <div className="bg-white rounded-lg p-2 shadow-sm">
                                  <p className="text-[10px] text-gray-400">浊度</p>
                                  <p className="font-semibold text-emerald-600">{typeof floor.waterQuality.turbidity === 'number' ? floor.waterQuality.turbidity.toFixed(2) : '0.00'}</p>
                                  <p className="text-[9px] text-gray-400">NTU</p>
                                </div>
                                <div className="bg-white rounded-lg p-2 shadow-sm">
                                  <p className="text-[10px] text-gray-400">pH值</p>
                                  <p className="font-semibold text-purple-600">{typeof floor.waterQuality.ph === 'number' ? floor.waterQuality.ph.toFixed(2) : '0.00'}</p>
                                </div>
                                <div className="bg-white rounded-lg p-2 shadow-sm">
                                  <p className="text-[10px] text-gray-400">含氯量</p>
                                  <p className="font-semibold text-teal-600">{typeof floor.waterQuality.chlorine === 'number' ? floor.waterQuality.chlorine.toFixed(2) : '0.00'}</p>
                                  <p className="text-[9px] text-gray-400">mg/L</p>
                                </div>
                              </div>
                            ) : (
                              <div className="flex items-center justify-center gap-2 text-red-400 text-sm py-2">
                                <XCircle className="w-4 h-4" />
                                <span>水质传感器离线</span>
                              </div>
                            )}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </main>

        {/* 设备详情弹窗 */}
        {selectedDevice && (
          <div 
            className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 animate-fade-in" 
            onClick={() => setSelectedDevice(null)}
          >
            <div 
              className="bg-white rounded-2xl p-8 w-[400px] max-w-[90vw] shadow-2xl animate-scale-in" 
              onClick={e => e.stopPropagation()}
            >
              <div className="flex items-center justify-between mb-6">
                <h3 className="text-xl font-bold text-gray-900">
                  {selectedBuilding?.name} - {parseDeviceCode(selectedDevice.deviceId)?.unitNo}单元
                </h3>
                <button onClick={() => setSelectedDevice(null)} className="p-1 hover:bg-gray-100 rounded-lg">
                  <X className="w-6 h-6 text-gray-500" />
                </button>
              </div>
              
              {/* 三个环形仪表盘 */}
              <div className="grid grid-cols-3 gap-6 mb-6">
                {/* 流量环 */}
                <div className="flex flex-col items-center">
                  <GaugeMeter value={selectedDevice.flow} max={0.5} size={100} color="#3b82f6" />
                  <p className="mt-2 text-sm font-medium text-blue-600">{selectedDevice.flow.toFixed(2)} L/s</p>
                  <p className="text-xs text-gray-400">流量</p>
                </div>
                {/* 水压环 */}
                <div className="flex flex-col items-center">
                  <GaugeMeter value={selectedDevice.pressure || 0} max={Math.max((selectedDevice.pressure || 0) * 1.5, 1)} size={100} color="#06b6d4" />
                  <p className="mt-2 text-sm font-medium text-cyan-600">{selectedDevice.pressure?.toFixed(2) || '--'} MPa</p>
                  <p className="text-xs text-gray-400">水压</p>
                </div>
                {/* 水温环 */}
                <div className="flex flex-col items-center">
                  <GaugeMeter value={selectedDevice.temperature || 0} max={Math.max((selectedDevice.temperature || 0) * 1.5, 30)} size={100} color="#f97316" />
                  <p className="mt-2 text-sm font-medium text-orange-600">{selectedDevice.temperature?.toFixed(2) || '--'} °C</p>
                  <p className="text-xs text-gray-400">水温</p>
                </div>
              </div>

              <div className="flex items-center justify-center gap-2">
                <span className={`w-2 h-2 rounded-full ${selectedDevice.status === 'online' ? 'bg-green-500' : 'bg-red-500'}`}></span>
                <span className="text-sm text-gray-600">{selectedDevice.status === 'online' ? '在线' : '离线'}</span>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
