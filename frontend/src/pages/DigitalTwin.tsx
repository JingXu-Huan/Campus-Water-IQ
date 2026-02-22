import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { iotApi, generateDeviceId, generateWaterQualitySensorId } from '@/api/iot'
import { 
  Droplets, User, Menu, X, Activity, LayoutDashboard, 
  Play, Square, RotateCcw, Power, PowerOff, AlertCircle, CheckCircle,
  Gauge, RefreshCw, Cpu
} from 'lucide-react'

type SimMode = 'normal' | 'leaking' | 'burstPipe' | 'shows'

// 校区选项
const CAMPUS_OPTIONS = [
  { value: 1, label: '花园校区' },
  { value: 2, label: '龙子湖校区' },
  { value: 3, label: '江淮校区' },
]

export default function DigitalTwin() {
  const navigate = useNavigate()
  const { uid, nickname, avatar } = useAuthStore()
  const [sidebarOpen, setSidebarOpen] = useState(true)
  
  // 设备状态
  const [deviceCount, setDeviceCount] = useState(0)
  const [isInitialized, setIsInitialized] = useState(false)
  const [isMetersRunning, setIsMetersRunning] = useState(false)
  const [isSensorsRunning, setIsSensorsRunning] = useState(false)
  const [isValvesOpen, setIsValvesOpen] = useState(true)
  const [simMode, setSimMode] = useState<SimMode>('normal')
  
  // 任务运行状态（用于判断是否能重置）
  const [isAnyTaskRunning, setIsAnyTaskRunning] = useState(false)
  
  // 楼宇配置
  const [buildingConfig, setBuildingConfig] = useState({
    dormitoryBuildings: 3,
    educationBuildings: 2,
    experimentBuildings: 1,
    floors: 6,
    rooms: 10
  })
  
  // 下线选择状态 - 水表
  const [meterOfflineSelect, setMeterOfflineSelect] = useState({
    campus: 1,
    building: 1,
    floor: 1,
    unit: 1
  })
  
  // 下线选择状态 - 传感器
  const [sensorOfflineSelect, setSensorOfflineSelect] = useState({
    campus: 1,
    building: 1,
    floor: 1
  })
  
  // 加载状态
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null)

  // 获取设备状态
  const fetchDeviceStatus = async () => {
    try {
      console.log('开始获取设备状态...')
      const count = await iotApi.getDeviceNums()
      console.log('getDeviceNums 完成, count =', count)
      const initialized = await iotApi.checkIsInitialized()
      console.log('checkIsInitialized 完成, initialized =', initialized, '类型:', typeof initialized)
      
      // 获取任务运行状态
      const taskStatus = await iotApi.getTaskStatus()
      console.log('taskStatus:', taskStatus)
      
      console.log('赋值前: setDeviceCount(', count, '), setIsInitialized(', initialized, ')')
      setDeviceCount(count)
      setIsInitialized(initialized)
      setIsMetersRunning(taskStatus.meterRunning)
      setIsSensorsRunning(taskStatus.sensorRunning)
      setIsAnyTaskRunning(taskStatus.meterRunning || taskStatus.sensorRunning)
      console.log('赋值完成')
    } catch (error) {
      console.error('获取设备状态失败:', error)
    }
  }

  // 获取楼宇配置
  const fetchBuildingConfig = async () => {
    try {
      const config = await iotApi.getBuildingConfig()
      // 根据配置计算各类楼宇数量
      const total = config.totalBuildings
      const edu = config.educationStart
      const exp = config.experimentStart
      setBuildingConfig({
        educationBuildings: edu,
        experimentBuildings: exp - edu,
        dormitoryBuildings: total - exp + 1,
        floors: config.floors,
        rooms: config.rooms
      })
    } catch (error) {
      console.error('获取配置失败:', error)
    }
  }

  useEffect(() => {
    fetchDeviceStatus()
    fetchBuildingConfig()
  }, [])

  // 显示消息
  const showMessage = (type: 'success' | 'error', text: string) => {
    setMessage({ type, text })
    setTimeout(() => setMessage(null), 3000)
  }

  // 显示操作结果消息
  const showResultMessage = (result: { success: boolean; message: string }, successText?: string) => {
    if (result.success) {
      showMessage('success', successText || result.message || '操作成功')
    } else {
      showMessage('error', result.message || '操作失败')
    }
  }

  // 初始化设备
  const handleInit = async () => {
    if (!confirm('确认初始化设备？这将创建新的虚拟设备。')) return
    
    setLoading(true)
    try {
      const result = await iotApi.initDevices(buildingConfig)
      if (result.success) {
        showMessage('success', '设备初始化成功')
        await fetchDeviceStatus()
      } else {
        showMessage('error', result.message)
      }
    } catch (error) {
      showMessage('error', '初始化失败')
    } finally {
      setLoading(false)
    }
  }

  // 重置设备（先停止再重置）
  const handleReset = async () => {
    if (!confirm('确认重置所有设备？这将停止所有设备并清除数据。')) return
    
    setLoading(true)
    try {
      // 先停止水表和传感器
      await iotApi.stopAllMeters()
      await iotApi.stopAllSensors()
      
      const result = await iotApi.resetDevices()
      if (result.success) {
        showMessage('success', '设备重置成功')
        await fetchDeviceStatus()
      } else {
        showMessage('error', result.message)
      }
    } catch (error) {
      showMessage('error', '重置失败')
    } finally {
      setLoading(false)
    }
  }

  // 停止水表
  const handleStopMeters = async () => {
    setLoading(true)
    try {
      const result = await iotApi.stopAllMeters()
      showResultMessage(result)
      if (result.success) {
        await fetchDeviceStatus()
      }
    } catch (error) {
      showMessage('error', '停止水表失败')
    } finally {
      setLoading(false)
    }
  }

  // 停止传感器
  const handleStopSensors = async () => {
    setLoading(true)
    try {
      const result = await iotApi.stopAllSensors()
      showResultMessage(result)
      if (result.success) {
        await fetchDeviceStatus()
      }
    } catch (error) {
      showMessage('error', '停止传感器失败')
    } finally {
      setLoading(false)
    }
  }

  // 水表下线
  const handleOfflineMeters = async () => {
    const { campus, building, floor, unit } = meterOfflineSelect
    const deviceId = generateDeviceId(campus, building, floor, unit)
    const locationLabel = `${CAMPUS_OPTIONS.find(c => c.value === campus)?.label} ${building}号楼 ${floor}层 ${unit}室`
    
    if (!confirm(`确认水表设备下线？\n位置: ${locationLabel}\n设备ID: ${deviceId}`)) return
    
    setLoading(true)
    try {
      const result = await iotApi.offlineMeters([deviceId])
      showResultMessage(result, `水表 ${locationLabel} 已下线`)
      if (result.success) {
        await fetchDeviceStatus()
      }
    } catch (error) {
      showMessage('error', '水表下线失败')
    } finally {
      setLoading(false)
    }
  }

  // 传感器下线
  const handleOfflineSensors = async () => {
    const { campus, building, floor } = sensorOfflineSelect
    const deviceId = generateWaterQualitySensorId(campus, building, floor)
    const locationLabel = `${CAMPUS_OPTIONS.find(c => c.value === campus)?.label} ${building}号楼 ${floor}层`
    
    if (!confirm(`确认传感器设备下线？\n位置: ${locationLabel}\n设备ID: ${deviceId}`)) return
    
    setLoading(true)
    try {
      const result = await iotApi.offlineSensors([deviceId])
      showResultMessage(result, `传感器 ${locationLabel} 已下线`)
      if (result.success) {
        await fetchDeviceStatus()
      }
    } catch (error) {
      showMessage('error', '传感器下线失败')
    } finally {
      setLoading(false)
    }
  }

  // 切换阀门
  const handleToggleValves = async () => {
    setLoading(true)
    try {
      const result = isValvesOpen 
        ? await iotApi.closeAllValves()
        : await iotApi.openAllValves()
      
      if (result.success) {
        showMessage('success', result.message)
        setIsValvesOpen(!isValvesOpen)
      } else {
        showMessage('error', result.message)
      }
    } catch (error) {
      showMessage('error', '操作失败')
    } finally {
      setLoading(false)
    }
  }

  // 切换模拟模式
  const handleChangeMode = async (mode: SimMode) => {
    setLoading(true)
    try {
      const result = await iotApi.changeSimulatorMode(mode)
      if (result.success) {
        showMessage('success', `已切换至${mode === 'normal' ? '正常' : mode === 'leaking' ? '漏水' : '爆管'}模式`)
        setSimMode(mode)
      } else {
        showMessage('error', result.message)
      }
    } catch (error) {
      showMessage('error', '切换模式失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="h-screen bg-gray-50 flex overflow-hidden">
      {/* Sidebar */}
      <aside className={`${sidebarOpen ? 'w-64' : 'w-20'} bg-white shadow-lg transition-all duration-300 flex flex-col h-screen`}>
        <div className="p-4 border-b border-gray-200 flex-shrink-0">
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
        <div className="p-2 border-b border-gray-200 flex-shrink-0">
          <button onClick={() => navigate('/dashboard')} className={`flex items-center gap-2 px-3 py-2 rounded-lg bg-primary-50 text-primary-700 hover:bg-primary-100 text-sm ${sidebarOpen ? 'w-full' : 'mx-auto justify-center'}`}>
            <LayoutDashboard className="w-5 h-5" />
            {sidebarOpen && <span>返回主界面</span>}
          </button>
        </div>

        {/* 导航菜单 */}
        <nav className="flex-1 p-2 overflow-y-auto">
          <div className="space-y-1">
            <button
              onClick={() => navigate('/dashboard')}
              className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-lg transition-all text-gray-600 hover:bg-gray-50`}
            >
              <Activity className="w-5 h-5" />
              {sidebarOpen && <span>数据概览</span>}
            </button>
            <button
              onClick={() => navigate('/monitoring')}
              className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-lg transition-all text-gray-600 hover:bg-gray-50`}
            >
              <Gauge className="w-5 h-5" />
              {sidebarOpen && <span>实时监测</span>}
            </button>
            <button
              onClick={() => {}}
              className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-lg transition-all bg-primary-50 text-primary-700 border border-primary-200`}
            >
              <Cpu className="w-5 h-5" />
              {sidebarOpen && <span>数字孪生</span>}
            </button>
          </div>
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
              <h2 className="text-xl font-semibold text-gray-900">数字孪生</h2>
              <div className="flex items-center gap-2 px-3 py-1.5 bg-gray-100 rounded-lg">
                <Cpu className="w-4 h-4 text-primary-500" />
                <span className="text-sm text-gray-600">虚拟设备管理</span>
              </div>
            </div>
            <button
              onClick={() => { fetchDeviceStatus(); fetchBuildingConfig(); }}
              className="flex items-center gap-2 px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
              刷新
            </button>
          </div>
        </header>

        <main className="flex-1 p-6 overflow-y-auto">
          {/* 消息提示 */}
          {message && (
            <div className={`mb-4 p-4 rounded-lg flex items-center gap-2 ${
              message.type === 'success' ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'
            }`}>
              {message.type === 'success' ? <CheckCircle className="w-5 h-5" /> : <AlertCircle className="w-5 h-5" />}
              {message.text}
            </div>
          )}

          {/* 状态概览 */}
          <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">
            <div className="bg-white rounded-xl p-5 shadow-sm">
              <div className="flex items-center gap-3 mb-2">
                <div className={`p-2.5 rounded-xl ${isInitialized ? 'bg-green-100' : 'bg-gray-100'}`}>
                  <Cpu className={`w-5 h-5 ${isInitialized ? 'text-green-600' : 'text-gray-400'}`} />
                </div>
              </div>
              <p className="text-sm text-gray-500">设备状态</p>
              <p className={`text-xl font-bold ${isInitialized ? 'text-green-600' : 'text-gray-400'}`}>
                {isInitialized ? '已初始化' : '未初始化'}
              </p>
            </div>

            <div className="bg-white rounded-xl p-5 shadow-sm">
              <div className="flex items-center gap-3 mb-2">
                <div className="p-2.5 bg-blue-100 rounded-xl">
                  <Droplets className="w-5 h-5 text-blue-600" />
                </div>
              </div>
              <p className="text-sm text-gray-500">水表数量</p>
              <p className="text-xl font-bold text-gray-900">{deviceCount}</p>
            </div>

            <div className="bg-white rounded-xl p-5 shadow-sm">
              <div className="flex items-center gap-3 mb-2">
                <div className={`p-2.5 rounded-xl ${isMetersRunning ? 'bg-green-100' : 'bg-gray-100'}`}>
                  <Play className={`w-5 h-5 ${isMetersRunning ? 'text-green-600' : 'text-gray-400'}`} />
                </div>
              </div>
              <p className="text-sm text-gray-500">水表运行</p>
              <p className={`text-xl font-bold ${isMetersRunning ? 'text-green-600' : 'text-gray-400'}`}>
                {isMetersRunning ? '运行中' : '已停止'}
              </p>
            </div>

            <div className="bg-white rounded-xl p-5 shadow-sm">
              <div className="flex items-center gap-3 mb-2">
                <div className="p-2.5 bg-purple-100 rounded-xl">
                  <Activity className="w-5 h-5 text-purple-600" />
                </div>
              </div>
              <p className="text-sm text-gray-500">传感器数量</p>
              <p className="text-xl font-bold text-gray-900">{(buildingConfig.educationBuildings + buildingConfig.experimentBuildings + buildingConfig.dormitoryBuildings) * buildingConfig.floors}</p>
            </div>

            <div className="bg-white rounded-xl p-5 shadow-sm">
              <div className="flex items-center gap-3 mb-2">
                <div className={`p-2.5 rounded-xl ${isSensorsRunning ? 'bg-green-100' : 'bg-gray-100'}`}>
                  <Play className={`w-5 h-5 ${isSensorsRunning ? 'text-green-600' : 'text-gray-400'}`} />
                </div>
              </div>
              <p className="text-sm text-gray-500">传感器运行</p>
              <p className={`text-xl font-bold ${isSensorsRunning ? 'text-green-600' : 'text-gray-400'}`}>
                {isSensorsRunning ? '运行中' : '已停止'}
              </p>
            </div>
          </div>

          {/* 楼宇配置 */}
          <div className="bg-white rounded-xl p-6 shadow-sm mb-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">楼宇配置</h3>
            <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
              <div>
                <label className="block text-sm text-gray-500 mb-1">教学楼数量</label>
                <input
                  type="number"
                  min={1}
                  max={30}
                  value={buildingConfig.educationBuildings}
                  onChange={(e) => setBuildingConfig({ ...buildingConfig, educationBuildings: parseInt(e.target.value) || 1 })}
                  disabled={isInitialized}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg disabled:bg-gray-100 disabled:cursor-not-allowed"
                />
              </div>
              <div>
                <label className="block text-sm text-gray-500 mb-1">实验楼数量</label>
                <input
                  type="number"
                  min={1}
                  max={30}
                  value={buildingConfig.experimentBuildings}
                  onChange={(e) => setBuildingConfig({ ...buildingConfig, experimentBuildings: parseInt(e.target.value) || 1 })}
                  disabled={isInitialized}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg disabled:bg-gray-100 disabled:cursor-not-allowed"
                />
              </div>
              <div>
                <label className="block text-sm text-gray-500 mb-1">宿舍楼数量</label>
                <input
                  type="number"
                  min={1}
                  max={33}
                  value={buildingConfig.dormitoryBuildings}
                  onChange={(e) => setBuildingConfig({ ...buildingConfig, dormitoryBuildings: parseInt(e.target.value) || 1 })}
                  disabled={isInitialized}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg disabled:bg-gray-100 disabled:cursor-not-allowed"
                />
              </div>
              <div>
                <label className="block text-sm text-gray-500 mb-1">楼层数</label>
                <input
                  type="number"
                  min={1}
                  max={99}
                  value={buildingConfig.floors}
                  onChange={(e) => setBuildingConfig({ ...buildingConfig, floors: parseInt(e.target.value) || 1 })}
                  disabled={isInitialized}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg disabled:bg-gray-100 disabled:cursor-not-allowed"
                />
              </div>
              <div>
                <label className="block text-sm text-gray-500 mb-1">每层单元数</label>
                <input
                  type="number"
                  min={1}
                  max={999}
                  value={buildingConfig.rooms}
                  onChange={(e) => setBuildingConfig({ ...buildingConfig, rooms: parseInt(e.target.value) || 1 })}
                  disabled={isInitialized}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg disabled:bg-gray-100 disabled:cursor-not-allowed"
                />
              </div>
            </div>
            <p className="text-sm text-gray-400 mt-2">
              总设备数: {buildingConfig.dormitoryBuildings + buildingConfig.educationBuildings + buildingConfig.experimentBuildings} 栋 × {buildingConfig.floors} 层 × {buildingConfig.rooms} 单元 = {(buildingConfig.dormitoryBuildings + buildingConfig.educationBuildings + buildingConfig.experimentBuildings) * buildingConfig.floors * buildingConfig.rooms} 个水表 + {(buildingConfig.dormitoryBuildings + buildingConfig.educationBuildings + buildingConfig.experimentBuildings) * buildingConfig.floors} 个传感器
            </p>
          </div>

          {/* 公共控制：初始化/重置 */}
          <div className="bg-white rounded-xl p-6 shadow-sm mb-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">设备初始化与重置</h3>
            <div className="grid grid-cols-2 gap-4 max-w-2xl">
              <button
                onClick={handleInit}
                disabled={loading || isInitialized}
                className="flex flex-col items-center gap-2 p-4 bg-blue-50 hover:bg-blue-100 rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Cpu className="w-8 h-8 text-blue-600" />
                <span className="font-medium text-blue-700">初始化设备</span>
                <span className="text-xs text-blue-500">创建虚拟设备</span>
              </button>

              <button
                onClick={handleReset}
                disabled={loading || !isInitialized || isAnyTaskRunning}
                className="flex flex-col items-center gap-2 p-4 bg-red-50 hover:bg-red-100 rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <RotateCcw className="w-8 h-8 text-red-600" />
                <span className="font-medium text-red-700">重置设备</span>
                <span className="text-xs text-red-500">{isAnyTaskRunning ? '请先停止设备' : '清除所有数据'}</span>
              </button>
            </div>
          </div>

          {/* 水表控制卡片 */}
          <div className="bg-white rounded-xl p-6 shadow-sm mb-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-2 bg-blue-100 rounded-lg">
                <Droplets className="w-5 h-5 text-blue-600" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900">水表控制</h3>
              <span className={`ml-auto px-3 py-1 rounded-full text-sm font-medium ${
                isMetersRunning ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
              }`}>
                {isMetersRunning ? '运行中' : '已停止'}
              </span>
            </div>
            
            {/* 下线位置选择 */}
            <div className="mb-4 p-4 bg-gray-50 rounded-lg">
              <p className="text-sm font-medium text-gray-700 mb-3">下线位置选择</p>
              <div className="grid grid-cols-4 gap-3">
                <div>
                  <label className="block text-xs text-gray-500 mb-1">校区</label>
                  <select
                    value={meterOfflineSelect.campus}
                    onChange={(e) => setMeterOfflineSelect({ ...meterOfflineSelect, campus: parseInt(e.target.value), building: 1, floor: 1, unit: 1 })}
                    className="w-full px-2 py-2 border border-gray-300 rounded-lg text-sm"
                  >
                    {CAMPUS_OPTIONS.map(c => (
                      <option key={c.value} value={c.value}>{c.label}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs text-gray-500 mb-1">楼栋</label>
                  <select
                    value={meterOfflineSelect.building}
                    onChange={(e) => setMeterOfflineSelect({ ...meterOfflineSelect, building: parseInt(e.target.value), floor: 1, unit: 1 })}
                    className="w-full px-2 py-2 border border-gray-300 rounded-lg text-sm"
                  >
                    {Array.from({ length: buildingConfig.educationBuildings + buildingConfig.experimentBuildings + buildingConfig.dormitoryBuildings }, (_, i) => i + 1).map(b => (
                      <option key={b} value={b}>{b}号楼</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs text-gray-500 mb-1">楼层</label>
                  <select
                    value={meterOfflineSelect.floor}
                    onChange={(e) => setMeterOfflineSelect({ ...meterOfflineSelect, floor: parseInt(e.target.value), unit: 1 })}
                    className="w-full px-2 py-2 border border-gray-300 rounded-lg text-sm"
                  >
                    {Array.from({ length: buildingConfig.floors }, (_, i) => i + 1).map(f => (
                      <option key={f} value={f}>{f}层</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs text-gray-500 mb-1">单元</label>
                  <select
                    value={meterOfflineSelect.unit}
                    onChange={(e) => setMeterOfflineSelect({ ...meterOfflineSelect, unit: parseInt(e.target.value) })}
                    className="w-full px-2 py-2 border border-gray-300 rounded-lg text-sm"
                  >
                    {Array.from({ length: buildingConfig.rooms }, (_, i) => i + 1).map(u => (
                      <option key={u} value={u}>{u}室</option>
                    ))}
                  </select>
                </div>
              </div>
            </div>
            
            {/* 全部操作 */}
            <div className="mb-4">
              <p className="text-sm font-medium text-gray-700 mb-3">全部操作</p>
              <div className="grid grid-cols-3 gap-4">
                <button
                  onClick={() => {
                    if (!isInitialized) { showMessage('error', '请先初始化设备'); return; }
                    iotApi.startAllMeters().then(result => {
                      showResultMessage(result, '水表已开启')
                      if (result.success) fetchDeviceStatus()
                    })
                  }}
                  disabled={loading || !isInitialized || isMetersRunning}
                  className="flex flex-col items-center gap-2 p-4 bg-green-50 hover:bg-green-100 rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Play className="w-8 h-8 text-green-600" />
                  <span className="font-medium text-green-700">开启全部</span>
                  <span className="text-xs text-green-500">启动数据上报</span>
                </button>

                <button
                  onClick={handleStopMeters}
                  disabled={loading || !isMetersRunning}
                  className="flex flex-col items-center gap-2 p-4 bg-yellow-50 hover:bg-yellow-100 rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Square className="w-8 h-8 text-yellow-600" />
                  <span className="font-medium text-yellow-700">停止全部</span>
                  <span className="text-xs text-yellow-500">停止数据上报</span>
                </button>

                <button
                  onClick={handleOfflineMeters}
                  disabled={loading || !isInitialized}
                  className="flex flex-col items-center gap-2 p-4 bg-gray-50 hover:bg-gray-100 rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <PowerOff className="w-8 h-8 text-gray-600" />
                  <span className="font-medium text-gray-700">下线全部</span>
                  <span className="text-xs text-gray-500">停止并下线</span>
                </button>
              </div>
            </div>
            
            {/* 批量操作 */}
            <div>
              <p className="text-sm font-medium text-gray-700 mb-3">批量操作</p>
              <div className="p-4 bg-gray-50 rounded-lg">
                <div className="grid grid-cols-4 gap-3 mb-4">
                  <div>
                    <label className="block text-xs text-gray-500 mb-1">校区</label>
                    <select
                      value={meterOfflineSelect.campus}
                      onChange={(e) => setMeterOfflineSelect({ ...meterOfflineSelect, campus: parseInt(e.target.value), building: 1, floor: 1, unit: 1 })}
                      className="w-full px-2 py-2 border border-gray-300 rounded-lg text-sm"
                    >
                      {CAMPUS_OPTIONS.map(c => (
                        <option key={c.value} value={c.value}>{c.label}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs text-gray-500 mb-1">楼栋</label>
                    <select
                      value={meterOfflineSelect.building}
                      onChange={(e) => setMeterOfflineSelect({ ...meterOfflineSelect, building: parseInt(e.target.value), floor: 1, unit: 1 })}
                      className="w-full px-2 py-2 border border-gray-300 rounded-lg text-sm"
                    >
                      {Array.from({ length: buildingConfig.educationBuildings + buildingConfig.experimentBuildings + buildingConfig.dormitoryBuildings }, (_, i) => i + 1).map(b => (
                        <option key={b} value={b}>{b}号楼</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs text-gray-500 mb-1">楼层</label>
                    <select
                      value={meterOfflineSelect.floor}
                      onChange={(e) => setMeterOfflineSelect({ ...meterOfflineSelect, floor: parseInt(e.target.value), unit: 1 })}
                      className="w-full px-2 py-2 border border-gray-300 rounded-lg text-sm"
                    >
                      {Array.from({ length: buildingConfig.floors }, (_, i) => i + 1).map(f => (
                        <option key={f} value={f}>{f}层</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs text-gray-500 mb-1">单元</label>
                    <select
                      value={meterOfflineSelect.unit}
                      onChange={(e) => setMeterOfflineSelect({ ...meterOfflineSelect, unit: parseInt(e.target.value) })}
                      className="w-full px-2 py-2 border border-gray-300 rounded-lg text-sm"
                    >
                      {Array.from({ length: buildingConfig.rooms }, (_, i) => i + 1).map(u => (
                        <option key={u} value={u}>{u}室</option>
                      ))}
                    </select>
                  </div>
                </div>
                <div className="grid grid-cols-3 gap-3">
                  <button
                    onClick={() => {
                      if (!isInitialized) { showMessage('error', '请先初始化设备'); return; }
                      const deviceId = generateDeviceId(meterOfflineSelect.campus, meterOfflineSelect.building, meterOfflineSelect.floor, meterOfflineSelect.unit)
                      const locationLabel = `${CAMPUS_OPTIONS.find(c => c.value === meterOfflineSelect.campus)?.label} ${meterOfflineSelect.building}号楼 ${meterOfflineSelect.floor}层 ${meterOfflineSelect.unit}室`
                      iotApi.startMeters([deviceId]).then(result => {
                        showResultMessage(result, `水表 ${locationLabel} 已开启`)
                        if (result.success) fetchDeviceStatus()
                      })
                    }}
                    disabled={loading || !isInitialized}
                    className="flex items-center justify-center gap-2 px-4 py-2 bg-green-50 hover:bg-green-100 rounded-lg text-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <Play className="w-4 h-4" /> 开启
                  </button>
                  <button
                    onClick={() => {
                      const deviceId = generateDeviceId(meterOfflineSelect.campus, meterOfflineSelect.building, meterOfflineSelect.floor, meterOfflineSelect.unit)
                      const locationLabel = `${CAMPUS_OPTIONS.find(c => c.value === meterOfflineSelect.campus)?.label} ${meterOfflineSelect.building}号楼 ${meterOfflineSelect.floor}层 ${meterOfflineSelect.unit}室`
                      iotApi.stopMeters([deviceId]).then(result => {
                        showResultMessage(result, `水表 ${locationLabel} 已停止`)
                        if (result.success) fetchDeviceStatus()
                      })
                    }}
                    disabled={loading}
                    className="flex items-center justify-center gap-2 px-4 py-2 bg-yellow-50 hover:bg-yellow-100 rounded-lg text-yellow-700 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <Square className="w-4 h-4" /> 停止
                  </button>
                  <button
                    onClick={handleOfflineMeters}
                    disabled={loading || !isInitialized}
                    className="flex items-center justify-center gap-2 px-4 py-2 bg-gray-50 hover:bg-gray-100 rounded-lg text-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <PowerOff className="w-4 h-4" /> 下线
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* 传感器控制卡片 */}
          <div className="bg-white rounded-xl p-6 shadow-sm mb-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-2 bg-purple-100 rounded-lg">
                <Activity className="w-5 h-5 text-purple-600" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900">传感器控制</h3>
              <span className={`ml-auto px-3 py-1 rounded-full text-sm font-medium ${
                isSensorsRunning ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
              }`}>
                {isSensorsRunning ? '运行中' : '已停止'}
              </span>
            </div>
            
            {/* 全部操作 */}
            <div className="mb-4">
              <p className="text-sm font-medium text-gray-700 mb-3">全部操作</p>
              <div className="grid grid-cols-3 gap-4">
                <button
                  onClick={() => {
                    if (!isInitialized) { showMessage('error', '请先初始化设备'); return; }
                    iotApi.startAllSensors().then(result => {
                      showResultMessage(result, '传感器已开启')
                      if (result.success) fetchDeviceStatus()
                    })
                  }}
                  disabled={loading || !isInitialized || isSensorsRunning}
                  className="flex flex-col items-center gap-2 p-4 bg-green-50 hover:bg-green-100 rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Play className="w-8 h-8 text-green-600" />
                  <span className="font-medium text-green-700">开启全部</span>
                  <span className="text-xs text-green-500">启动数据上报</span>
                </button>

                <button
                  onClick={handleStopSensors}
                  disabled={loading || !isSensorsRunning}
                  className="flex flex-col items-center gap-2 p-4 bg-yellow-50 hover:bg-yellow-100 rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Square className="w-8 h-8 text-yellow-600" />
                  <span className="font-medium text-yellow-700">停止全部</span>
                  <span className="text-xs text-yellow-500">停止数据上报</span>
                </button>

                <button
                  onClick={handleOfflineSensors}
                  disabled={loading || !isInitialized}
                  className="flex flex-col items-center gap-2 p-4 bg-gray-50 hover:bg-gray-100 rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <PowerOff className="w-8 h-8 text-gray-600" />
                  <span className="font-medium text-gray-700">下线全部</span>
                  <span className="text-xs text-gray-500">停止并下线</span>
                </button>
              </div>
            </div>
            
            {/* 批量操作 */}
            <div>
              <p className="text-sm font-medium text-gray-700 mb-3">批量操作</p>
              <div className="p-4 bg-gray-50 rounded-lg">
                <div className="grid grid-cols-3 gap-3 mb-4">
                  <div>
                    <label className="block text-xs text-gray-500 mb-1">校区</label>
                    <select
                      value={sensorOfflineSelect.campus}
                      onChange={(e) => setSensorOfflineSelect({ ...sensorOfflineSelect, campus: parseInt(e.target.value), building: 1, floor: 1 })}
                      className="w-full px-2 py-2 border border-gray-300 rounded-lg text-sm"
                    >
                      {CAMPUS_OPTIONS.map(c => (
                        <option key={c.value} value={c.value}>{c.label}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs text-gray-500 mb-1">楼栋</label>
                    <select
                      value={sensorOfflineSelect.building}
                      onChange={(e) => setSensorOfflineSelect({ ...sensorOfflineSelect, building: parseInt(e.target.value), floor: 1 })}
                      className="w-full px-2 py-2 border border-gray-300 rounded-lg text-sm"
                    >
                      {Array.from({ length: buildingConfig.educationBuildings + buildingConfig.experimentBuildings + buildingConfig.dormitoryBuildings }, (_, i) => i + 1).map(b => (
                        <option key={b} value={b}>{b}号楼</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs text-gray-500 mb-1">楼层</label>
                    <select
                      value={sensorOfflineSelect.floor}
                      onChange={(e) => setSensorOfflineSelect({ ...sensorOfflineSelect, floor: parseInt(e.target.value) })}
                      className="w-full px-2 py-2 border border-gray-300 rounded-lg text-sm"
                    >
                      {Array.from({ length: buildingConfig.floors }, (_, i) => i + 1).map(f => (
                        <option key={f} value={f}>{f}层</option>
                      ))}
                    </select>
                  </div>
                </div>
                <div className="grid grid-cols-3 gap-3">
                  <button
                    onClick={() => {
                      if (!isInitialized) { showMessage('error', '请先初始化设备'); return; }
                      const deviceId = generateWaterQualitySensorId(sensorOfflineSelect.campus, sensorOfflineSelect.building, sensorOfflineSelect.floor)
                      const locationLabel = `${CAMPUS_OPTIONS.find(c => c.value === sensorOfflineSelect.campus)?.label} ${sensorOfflineSelect.building}号楼 ${sensorOfflineSelect.floor}层`
                      iotApi.startSensors([deviceId]).then(result => {
                        showResultMessage(result, `传感器 ${locationLabel} 已开启`)
                        if (result.success) fetchDeviceStatus()
                      })
                    }}
                    disabled={loading || !isInitialized}
                    className="flex items-center justify-center gap-2 px-4 py-2 bg-green-50 hover:bg-green-100 rounded-lg text-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <Play className="w-4 h-4" /> 开启
                  </button>
                  <button
                    onClick={() => {
                      const deviceId = generateWaterQualitySensorId(sensorOfflineSelect.campus, sensorOfflineSelect.building, sensorOfflineSelect.floor)
                      const locationLabel = `${CAMPUS_OPTIONS.find(c => c.value === sensorOfflineSelect.campus)?.label} ${sensorOfflineSelect.building}号楼 ${sensorOfflineSelect.floor}层`
                      iotApi.stopSensors([deviceId]).then(result => {
                        showResultMessage(result, `传感器 ${locationLabel} 已停止`)
                        if (result.success) fetchDeviceStatus()
                      })
                    }}
                    disabled={loading}
                    className="flex items-center justify-center gap-2 px-4 py-2 bg-yellow-50 hover:bg-yellow-100 rounded-lg text-yellow-700 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <Square className="w-4 h-4" /> 停止
                  </button>
                  <button
                    onClick={handleOfflineSensors}
                    disabled={loading || !isInitialized}
                    className="flex items-center justify-center gap-2 px-4 py-2 bg-gray-50 hover:bg-gray-100 rounded-lg text-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <PowerOff className="w-4 h-4" /> 下线
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* 阀门控制 */}
          <div className="bg-white rounded-xl p-6 shadow-sm mb-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">阀门控制</h3>
            <button
              onClick={handleToggleValves}
              disabled={loading || !isInitialized}
              className={`flex items-center gap-3 px-6 py-3 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed ${
                isValvesOpen 
                  ? 'bg-orange-100 hover:bg-orange-200 text-orange-700' 
                  : 'bg-green-100 hover:bg-green-200 text-green-700'
              }`}
            >
              {isValvesOpen ? <PowerOff className="w-5 h-5" /> : <Power className="w-5 h-5" />}
              <span className="font-medium">{isValvesOpen ? '关闭所有阀门' : '开启所有阀门'}</span>
            </button>
          </div>

          {/* 模拟模式 */}
          <div className="bg-white rounded-xl p-6 shadow-sm">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">模拟模式</h3>
            <div className="flex flex-wrap gap-3">
              <button
                onClick={() => handleChangeMode('normal')}
                disabled={loading}
                className={`px-6 py-3 rounded-lg font-medium transition-colors ${
                  simMode === 'normal'
                    ? 'bg-green-500 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                正常模式
              </button>
              <button
                onClick={() => handleChangeMode('shows')}
                disabled={loading}
                className={`px-6 py-3 rounded-lg font-medium transition-colors ${
                  simMode === 'shows'
                    ? 'bg-blue-500 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                演示模式
              </button>
              <button
                onClick={() => handleChangeMode('leaking')}
                disabled={loading}
                className={`px-6 py-3 rounded-lg font-medium transition-colors ${
                  simMode === 'leaking'
                    ? 'bg-yellow-500 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                漏水模式
              </button>
              <button
                onClick={() => handleChangeMode('burstPipe')}
                disabled={loading}
                className={`px-6 py-3 rounded-lg font-medium transition-colors ${
                  simMode === 'burstPipe'
                    ? 'bg-red-500 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                爆管模式
              </button>
            </div>
            <p className="text-sm text-gray-500 mt-3">
              当前模式: {simMode === 'normal' ? '正常' : simMode === 'leaking' ? '漏水检测' : simMode === 'burstPipe' ? '爆管模拟' : '演示模式'}
            </p>
          </div>
        </main>
      </div>
    </div>
  )
}
