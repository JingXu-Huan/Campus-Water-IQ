import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { iotApi, generateDeviceId, parseDeviceCode, BuildingInfo, BuildingType, DeviceFlowData, getBuildingConfig, getBuildingType } from '@/api/iot'
import { Droplets, User, Menu, X, Activity, Building2, Building, Home, RefreshCw, CheckCircle, XCircle, LayoutDashboard } from 'lucide-react'

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

// 圆形进度条
function GaugeMeter({ value, max = 100, size = 180, color = '#22c55e' }: { value: number; max?: number; size?: number; color?: string }) {
  const percentage = Math.min(Math.max(value / max, 0), 1)
  const radius = (size - 24) / 2
  const circumference = 2 * Math.PI * radius
  return (
    <div className="relative" style={{ width: size, height: size }}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        <circle cx={size/2} cy={size/2} r={radius} fill="none" stroke="#e5e7eb" strokeWidth="12" />
        <circle cx={size/2} cy={size/2} r={radius} fill="none" stroke={color} strokeWidth="12"
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
  const [loading, setLoading] = useState(false)
  const [lastUpdate, setLastUpdate] = useState<Date>(new Date())
  
  const campuses = [
    { id: 1, name: '花园校区', code: 'HY' },
    { id: 2, name: '龙子湖校区', code: 'LZ' },
    { id: 3, name: '江淮校区', code: 'JH' }
  ]
  
  const [buildings, setBuildings] = useState<BuildingInfo[]>([])
  const [buildingConfig, setBuildingConfig] = useState({ floors: 6, rooms: 10 })
  const [loadingBuildings, setLoadingBuildings] = useState(true)
  
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
      const data = await iotApi.getBatchFlow(deviceIds)
      setDeviceData(data)
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
  const avgFlow = onlineCount > 0 
    ? deviceData.filter(d => d.status === 'online').reduce((sum, d) => sum + d.flow, 0) / onlineCount 
    : 0
  const totalFlow = deviceData.reduce((sum, d) => sum + d.flow, 0)
  const avgPressure = onlineCount > 0 ? deviceData.filter(d => d.status === 'online').reduce((s, d) => s + (d.pressure || 0), 0) / onlineCount : 0
  const avgTemperature = onlineCount > 0 ? deviceData.filter(d => d.status === 'online').reduce((s, d) => s + (d.temperature || 0), 0) / onlineCount : 0
  
  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* Sidebar */}
      <aside className={`${sidebarOpen ? 'w-64' : 'w-20'} bg-white shadow-lg transition-all duration-300 flex flex-col h-screen`}>
        <div className="p-6 border-b border-gray-200">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 bg-primary-100 rounded-lg">
              <Droplets className="w-6 h-6 text-primary-600" />
            </div>
            {sidebarOpen && (
              <h1 className="text-lg font-bold text-gray-900">水务平台</h1>
            )}
          </div>
        </div>

        {/* 返回主界面 */}
        <div className="p-2 border-b border-gray-200">
          <button onClick={() => navigate('/dashboard')} className={`flex items-center gap-2 px-3 py-2 rounded-lg bg-primary-50 text-primary-700 hover:bg-primary-100 text-sm ${sidebarOpen ? 'w-full' : 'mx-auto justify-center'}`}>
            <LayoutDashboard className="w-5 h-5" />
            {sidebarOpen && <span>返回主界面</span>}
          </button>
        </div>

        {/* 校区选择 */}
        <nav className="flex-1 p-2 overflow-y-auto">
          <p className="px-4 mb-2 text-xs font-medium text-gray-400 uppercase">切换校区</p>
          <div className="space-y-1 mb-6">
            {campuses.map((campus) => (
              <button
                key={campus.id}
                onClick={() => { setSelectedCampus(campus.id); setSelectedBuilding(null) }}
                className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-lg transition-all ${
                  selectedCampus === campus.id
                    ? 'bg-primary-50 text-primary-700 border border-primary-200'
                    : 'text-gray-600 hover:bg-gray-50'
                }`}
              >
                <div className={`w-2 h-2 rounded-full ${selectedCampus === campus.id ? 'bg-primary-600' : 'bg-gray-300'}`} />
                {sidebarOpen && (
                  <div className="flex-1 text-left">
                    <div className="text-sm font-medium">{campus.name}</div>
                    <div className="text-xs text-gray-400">{campus.code}</div>
                  </div>
                )}
              </button>
            ))}
          </div>
          
          {/* 楼宇列表 */}
          <p className="px-4 mb-2 text-xs font-medium text-gray-400 uppercase">楼宇列表</p>
          {loadingBuildings ? (
            <div className="px-4 py-8 text-center">
              <RefreshCw className="w-5 h-5 text-gray-400 animate-spin mx-auto" />
              <p className="text-xs text-gray-500 mt-2">加载中...</p>
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
                  className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-lg transition-all ${
                    selectedBuilding?.id === building.id
                      ? 'bg-primary-50 text-primary-700 border border-primary-200'
                      : 'text-gray-600 hover:bg-gray-50 border border-transparent'
                  }`}
                >
                  <Icon className={`w-4 h-4 text-${config.color}-600`} />
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
                <p className="text-sm font-medium text-gray-900 truncate">{nickname || '用户'}</p>
                <p className="text-xs text-gray-500 truncate">UID: {uid || '未知'}</p>
              </div>
            )}
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col">
        <header className="bg-white shadow-sm">
          <div className="px-6 py-4 flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={() => setSidebarOpen(!sidebarOpen)}
                className="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg"
              >
                {sidebarOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
              </button>
              <h2 className="text-xl font-semibold text-gray-900">实时监测</h2>
              <div className="flex items-center gap-2 px-3 py-1.5 bg-gray-100 rounded-lg">
                <Activity className="w-4 h-4 text-green-500" />
                <span className="text-sm text-gray-600">实时</span>
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

        <main className="flex-1 p-6 overflow-auto">
          {!selectedBuilding ? (
            <div className="flex items-center justify-center h-full">
              <div className="text-center">
                <Building2 className="w-16 h-16 text-gray-300 mx-auto mb-4" />
                <p className="text-gray-500">请从左侧选择楼宇</p>
              </div>
            </div>
          ) : (
            <div className="space-y-6">
              {/* 统计卡片 */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="bg-white rounded-xl p-5 shadow-sm hover:shadow-md transition-shadow">
                  <div className="flex items-center gap-3 mb-2">
                    <div className="p-2.5 bg-blue-100 rounded-xl">
                      <Droplets className="w-5 h-5 text-blue-600" />
                    </div>
                  </div>
                  <p className="text-sm text-gray-500">总流量</p>
                  <p className="text-2xl font-bold text-gray-900">{totalFlow.toFixed(1)} <span className="text-sm font-normal text-gray-400">L/s</span></p>
                </div>
                <div className="bg-white rounded-xl p-5 shadow-sm hover:shadow-md transition-shadow">
                  <div className="flex items-center gap-3 mb-2">
                    <div className="p-2.5 bg-purple-100 rounded-xl">
                      <Activity className="w-5 h-5 text-purple-600" />
                    </div>
                  </div>
                  <p className="text-sm text-gray-500">平均流量</p>
                  <p className="text-2xl font-bold text-gray-900">{avgFlow.toFixed(2)} <span className="text-sm font-normal text-gray-400">L/s</span></p>
                </div>
                <div className="bg-white rounded-xl p-5 shadow-sm hover:shadow-md transition-shadow">
                  <div className="flex items-center gap-3 mb-2">
                    <div className="p-2.5 bg-green-100 rounded-xl">
                      <CheckCircle className="w-5 h-5 text-green-600" />
                    </div>
                  </div>
                  <p className="text-sm text-gray-500">在线设备</p>
                  <p className="text-2xl font-bold text-green-600">{onlineCount}</p>
                </div>
                <div className="bg-white rounded-xl p-5 shadow-sm hover:shadow-md transition-shadow">
                  <div className="flex items-center gap-3 mb-2">
                    <div className="p-2.5 bg-red-100 rounded-xl">
                      <XCircle className="w-5 h-5 text-red-600" />
                    </div>
                  </div>
                  <p className="text-sm text-gray-500">离线设备</p>
                  <p className="text-2xl font-bold text-red-600">{offlineCount}</p>
                </div>
              </div>

              {/* 三个监测仪表盘 */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                <div className="bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl p-4 shadow-sm">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-medium text-gray-700">流量监测</span>
                    <GaugeMeter value={totalFlow} max={Math.max(totalFlow * 1.5, 50)} size={80} color="#3b82f6" />
                  </div>
                  <p className="text-xl font-bold text-blue-600">{totalFlow.toFixed(1)} L/s</p>
                </div>
                <div className="bg-gradient-to-br from-cyan-50 to-teal-50 rounded-xl p-4 shadow-sm">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-medium text-gray-700">水压监测</span>
                    <GaugeMeter value={avgPressure} max={Math.max(avgPressure * 1.5, 1)} size={80} color="#06b6d4" />
                  </div>
                  <p className="text-xl font-bold text-cyan-600">{avgPressure.toFixed(2)} MPa</p>
                </div>
                <div className="bg-gradient-to-br from-orange-50 to-red-50 rounded-xl p-4 shadow-sm">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-medium text-gray-700">水温监测</span>
                    <GaugeMeter value={avgTemperature} max={Math.max(avgTemperature * 1.5, 30)} size={80} color="#f97316" />
                  </div>
                  <p className="text-xl font-bold text-orange-600">{avgTemperature.toFixed(1)} °C</p>
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
                  <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
                    {deviceData.map((device) => (
                      <div
                        key={device.deviceId}
                        className={`p-4 rounded-xl border-2 transition-all hover:shadow-md ${
                          device.status === 'online' 
                            ? 'bg-white border-gray-100 hover:border-blue-200 hover:-translate-y-1' 
                            : 'bg-gray-50 border-gray-100 opacity-75'
                        }`}
                      >
                        <div className="text-center">
                          <p className="text-sm font-semibold text-gray-800">
                            {parseDeviceCode(device.deviceId)?.unitNo}
                          </p>
                          <p className="text-xs text-gray-400 mb-2">单元</p>
                          {device.status === 'online' ? (
                            <>
                              <p className="text-2xl font-bold text-blue-600">{device.flow.toFixed(1)}</p>
                              <p className="text-xs text-gray-400">L/s</p>
                            </>
                          ) : (
                            <div className="flex items-center justify-center gap-1 text-red-500">
                              <XCircle className="w-4 h-4" />
                              <span className="text-xs">离线</span>
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  )
}
