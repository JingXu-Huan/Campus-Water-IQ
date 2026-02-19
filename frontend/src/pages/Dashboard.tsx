import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { Droplets, LogOut, User, BarChart3, AlertTriangle, Settings } from 'lucide-react'

export default function Dashboard() {
  const navigate = useNavigate()
  const { clearAuth } = useAuthStore()

  const handleLogout = () => {
    clearAuth()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 bg-primary-100 rounded-lg">
              <Droplets className="w-6 h-6 text-primary-600" />
            </div>
            <h1 className="text-xl font-bold text-gray-900">校园水务数字孪生平台</h1>
          </div>
          <button
            onClick={handleLogout}
            className="flex items-center gap-2 px-4 py-2 text-gray-600 hover:text-gray-900 transition-colors"
          >
            <LogOut className="w-5 h-5" />
            <span>退出登录</span>
          </button>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          <div className="bg-white rounded-xl p-6 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-blue-100 rounded-lg">
                <Droplets className="w-6 h-6 text-blue-600" />
              </div>
              <span className="text-sm text-green-600">+12.5%</span>
            </div>
            <p className="text-sm text-gray-500">今日用水量</p>
            <p className="text-2xl font-bold text-gray-900">1,234 m³</p>
          </div>

          <div className="bg-white rounded-xl p-6 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-purple-100 rounded-lg">
                <BarChart3 className="w-6 h-6 text-purple-600" />
              </div>
              <span className="text-sm text-green-600">+8.3%</span>
            </div>
            <p className="text-sm text-gray-500">本月用水量</p>
            <p className="text-2xl font-bold text-gray-900">36,780 m³</p>
          </div>

          <div className="bg-white rounded-xl p-6 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-yellow-100 rounded-lg">
                <AlertTriangle className="w-6 h-6 text-yellow-600" />
              </div>
              <span className="text-sm text-red-600">+2</span>
            </div>
            <p className="text-sm text-gray-500">异常告警</p>
            <p className="text-2xl font-bold text-gray-900">3 条</p>
          </div>

          <div className="bg-white rounded-xl p-6 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-green-100 rounded-lg">
                <User className="w-6 h-6 text-green-600" />
              </div>
            </div>
            <p className="text-sm text-gray-500">在线设备</p>
            <p className="text-2xl font-bold text-gray-900">128 台</p>
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
              <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                <div>
                  <p className="font-medium text-gray-900">宿舍楼 A 区</p>
                  <p className="text-sm text-gray-500">水压正常 • 流速 2.5 L/s</p>
                </div>
                <span className="px-3 py-1 bg-green-100 text-green-700 text-sm rounded-full">正常</span>
              </div>
              <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                <div>
                  <p className="font-medium text-gray-900">教学楼 B 区</p>
                  <p className="text-sm text-gray-500">水压偏低 • 流速 0.8 L/s</p>
                </div>
                <span className="px-3 py-1 bg-yellow-100 text-yellow-700 text-sm rounded-full">预警</span>
              </div>
              <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                <div>
                  <p className="font-medium text-gray-900">食堂一楼</p>
                  <p className="text-sm text-gray-500">水压正常 • 流速 3.2 L/s</p>
                </div>
                <span className="px-3 py-1 bg-green-100 text-green-700 text-sm rounded-full">正常</span>
              </div>
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
      </main>
    </div>
  )
}
