import { useState, useEffect } from 'react'
import { useAuthStore } from '@/store/authStore'
import { iotApi, generateDeviceId, generateWaterQualitySensorId, getBuildingConfig } from '@/api/iot'

interface BuildingConfig {
  educationStart: number
  experimentStart: number
  dormitoryStart: number
  totalBuildings: number
  floors: number
  rooms: number
}
import { FileText, Download, RefreshCw, ChevronDown } from 'lucide-react'

export default function Reports() {
  const { uid } = useAuthStore()
  const [buildingConfig, setBuildingConfig] = useState<BuildingConfig | null>(null)
  const [selectedDevice, setSelectedDevice] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')
  const [devices, setDevices] = useState<string[]>([])

  useEffect(() => {
    const fetchConfig = async () => {
      try {
        const config = await getBuildingConfig()
        setBuildingConfig(config)
        
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
    <div className="p-6">
      <div className="max-w-2xl mx-auto">
        <div className="bg-white rounded-xl shadow-sm p-6">
          <div className="flex items-center gap-3 mb-6">
            <FileText className="w-6 h-6 text-primary-600" />
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
    </div>
  )
}
