import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { iotApi, generateDeviceId, generateWaterQualitySensorId, getBuildingConfig } from '@/api/iot'

import { Droplets, User, Menu, X, FileText, Download, RefreshCw, ChevronDown } from 'lucide-react'
import NavigationMenu from '@/components/NavigationMenu'

export default function Reports() {
  const { nickname } = useAuthStore()
  const navigate = useNavigate()
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [selectedDevice, setSelectedDevice] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')
  const [devices, setDevices] = useState<string[]>([])

  useEffect(() => {
    const fetchConfig = async () => {
      try {
        const config = await getBuildingConfig()
        // 根据配置生成所有设备编码
        const allDevices: string[] = []
        
        // 教学楼 (educationStart ~ educationStart+2)
        const educationBuildings = config.educationStart + 2
        for (let b = config.educationStart; b <= educationBuildings && b <= config.totalBuildings; b++) {
          for (let floor = 1; floor <= config.floors; floor++) {
            for (let room = 1; room <= config.rooms; room++) {
              allDevices.push(generateDeviceId(1, b, floor, room))
            }
            // 水质传感器
            allDevices.push(generateWaterQualitySensorId(1, b, floor))
          }
        }
        
        // 实验楼 (experimentStart ~ experimentStart+1)
        const experimentBuildings = config.experimentStart + 1
        for (let b = config.experimentStart; b <= experimentBuildings && b <= config.totalBuildings; b++) {
          for (let floor = 1; floor <= config.floors; floor++) {
            for (let room = 1; room <= config.rooms; room++) {
              allDevices.push(generateDeviceId(2, b, floor, room))
            }
            allDevices.push(generateWaterQualitySensorId(2, b, floor))
          }
        }
        
        // 宿舍楼 (dormitoryStart ~ totalBuildings)
        for (let b = config.dormitoryStart; b <= config.totalBuildings; b++) {
          for (let floor = 1; floor <= config.floors; floor++) {
            for (let room = 1; room <= config.rooms; room++) {
              allDevices.push(generateDeviceId(3, b, floor, room))
            }
            allDevices.push(generateWaterQualitySensorId(3, b, floor))
          }
        }
        
        setDevices(allDevices)
      } catch (error) {
        console.error('获取楼宇配置失败:', error)
      }
    }
    fetchConfig()
  }, [])

  const handleExport = async () => {
    if (!selectedDevice) {
      setMessage('请选择设备')
      return
    }

    setLoading(true)
    setMessage('')

    try {
      const blob = await iotApi.getDeviceData(selectedDevice)
      
      // 创建下载链接
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `device_${selectedDevice}_${new Date().toISOString().split('T')[0]}.xlsx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      
      setMessage('导出成功')
    } catch (error: any) {
      console.error('导出失败:', error)
      setMessage(error.message || '导出失败，请选择其他设备')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="app-shell h-screen flex overflow-hidden">
      <aside className={`app-sidebar ${sidebarOpen ? 'w-64' : 'w-20'} transition-all duration-300 flex flex-col h-screen`}>
        <div className="p-4 border-b border-white/10 flex-shrink-0">
          <div className="flex items-center gap-3">
            <div className="app-brand-mark flex items-center justify-center w-10 h-10 rounded-xl flex-shrink-0">
              <Droplets className="w-5 h-5 text-white" />
            </div>
            {sidebarOpen && <h1 className="text-lg font-semibold text-white">水务平台</h1>}
          </div>
        </div>
        <nav className="flex-1 p-3 overflow-y-auto">
          <p className="app-sidebar-label px-3 mb-2 text-xs font-medium uppercase">菜单</p>
          <NavigationMenu collapsed={!sidebarOpen} />
        </nav>
        <div className="p-3 border-t border-white/10 flex-shrink-0">
          <div className={`flex items-center gap-3 px-3 py-2.5 rounded-xl ${sidebarOpen ? '' : 'justify-center'}`}>
            <div className="app-avatar w-9 h-9 rounded-full flex items-center justify-center">
              <User className="w-4 h-4 text-white" />
            </div>
            {sidebarOpen && <span className="text-sm font-medium text-white truncate">{nickname || '用户'}</span>}
          </div>
        </div>
      </aside>

      <div className="flex-1 flex flex-col overflow-hidden">
        <header className="app-header app-header-light flex-shrink-0">
          <div className="px-6 py-4 flex items-center gap-4">
            <button type="button" onClick={() => setSidebarOpen(!sidebarOpen)} className="app-header-action p-2 rounded-lg" aria-label="切换侧边栏">
              {sidebarOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
            <div>
              <p className="app-eyebrow">DATA CENTER</p>
              <h2 className="text-xl font-semibold text-slate-900">数据报表</h2>
            </div>
            <button type="button" onClick={() => navigate('/dashboard')} className="app-header-back ml-auto text-sm">
              返回仪表盘
            </button>
          </div>
        </header>

        <main className="app-page flex-1 overflow-y-auto p-6">
          <div className="max-w-3xl mx-auto">
            <div className="app-card rounded-2xl p-6">
          <div className="flex items-center gap-3 mb-6">
            <div className="app-icon app-icon-blue"><FileText className="w-5 h-5" /></div>
            <h1 className="text-2xl font-bold text-gray-900">数据报表</h1>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                选择设备
              </label>
              <div className="relative">
                <select
                  value={selectedDevice}
                  onChange={(e) => setSelectedDevice(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 appearance-none bg-white"
                >
                  <option value="">请选择设备</option>
                  {devices.map((code) => (
                    <option key={code} value={code}>
                      {code}
                    </option>
                  ))}
                </select>
                <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 pointer-events-none" />
              </div>
              <p className="mt-1 text-sm text-gray-500">
                共 {devices.length} 个设备可选
              </p>
            </div>

            <button
              onClick={handleExport}
              disabled={loading || !selectedDevice}
              className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? (
                <>
                  <RefreshCw className="w-5 h-5 animate-spin" />
                  导出中...
                </>
              ) : (
                <>
                  <Download className="w-5 h-5" />
                  导出数据
                </>
              )}
            </button>

            {message && (
              <p className={`text-center text-sm ${message.includes('成功') ? 'text-green-600' : 'text-red-600'}`}>
                {message}
              </p>
            )}
          </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  )
}
