import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { iotApi, generateDeviceId, generateWaterQualitySensorId, type IotDeviceConfig } from '@/api/iot'
import { 
  Droplets, User, Menu, X, Activity, LayoutDashboard, 
  Play, RotateCcw, Power, PowerOff, AlertCircle, CheckCircle,
  RefreshCw, Cpu, ChevronDown, AlertTriangle, Settings2, Cloud
} from 'lucide-react'
import NavigationMenu from '@/components/NavigationMenu'

type SimMode = 'normal' | 'leaking' | 'burstPipe' | 'shows'

// 校区选项
const CAMPUS_OPTIONS = [
  { value: 1, label: '花园校区' },
  { value: 2, label: '龙子湖校区' },
  { value: 3, label: '江淮校区' },
]

const formatConfigNumber = (value: number, maximumFractionDigits = 8) =>
  value.toLocaleString('zh-CN', { maximumFractionDigits })

function ConfigMetric({ label, value, unit }: { label: string; value: string | number; unit?: string }) {
  return (
    <div className="rounded-xl border border-slate-100 bg-slate-50/80 p-3">
      <p className="text-xs text-gray-500">{label}</p>
      <p className="mt-1 text-base font-semibold text-gray-900">
        {value} {unit && <span className="text-xs font-normal text-gray-500">{unit}</span>}
      </p>
    </div>
  )
}

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
  const [simTime, setSimTime] = useState<number>(43200) // 默认12点（秒）
  const [simSeason, setSimSeason] = useState<number>(1) // 默认春季
  
  // 任务运行状态（用于判断是否能重置）
  const [isAnyTaskRunning, setIsAnyTaskRunning] = useState(false)
  
  // 展开/收起状态
  const [meterExpanded, setMeterExpanded] = useState(false)
  const [sensorExpanded, setSensorExpanded] = useState(false)
  
  // 弹窗状态
  const [modal, setModal] = useState<{ show: boolean; type: 'success' | 'error' | 'info'; title: string; message: string } | null>(null)
  
  // 确认弹窗状态
  const [confirmModal, setConfirmModal] = useState<{ show: boolean; title: string; message: string; onConfirm: () => void } | null>(null)
  
  // 楼宇配置
  const [buildingConfig, setBuildingConfig] = useState({
    dormitoryBuildings: 3,
    educationBuildings: 2,
    experimentBuildings: 1,
    floors: 6,
    rooms: 10
  })

  // IoT-device 从 Nacos 加载后的当前生效配置（只读）
  const [deviceConfig, setDeviceConfig] = useState<IotDeviceConfig | null>(null)
  const [deviceConfigLoading, setDeviceConfigLoading] = useState(false)
  const [deviceConfigError, setDeviceConfigError] = useState<string | null>(null)
  
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

  // 获取 IoT-device 当前生效的 Nacos 配置
  const fetchDeviceConfig = async () => {
    setDeviceConfigLoading(true)
    setDeviceConfigError(null)
    try {
      const config = await iotApi.getDeviceConfig()
      setDeviceConfig(config)
    } catch (error) {
      console.error('获取 IoT-device Nacos 配置失败:', error)
      setDeviceConfig(null)
      setDeviceConfigError('暂时无法读取 IoT-device 当前配置，请确认服务和 Nacos 连接正常。')
    } finally {
      setDeviceConfigLoading(false)
    }
  }

  // 获取模拟模式
  const fetchSimulatorMode = async () => {
    try {
      const mode = await iotApi.getSimulatorMode()
      console.log('========== fetchSimulatorMode:', mode)
      if (mode) {
        setSimMode(mode as SimMode)
      }
    } catch (error) {
      console.error('获取模拟模式失败:', error)
    }
  }

  useEffect(() => {
    fetchDeviceStatus()
    fetchBuildingConfig()
    fetchDeviceConfig()
    fetchSimulatorMode()
    
    // 获取当前模拟季节
    iotApi.getSeason().then(season => {
      setSimSeason(season)
    }).catch(err => {
      console.error('获取季节失败:', err)
    })
    
    // 每5秒自动刷新设备状态
    const interval = setInterval(() => {
      fetchDeviceStatus()
    }, 5000)
    
    return () => clearInterval(interval)
  }, [])

  // 显示消息
  const showMessage = (type: 'success' | 'error', text: string) => {
    setMessage({ type, text })
  }

  // 显示弹窗
  const showModal = (type: 'success' | 'error' | 'info', title: string, message: string) => {
    setModal({ show: true, type, title, message })
  }

  // 显示操作结果消息
  const showResultMessage = (result: { success: boolean; message: string }, successText?: string) => {
    if (result.success) {
      showModal('success', '操作成功', successText || result.message || '操作成功')
    } else {
      showModal('error', '操作失败', result.message || '操作失败')
    }
  }

  // 初始化设备
  const handleInit = async () => {
    setConfirmModal({
      show: true,
      title: '确认初始化设备',
      message: '这将创建新的虚拟设备，是否继续？',
      onConfirm: async () => {
        setConfirmModal(null)
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
    })
  }

  // 重置设备（先停止再重置）
  const handleReset = async () => {
    setConfirmModal({
      show: true,
      title: '确认重置设备',
      message: '这将停止所有设备并清除数据，是否继续？',
      onConfirm: async () => {
        setConfirmModal(null)
        setLoading(true)
        try {
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
    })
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
    
    setConfirmModal({
      show: true,
      title: '确认水表设备下线',
      message: `位置: ${locationLabel}\n设备ID: ${deviceId}`,
      onConfirm: async () => {
        setConfirmModal(null)
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
    })
  }

  // 传感器下线
  const handleOfflineSensors = async () => {
    const { campus, building, floor } = sensorOfflineSelect
    const deviceId = generateWaterQualitySensorId(campus, building, floor)
    const locationLabel = `${CAMPUS_OPTIONS.find(c => c.value === campus)?.label} ${building}号楼 ${floor}层`
    
    setConfirmModal({
      show: true,
      title: '确认传感器设备下线',
      message: `位置: ${locationLabel}\n设备ID: ${deviceId}`,
      onConfirm: async () => {
        setConfirmModal(null)
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
    })
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
      // 先更新本地状态，让 UI 立即响应
      setSimMode(mode)
      
      const result = await iotApi.changeSimulatorMode(mode)
      showResultMessage(result, `已切换至${mode === 'normal' ? '正常' : mode === 'shows' ? '演示' : mode === 'leaking' ? '漏水' : '爆管'}模式`)
      if (result.success) {
        fetchSimulatorMode()
      }
    } catch (error) {
      showModal('error', '切换失败', '切换模式失败')
    } finally {
      setLoading(false)
    }
  }

  // 更改模拟时间
  const handleChangeTime = async (time: number) => {
    setLoading(true)
    try {
      await iotApi.changeTime(time)
      setSimTime(time)
      showModal('success', '操作成功', '时间已更改')
    } catch (error) {
      showModal('error', '更改失败', '更改时间失败')
    } finally {
      setLoading(false)
    }
  }

  // 更改模拟季节
  const handleChangeSeason = async (season: number) => {
    setLoading(true)
    try {
      await iotApi.changeSeason(season)
      setSimSeason(season)
      showModal('success', '操作成功', `已切换至${season === 1 ? '春季' : season === 2 ? '夏季' : season === 3 ? '秋季' : '冬季'}`)
    } catch (error) {
      showModal('error', '更改失败', '更改季节失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="app-shell h-screen bg-gray-50 flex overflow-hidden">
      {/* Sidebar */}
      <aside className={`app-sidebar ${sidebarOpen ? 'w-64' : 'w-20'} bg-gradient-to-b from-primary-600 to-primary-800 shadow-xl transition-all duration-300 flex flex-col h-screen`}>
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

        {/* 导航菜单 */}
        <nav className="flex-1 p-2 overflow-y-auto">
          <NavigationMenu collapsed={!sidebarOpen}/>
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
        <header className="app-header bg-gradient-to-r from-primary-600 to-primary-800 shadow-lg">
          <div className="px-6 py-4 flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={() => setSidebarOpen(!sidebarOpen)}
                className="p-2 text-white/80 hover:text-white hover:bg-white/10 rounded-xl transition-all"
              >
                {sidebarOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
              </button>
              <h2 className="text-xl font-bold text-white">数字孪生</h2>
              <div className="flex items-center gap-2 px-3 py-1.5 bg-white/10 backdrop-blur-sm rounded-xl">
                <Cpu className="w-4 h-4 text-white/80" />
                <span className="text-sm text-white/80">虚拟设备管理</span>
              </div>
            </div>
            <button
              onClick={() => { fetchDeviceStatus(); fetchBuildingConfig(); fetchDeviceConfig(); }}
              className="flex items-center gap-2 px-4 py-2 text-white/80 hover:text-white hover:bg-white/10 rounded-xl transition-all"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
              刷新
            </button>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-4 sm:p-6">
          <div className="mx-auto w-full max-w-[1680px]">
          {/* 消息提示 */}
          {message && (
            <div className={`mb-4 p-4 rounded-lg flex items-center gap-3 text-base font-medium border-2 ${
              message.type === 'success' ? 'bg-green-50 text-green-700 border-green-300' : 'bg-red-50 text-red-700 border-red-300'
            }`}>
              {message.type === 'success' ? <CheckCircle className="w-6 h-6" /> : <AlertCircle className="w-6 h-6" />}
              <span className="flex-1">{message.text}</span>
              <button onClick={() => setMessage(null)} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>
          )}

          {/* 弹窗 */}
          {modal && (
            <div className="fixed inset-0 z-50 flex items-center justify-center animate-fade-in">
              <div className="absolute inset-0 bg-gray-900/60 backdrop-blur-sm" onClick={() => setModal(null)} />
              <div className="relative w-full max-w-md mx-4 rounded-3xl bg-white p-6 sm:p-8 shadow-2xl animate-scale-in">
                <div className="text-center">
                  <div className={`mx-auto w-16 h-16 rounded-full flex items-center justify-center mb-4 ${
                    modal.type === 'success' ? 'bg-green-100' : 
                    modal.type === 'error' ? 'bg-red-100' : 'bg-blue-100'
                  }`}>
                    {modal.type === 'success' && <CheckCircle className="w-8 h-8 text-green-600" />}
                    {modal.type === 'error' && <AlertCircle className="w-8 h-8 text-red-600" />}
                    {modal.type === 'info' && <Activity className="w-8 h-8 text-blue-600" />}
                  </div>
                  <h3 className={`text-xl font-bold mb-2 ${
                    modal.type === 'success' ? 'text-green-600' : 
                    modal.type === 'error' ? 'text-red-600' : 'text-blue-600'
                  }`}>
                    {modal.title}
                  </h3>
                  <p className="text-gray-600 mb-6">{modal.message}</p>
                  <button
                    onClick={() => {
                      setModal(null)
                      fetchDeviceStatus()
                    }}
                    className={`px-8 py-2.5 rounded-xl font-medium text-white transition-all ${
                      modal.type === 'success' ? 'bg-green-500 hover:bg-green-600' : 
                      modal.type === 'error' ? 'bg-red-500 hover:bg-red-600' : 'bg-blue-500 hover:bg-blue-600'
                    }`}
                  >
                    确定
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* 确认弹窗 */}
          {confirmModal && (
            <div className="fixed inset-0 z-50 flex items-center justify-center animate-fade-in">
              <div className="absolute inset-0 bg-gray-900/60 backdrop-blur-sm" onClick={() => setConfirmModal(null)} />
              <div className="relative w-full max-w-md mx-4 rounded-3xl bg-white p-6 sm:p-8 shadow-2xl animate-scale-in">
                <div className="text-center">
                  <div className="mx-auto w-16 h-16 rounded-full flex items-center justify-center mb-4 bg-amber-100">
                    <AlertTriangle className="w-8 h-8 text-amber-600" />
                  </div>
                  <h3 className="text-xl font-bold text-gray-900 mb-2">
                    {confirmModal.title}
                  </h3>
                  <p className="text-gray-600 mb-6">{confirmModal.message}</p>
                  <div className="flex gap-3">
                    <button
                      onClick={() => setConfirmModal(null)}
                      className="flex-1 px-6 py-2.5 rounded-xl font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 transition-colors"
                    >
                      取消
                    </button>
                    <button
                      onClick={confirmModal.onConfirm}
                      className="flex-1 px-6 py-2.5 rounded-xl font-medium text-white bg-amber-500 hover:bg-amber-600 transition-colors"
                    >
                      确认
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* 状态概览 */}
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-5 gap-4 mb-6">
            <div className="glass-card rounded-2xl p-5">
              <div className="flex items-center gap-3 mb-2">
                <div className={`p-3 rounded-xl ${isInitialized ? 'bg-gradient-to-br from-green-400 to-green-600' : 'bg-gray-100'}`}>
                  <Cpu className={`w-5 h-5 ${isInitialized ? 'text-white' : 'text-gray-400'}`} />
                </div>
              </div>
              <p className="text-sm text-gray-500">设备状态</p>
              <p className={`text-xl font-bold ${isInitialized ? 'text-green-600' : 'text-gray-400'}`}>
                {isInitialized ? '已初始化' : '未初始化'}
              </p>
            </div>

            <div className="glass-card rounded-2xl p-5">
              <div className="flex items-center gap-3 mb-2">
                <div className="p-3 rounded-xl bg-gradient-to-br from-blue-400 to-blue-600">
                  <Droplets className="w-5 h-5 text-white" />
                </div>
              </div>
              <p className="text-sm text-gray-500">水表数量</p>
              <p className="text-xl font-bold text-gray-900">{deviceCount}</p>
            </div>

            <div className="glass-card rounded-2xl p-5">
              <div className="flex items-center gap-3 mb-2">
                <div className={`p-3 rounded-xl ${isMetersRunning ? 'bg-gradient-to-br from-green-400 to-green-600' : 'bg-gray-100'}`}>
                  <Play className={`w-5 h-5 ${isMetersRunning ? 'text-white' : 'text-gray-400'}`} />
                </div>
              </div>
              <p className="text-sm text-gray-500">水表运行</p>
              <p className={`text-xl font-bold ${isMetersRunning ? 'text-green-600' : 'text-gray-400'}`}>
                {isMetersRunning ? '运行中' : '已停止'}
              </p>
            </div>

            <div className="glass-card rounded-2xl p-5">
              <div className="flex items-center gap-3 mb-2">
                <div className="p-3 rounded-xl bg-gradient-to-br from-purple-400 to-purple-600">
                  <Activity className="w-5 h-5 text-white" />
                </div>
              </div>
              <p className="text-sm text-gray-500">传感器数量</p>
              <p className="text-xl font-bold text-gray-900">{(buildingConfig.educationBuildings + buildingConfig.experimentBuildings + buildingConfig.dormitoryBuildings) * buildingConfig.floors}</p>
            </div>

            <div className="glass-card rounded-2xl p-5">
              <div className="flex items-center gap-3 mb-2">
                <div className={`p-3 rounded-xl ${isSensorsRunning ? 'bg-gradient-to-br from-green-400 to-green-600' : 'bg-gray-100'}`}>
                  <Play className={`w-5 h-5 ${isSensorsRunning ? 'text-white' : 'text-gray-400'}`} />
                </div>
              </div>
              <p className="text-sm text-gray-500">传感器运行</p>
              <p className={`text-xl font-bold ${isSensorsRunning ? 'text-green-600' : 'text-gray-400'}`}>
                {isSensorsRunning ? '运行中' : '已停止'}
              </p>
            </div>
          </div>

          {/* 楼宇配置 */}
          <div className="bg-white rounded-3xl p-5 sm:p-6 shadow-lg shadow-gray-100/50 border border-gray-100 mb-6">
            <div className="mb-5 flex flex-wrap items-end justify-between gap-2">
              <div>
                <h3 className="text-lg font-semibold text-gray-900">楼宇配置</h3>
                <p className="mt-1 text-sm text-gray-500">初始化前可调整设备规模；初始化后锁定，防止设备拓扑不一致。</p>
              </div>
              <span className={`rounded-full px-3 py-1 text-xs font-medium ${isInitialized ? 'bg-slate-100 text-slate-600' : 'bg-emerald-50 text-emerald-700'}`}>
                {isInitialized ? '已锁定' : '可编辑'}
              </span>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-5 gap-4">
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

          {/* IoT-device 运行参数：只读展示 Nacos 当前生效值 */}
          <div className="bg-white rounded-3xl p-5 sm:p-6 shadow-lg shadow-gray-100/50 border border-gray-100 mb-6">
            <div className="flex flex-wrap items-start justify-between gap-3 mb-5">
              <div className="flex items-center gap-3">
                <div className="p-3 bg-gradient-to-br from-slate-500 to-slate-700 rounded-xl">
                  <Settings2 className="w-5 h-5 text-white" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900">IoT-device 运行参数</h3>
                  <p className="text-xs text-gray-500 mt-1">只读展示当前实例已加载的生效值，不暴露 Nacos 凭据</p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <span className="inline-flex items-center gap-1.5 rounded-full bg-sky-50 px-2.5 py-1 text-xs font-medium text-sky-700">
                  <Cloud className="w-3.5 h-3.5" /> Nacos 远端
                </span>
                <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600">只读</span>
                <button
                  type="button"
                  onClick={fetchDeviceConfig}
                  disabled={deviceConfigLoading}
                  className="inline-flex items-center gap-1.5 rounded-lg border border-gray-200 px-3 py-1.5 text-xs text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${deviceConfigLoading ? 'animate-spin' : ''}`} />
                  刷新配置
                </button>
              </div>
            </div>

            {deviceConfigError ? (
              <div className="flex items-center gap-2 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{deviceConfigError}</span>
              </div>
            ) : deviceConfig ? (
              <>
                <div className="grid grid-cols-1 xl:grid-cols-3 gap-4">
                  <div className="rounded-2xl border border-slate-100 bg-slate-50/50 p-4">
                    <h4 className="mb-3 text-sm font-semibold text-gray-700">服务与上报</h4>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      <ConfigMetric label="HTTP 服务端口" value={deviceConfig.port} />
                      <ConfigMetric label="离线判定阈值" value={deviceConfig.n} unit="个周期" />
                      <ConfigMetric label="水表上报频率" value={formatConfigNumber(deviceConfig.meterReportFrequency)} unit="ms" />
                      <ConfigMetric label="水表时间偏移" value={formatConfigNumber(deviceConfig.meterTimeOffset)} unit="ms" />
                      <ConfigMetric label="水质上报频率" value={formatConfigNumber(deviceConfig.waterQualityReportFrequency)} unit="ms" />
                      <ConfigMetric label="水质时间偏移" value={formatConfigNumber(deviceConfig.waterQualityReportTimeOffset)} unit="ms" />
                    </div>
                  </div>

                  <div className="rounded-2xl border border-slate-100 bg-slate-50/50 p-4">
                    <h4 className="mb-3 text-sm font-semibold text-gray-700">虚拟管网压力模型</h4>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      <ConfigMetric label="初始压力 p0" value={formatConfigNumber(deviceConfig.p0)} />
                      <ConfigMetric label="离散步长 step" value={formatConfigNumber(deviceConfig.step)} />
                      <ConfigMetric label="最小压力 pmin" value={formatConfigNumber(deviceConfig.pmin)} />
                      <ConfigMetric label="最大压力 pmax" value={formatConfigNumber(deviceConfig.pmax)} />
                      <ConfigMetric label="不可信数据概率" value={formatConfigNumber(deviceConfig.pnotCredible)} />
                    </div>
                  </div>

                  <div className="rounded-2xl border border-slate-100 bg-slate-50/50 p-4">
                    <h4 className="mb-3 text-sm font-semibold text-gray-700">行为参数</h4>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      <ConfigMetric
                        label="早八宿舍起床比例"
                        value={`${formatConfigNumber(deviceConfig.wakeUpDormRate * 100, 2)}%`}
                      />
                    </div>
                  </div>
                </div>
                <p className="mt-4 rounded-xl bg-sky-50 px-3 py-2 text-xs text-sky-700">
                  配置来源：Nacos 的 IoT-device.yml。请在 Nacos 控制台修改参数，待动态刷新后点击“刷新配置”查看实例生效值。
                </p>
              </>
            ) : (
              <div className="rounded-xl border border-gray-100 bg-gray-50 px-4 py-5 text-sm text-gray-500">
                {deviceConfigLoading ? '正在读取 Nacos 配置…' : '尚未读取到 IoT-device 配置。'}
              </div>
            )}
          </div>

          {/* 公共控制：初始化/重置 */}
          <div className="bg-white rounded-3xl p-5 sm:p-6 shadow-lg shadow-gray-100/50 border border-gray-100 mb-6">
            <div className="mb-5 flex flex-wrap items-end justify-between gap-2">
              <div>
                <h3 className="text-lg font-semibold text-gray-900">设备初始化与重置</h3>
                <p className="mt-1 text-sm text-gray-500">首次使用先创建设备；重置会清空设备和运行数据。</p>
              </div>
              <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">高风险操作</span>
            </div>
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              <button
                onClick={handleInit}
                disabled={loading || isInitialized}
                className="flex min-h-44 flex-col items-center justify-center gap-2 p-5 bg-gradient-to-br from-blue-50 to-blue-100 hover:from-blue-100 hover:to-blue-200 rounded-2xl transition-all hover:-translate-y-0.5 disabled:opacity-50 disabled:cursor-not-allowed border border-blue-200"
              >
                <div className="p-3 bg-gradient-to-br from-blue-400 to-blue-600 rounded-xl shadow-lg shadow-blue-500/30">
                  <Cpu className="w-8 h-8 text-white" />
                </div>
                <span className="font-semibold text-blue-700">初始化设备</span>
                <span className="text-xs text-blue-500">创建虚拟设备</span>
              </button>

              <button
                onClick={handleReset}
                disabled={loading || !isInitialized || isAnyTaskRunning}
                className="flex min-h-44 flex-col items-center justify-center gap-2 p-5 bg-gradient-to-br from-orange-50 to-orange-100 hover:from-orange-100 hover:to-orange-200 rounded-2xl transition-all hover:-translate-y-0.5 disabled:opacity-50 disabled:cursor-not-allowed border border-orange-200"
              >
                <RotateCcw className="w-8 h-8 text-orange-600" />
                <span className="font-medium text-orange-700">重置设备</span>
                <span className="text-xs text-orange-500">{isAnyTaskRunning ? '请先停止设备' : '清除所有数据'}</span>
              </button>
            </div>
          </div>

          {/* 水表控制卡片 - 可展开 */}
          <div className="bg-white rounded-3xl shadow-lg shadow-gray-100/50 border border-gray-100 mb-4 overflow-hidden">
            <div 
              className="flex flex-col gap-3 p-4 cursor-pointer hover:bg-gray-50/50 transition-colors sm:flex-row sm:items-center sm:p-5"
              onClick={() => setMeterExpanded(!meterExpanded)}
            >
              <div className="p-3 bg-gradient-to-br from-blue-400 to-blue-600 rounded-xl">
                <Droplets className="w-5 h-5 text-white" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900">水表控制</h3>
              <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                isMetersRunning ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
              }`}>
                {isMetersRunning ? '运行中' : '已停止'}
              </span>
              <div className="ml-auto flex w-full flex-wrap items-center justify-end gap-2 sm:w-auto">
                <button
                  onClick={(e) => {
                    e.stopPropagation()
                    if (!isInitialized) { showMessage('error', '请先初始化设备'); return; }
                    iotApi.startAllMeters().then(result => {
                      showResultMessage(result, '水表已开启')
                      if (result.success) fetchDeviceStatus()
                    })
                  }}
                  disabled={loading || !isInitialized || isMetersRunning}
                  className="px-3 py-1.5 text-xs bg-gradient-to-r from-green-500 to-green-600 text-white rounded-lg hover:from-green-600 hover:to-green-700 disabled:opacity-50 disabled:cursor-not-allowed shadow-sm"
                >
                  开启
                </button>
                <button
                  onClick={(e) => {
                    e.stopPropagation()
                    handleStopMeters()
                  }}
                  disabled={!isMetersRunning}
                  className="px-3 py-1.5 text-xs bg-gradient-to-r from-yellow-500 to-yellow-600 text-white rounded-lg hover:from-yellow-600 hover:to-yellow-700 disabled:opacity-50 disabled:cursor-not-allowed shadow-sm"
                >
                  停止
                </button>
                <button
                  onClick={(e) => {
                    e.stopPropagation()
                    if (!isInitialized) { showMessage('error', '请先初始化设备'); return; }
                    if (!confirm('确认水表全部下线？')) return
                    iotApi.offlineMeters([]).then(result => {
                      showResultMessage(result, '水表已全部下线')
                      if (result.success) fetchDeviceStatus()
                    })
                  }}
                  disabled={loading || !isInitialized}
                  className="px-3 py-1.5 text-xs bg-gradient-to-r from-orange-500 to-orange-600 text-white rounded-lg hover:from-orange-600 hover:to-orange-700 disabled:opacity-50 disabled:cursor-not-allowed shadow-sm"
                >
                  下线
                </button>
                <ChevronDown className={`w-5 h-5 text-gray-400 transition-transform ${meterExpanded ? 'rotate-180' : ''}`} />
              </div>
            </div>
            
            {/* 展开内容 */}
            {meterExpanded && (
              <div className="px-4 pb-4 border-t border-gray-100">
                <div className="pt-3">
                  <p className="text-sm font-medium text-gray-700 mb-2">批量操作</p>
                  <div className="grid grid-cols-4 gap-2 mb-3">
                    <select
                      value={meterOfflineSelect.campus}
                      onChange={(e) => setMeterOfflineSelect({ ...meterOfflineSelect, campus: parseInt(e.target.value), building: 1, floor: 1, unit: 1 })}
                      className="px-2 py-1.5 border border-gray-300 rounded-lg text-sm"
                    >
                      {CAMPUS_OPTIONS.map(c => (<option key={c.value} value={c.value}>{c.label}</option>))}
                    </select>
                    <select
                      value={meterOfflineSelect.building}
                      onChange={(e) => setMeterOfflineSelect({ ...meterOfflineSelect, building: parseInt(e.target.value), floor: 1, unit: 1 })}
                      className="px-2 py-1.5 border border-gray-300 rounded-lg text-sm"
                    >
                      {Array.from({ length: buildingConfig.educationBuildings + buildingConfig.experimentBuildings + buildingConfig.dormitoryBuildings }, (_, i) => i + 1).map(b => (<option key={b} value={b}>{b}号楼</option>))}
                    </select>
                    <select
                      value={meterOfflineSelect.floor}
                      onChange={(e) => setMeterOfflineSelect({ ...meterOfflineSelect, floor: parseInt(e.target.value), unit: 1 })}
                      className="px-2 py-1.5 border border-gray-300 rounded-lg text-sm"
                    >
                      {Array.from({ length: buildingConfig.floors }, (_, i) => i + 1).map(f => (<option key={f} value={f}>{f}层</option>))}
                    </select>
                    <select
                      value={meterOfflineSelect.unit}
                      onChange={(e) => setMeterOfflineSelect({ ...meterOfflineSelect, unit: parseInt(e.target.value) })}
                      className="px-2 py-1.5 border border-gray-300 rounded-lg text-sm"
                    >
                      {Array.from({ length: buildingConfig.rooms }, (_, i) => i + 1).map(u => (<option key={u} value={u}>{u}室</option>))}
                    </select>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => {
                        if (!isInitialized) { showMessage('error', '请先初始化设备'); return; }
                        const deviceId = generateDeviceId(meterOfflineSelect.campus, meterOfflineSelect.building, meterOfflineSelect.floor, meterOfflineSelect.unit)
                        const locationLabel = `${CAMPUS_OPTIONS.find(c => c.value === meterOfflineSelect.campus)?.label} ${meterOfflineSelect.building}号楼 ${meterOfflineSelect.floor}层 ${meterOfflineSelect.unit}室`
                        iotApi.startMeters([deviceId]).then(result => { showResultMessage(result, `水表 ${locationLabel} 已开启`); if (result.success) fetchDeviceStatus() })
                      }}
                      disabled={loading || !isInitialized}
                      className="flex-1 px-3 py-1.5 text-sm bg-green-50 text-green-700 rounded-lg hover:bg-green-100 disabled:opacity-50"
                    >
                      开启
                    </button>
                    <button
                      onClick={() => {
                        const deviceId = generateDeviceId(meterOfflineSelect.campus, meterOfflineSelect.building, meterOfflineSelect.floor, meterOfflineSelect.unit)
                        const locationLabel = `${CAMPUS_OPTIONS.find(c => c.value === meterOfflineSelect.campus)?.label} ${meterOfflineSelect.building}号楼 ${meterOfflineSelect.floor}层 ${meterOfflineSelect.unit}室`
                        iotApi.stopMeters([deviceId]).then(result => { showResultMessage(result, `水表 ${locationLabel} 已停止`); if (result.success) fetchDeviceStatus() })
                      }}
                      disabled={loading}
                      className="flex-1 px-3 py-1.5 text-sm bg-yellow-50 text-yellow-700 rounded-lg hover:bg-yellow-100 disabled:opacity-50"
                    >
                      停止
                    </button>
                    <button
                      onClick={handleOfflineMeters}
                      disabled={loading || !isInitialized}
                      className="flex-1 px-3 py-1.5 text-sm bg-gray-50 text-gray-700 rounded-lg hover:bg-gray-100 disabled:opacity-50"
                    >
                      下线
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* 传感器控制卡片 - 可展开 */}
          <div className="bg-white rounded-3xl shadow-lg shadow-gray-100/50 border border-gray-100 mb-4 overflow-hidden">
            <div 
              className="flex flex-col gap-3 p-4 cursor-pointer hover:bg-gray-50/50 transition-colors sm:flex-row sm:items-center sm:p-5"
              onClick={() => setSensorExpanded(!sensorExpanded)}
            >
              <div className="p-3 bg-gradient-to-br from-purple-400 to-purple-600 rounded-xl">
                <Activity className="w-5 h-5 text-white" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900">传感器控制</h3>
              <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                isSensorsRunning ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
              }`}>
                {isSensorsRunning ? '运行中' : '已停止'}
              </span>
              <div className="ml-auto flex w-full flex-wrap items-center justify-end gap-2 sm:w-auto">
                <button
                  onClick={(e) => {
                    e.stopPropagation()
                    if (!isInitialized) { showMessage('error', '请先初始化设备'); return; }
                    iotApi.startAllSensors().then(result => {
                      showResultMessage(result, '传感器已开启')
                      if (result.success) fetchDeviceStatus()
                    })
                  }}
                  disabled={loading || !isInitialized || isSensorsRunning}
                  className="px-3 py-1.5 text-xs bg-gradient-to-r from-green-500 to-green-600 text-white rounded-lg hover:from-green-600 hover:to-green-700 disabled:opacity-50 disabled:cursor-not-allowed shadow-sm"
                >
                  开启
                </button>
                <button
                  onClick={(e) => {
                    e.stopPropagation()
                    handleStopSensors()
                  }}
                  disabled={!isSensorsRunning}
                  className="px-3 py-1.5 text-xs bg-gradient-to-r from-yellow-500 to-yellow-600 text-white rounded-lg hover:from-yellow-600 hover:to-yellow-700 disabled:opacity-50 disabled:cursor-not-allowed shadow-sm"
                >
                  停止
                </button>
                <button
                  onClick={(e) => {
                    e.stopPropagation()
                    if (!isInitialized) { showMessage('error', '请先初始化设备'); return; }
                    if (!confirm('确认传感器全部下线？')) return
                    iotApi.offlineSensors([]).then(result => {
                      showResultMessage(result, '传感器已全部下线')
                      if (result.success) fetchDeviceStatus()
                    })
                  }}
                  disabled={loading || !isInitialized}
                  className="px-3 py-1.5 text-xs bg-gradient-to-r from-orange-500 to-orange-600 text-white rounded-lg hover:from-orange-600 hover:to-orange-700 disabled:opacity-50 disabled:cursor-not-allowed shadow-sm"
                >
                  下线
                </button>
                <ChevronDown className={`w-5 h-5 text-gray-400 transition-transform ${sensorExpanded ? 'rotate-180' : ''}`} />
              </div>
            </div>
            
            {/* 展开内容 */}
            {sensorExpanded && (
              <div className="px-4 pb-4 border-t border-gray-100">
                <div className="pt-3">
                  <p className="text-sm font-medium text-gray-700 mb-2">批量操作</p>
                  <div className="grid grid-cols-3 gap-2 mb-3">
                    <select
                      value={sensorOfflineSelect.campus}
                      onChange={(e) => setSensorOfflineSelect({ ...sensorOfflineSelect, campus: parseInt(e.target.value), building: 1, floor: 1 })}
                      className="px-2 py-1.5 border border-gray-300 rounded-lg text-sm"
                    >
                      {CAMPUS_OPTIONS.map(c => (<option key={c.value} value={c.value}>{c.label}</option>))}
                    </select>
                    <select
                      value={sensorOfflineSelect.building}
                      onChange={(e) => setSensorOfflineSelect({ ...sensorOfflineSelect, building: parseInt(e.target.value), floor: 1 })}
                      className="px-2 py-1.5 border border-gray-300 rounded-lg text-sm"
                    >
                      {Array.from({ length: buildingConfig.educationBuildings + buildingConfig.experimentBuildings + buildingConfig.dormitoryBuildings }, (_, i) => i + 1).map(b => (<option key={b} value={b}>{b}号楼</option>))}
                    </select>
                    <select
                      value={sensorOfflineSelect.floor}
                      onChange={(e) => setSensorOfflineSelect({ ...sensorOfflineSelect, floor: parseInt(e.target.value) })}
                      className="px-2 py-1.5 border border-gray-300 rounded-lg text-sm"
                    >
                      {Array.from({ length: buildingConfig.floors }, (_, i) => i + 1).map(f => (<option key={f} value={f}>{f}层</option>))}
                    </select>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => {
                        if (!isInitialized) { showMessage('error', '请先初始化设备'); return; }
                        const deviceId = generateWaterQualitySensorId(sensorOfflineSelect.campus, sensorOfflineSelect.building, sensorOfflineSelect.floor)
                        const locationLabel = `${CAMPUS_OPTIONS.find(c => c.value === sensorOfflineSelect.campus)?.label} ${sensorOfflineSelect.building}号楼 ${sensorOfflineSelect.floor}层`
                        iotApi.startSensors([deviceId]).then(result => { showResultMessage(result, `传感器 ${locationLabel} 已开启`); if (result.success) fetchDeviceStatus() })
                      }}
                      disabled={loading || !isInitialized}
                      className="flex-1 px-3 py-1.5 text-sm bg-green-50 text-green-700 rounded-lg hover:bg-green-100 disabled:opacity-50"
                    >
                      开启
                    </button>
                    <button
                      onClick={() => {
                        const deviceId = generateWaterQualitySensorId(sensorOfflineSelect.campus, sensorOfflineSelect.building, sensorOfflineSelect.floor)
                        const locationLabel = `${CAMPUS_OPTIONS.find(c => c.value === sensorOfflineSelect.campus)?.label} ${sensorOfflineSelect.building}号楼 ${sensorOfflineSelect.floor}层`
                        iotApi.stopSensors([deviceId]).then(result => { showResultMessage(result, `传感器 ${locationLabel} 已停止`); if (result.success) fetchDeviceStatus() })
                      }}
                      disabled={loading}
                      className="flex-1 px-3 py-1.5 text-sm bg-yellow-50 text-yellow-700 rounded-lg hover:bg-yellow-100 disabled:opacity-50"
                    >
                      停止
                    </button>
                    <button
                      onClick={handleOfflineSensors}
                      disabled={loading || !isInitialized}
                      className="flex-1 px-3 py-1.5 text-sm bg-gray-50 text-gray-700 rounded-lg hover:bg-gray-100 disabled:opacity-50"
                    >
                      下线
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* 阀门控制 */}
          <div className="bg-white rounded-3xl p-5 sm:p-6 shadow-lg shadow-gray-100/50 border border-gray-100 mb-6">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <div className="flex items-center gap-3">
                <div className={`flex h-11 w-11 items-center justify-center rounded-xl ${isValvesOpen ? 'bg-orange-100 text-orange-600' : 'bg-emerald-100 text-emerald-600'}`}>
                  {isValvesOpen ? <PowerOff className="w-5 h-5" /> : <Power className="w-5 h-5" />}
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900">阀门控制</h3>
                  <p className="mt-1 text-sm text-gray-500">当前阀门：{isValvesOpen ? '开启，可正常供水' : '关闭，供水已暂停'}</p>
                </div>
              </div>
            <button
              onClick={handleToggleValves}
              disabled={loading || !isInitialized}
              className={`flex items-center gap-3 px-6 py-3 rounded-xl transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-sm ${
                isValvesOpen 
                  ? 'bg-gradient-to-r from-orange-400 to-orange-500 hover:from-orange-500 hover:to-orange-600 text-white' 
                  : 'bg-gradient-to-r from-green-400 to-green-500 hover:from-green-500 hover:to-green-600 text-white'
              }`}
            >
              {isValvesOpen ? <PowerOff className="w-5 h-5" /> : <Power className="w-5 h-5" />}
              <span className="font-medium">{isValvesOpen ? '关闭所有阀门' : '开启所有阀门'}</span>
            </button>
            </div>
          </div>

          {/* 模拟模式 */}
          <div className="bg-white rounded-2xl p-6 shadow-lg shadow-gray-100/50 border border-gray-100">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900">模拟模式</h3>
              <span className="text-sm text-gray-500">
                当前: {simMode === 'normal' ? '正常' : simMode === 'leaking' ? '漏水检测' : simMode === 'burstPipe' ? '爆管模拟' : '演示模式'}
              </span>
            </div>
            
            {/* 模式选择 */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
              <button
                onClick={() => handleChangeMode('normal')}
                disabled={loading}
                className={`px-4 py-2.5 rounded-lg font-medium transition-colors ${
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
                className={`px-4 py-2.5 rounded-lg font-medium transition-colors ${
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
                className={`px-4 py-2.5 rounded-lg font-medium transition-colors ${
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
                className={`px-4 py-2.5 rounded-lg font-medium transition-colors ${
                  simMode === 'burstPipe'
                    ? 'bg-orange-500 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                爆管模式
              </button>
            </div>
            
            {/* 时间和季节 */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-4 border-t border-gray-100">
              {/* 模拟时间 - 可拖动进度条 */}
              <div>
                <h4 className="text-sm font-medium text-gray-700 mb-2">
                  模拟时间: <span className="text-blue-600 font-bold">{Math.floor(simTime / 3600).toString().padStart(2, '0')}:{Math.floor((simTime % 3600) / 60).toString().padStart(2, '0')}</span>
                </h4>
                <div className="flex items-center gap-3">
                  <span className="text-xs text-gray-400 w-10 text-right">00:00</span>
                  <div className="flex-1 relative h-6 flex items-center">
                    {/* 背景轨道 */}
                    <div className="absolute left-0 right-0 h-2 bg-blue-100 rounded-full"></div>
                    {/* 进度填充 */}
                    <div 
                      className="absolute left-0 h-2 bg-blue-400 rounded-full transition-all duration-150"
                      style={{ width: `${(simTime / 86400) * 100}%` }}
                    ></div>
                    {/* 滑块 */}
                    <input
                      type="range"
                      min={0}
                      max={86399}
                      value={simTime}
                      onChange={(e) => {
                        const newTime = parseInt(e.target.value)
                        setSimTime(newTime)
                      }}
                      onMouseUp={(e) => {
                        const newTime = parseInt((e.target as HTMLInputElement).value)
                        handleChangeTime(newTime)
                      }}
                      onTouchEnd={(e) => {
                        const newTime = parseInt((e.target as HTMLInputElement).value)
                        handleChangeTime(newTime)
                      }}
                      disabled={loading}
                      className="relative w-full h-6 bg-transparent cursor-pointer appearance-none z-10 disabled:opacity-50 disabled:cursor-not-allowed"
                    />
                    <style>{`
                      input[type="range"]::-webkit-slider-thumb {
                        -webkit-appearance: none;
                        width: 16px;
                        height: 16px;
                        background: white;
                        border: 2px solid #3b82f6;
                        border-radius: 50%;
                        cursor: pointer;
                        box-shadow: 0 1px 3px rgba(0,0,0,0.1);
                      }
                      input[type="range"]::-moz-range-thumb {
                        width: 16px;
                        height: 16px;
                        background: white;
                        border: 2px solid #3b82f6;
                        border-radius: 50%;
                        cursor: pointer;
                      }
                    `}</style>
                  </div>
                  <span className="text-xs text-gray-400 w-10">24:00</span>
                </div>
                {/* 快速选择 */}
                <div className="flex gap-1 mt-3">
                  {[
                    { h: 0, label: '凌晨' },
                    { h: 6, label: '早上' },
                    { h: 12, label: '中午' },
                    { h: 18, label: '傍晚' },
                    { h: 24, label: '深夜' }
                  ].map((t) => (
                    <button
                      key={t.h}
                      onClick={() => handleChangeTime(t.h * 3600)}
                      disabled={loading}
                      className={`flex-1 px-2 py-1.5 text-xs rounded-lg transition-colors ${
                        (t.h === 24 && Math.floor(simTime/3600) === 24) || (t.h < 24 && Math.floor(simTime/3600) === t.h)
                          ? 'bg-blue-500 text-white'
                          : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      }`}
                    >
                      {t.label}
                    </button>
                  ))}
                </div>
              </div>
              
              {/* 模拟季节 */}
              <div>
                <h4 className="text-sm font-medium text-gray-700 mb-2">模拟季节</h4>
                <div className="flex gap-2 mt-7">
                  <button
                    onClick={() => handleChangeSeason(1)}
                    disabled={loading}
                    className={`flex-1 px-4 py-3 text-base rounded-lg transition-colors ${
                      simSeason === 1
                        ? 'bg-green-500 text-white'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    }`}
                  >
                    春
                  </button>
                  <button
                    onClick={() => handleChangeSeason(2)}
                    disabled={loading}
                    className={`flex-1 px-4 py-3 text-base rounded-lg transition-colors ${
                      simSeason === 2
                        ? 'bg-red-500 text-white'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    }`}
                  >
                    夏
                  </button>
                  <button
                    onClick={() => handleChangeSeason(3)}
                    disabled={loading}
                    className={`flex-1 px-4 py-3 text-base rounded-lg transition-colors ${
                      simSeason === 3
                        ? 'bg-orange-500 text-white'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    }`}
                  >
                    秋
                  </button>
                  <button
                    onClick={() => handleChangeSeason(4)}
                    disabled={loading}
                    className={`flex-1 px-4 py-3 text-base rounded-lg transition-colors ${
                      simSeason === 4
                        ? 'bg-blue-500 text-white'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    }`}
                  >
                    冬
                  </button>
                </div>
              </div>
            </div>
          </div>
          </div>
        </main>
      </div>
    </div>
  )
}
