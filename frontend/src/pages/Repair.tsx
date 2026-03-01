import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { repairApi, RepairOrder, RepairStatus, statusLabels, severityLabels, severityColors } from '@/api/repair'
import { 
  Droplets, LayoutDashboard, Activity, Map, FileText, Settings, HelpCircle, 
  Menu, X, RefreshCw, CheckCircle, Clock, AlertTriangle, Filter
} from 'lucide-react'

// 状态选项
const STATUS_OPTIONS: { value: RepairStatus; label: string }[] = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'CONFIRMED', label: '已确认' },
  { value: 'PROCESSING', label: '待确认' },
  { value: 'DONE', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
]

// 状态颜色映射
const getStatusColor = (status: RepairStatus) => {
  switch (status) {
    case 'DRAFT': return 'bg-gray-100 text-gray-600'
    case 'CONFIRMED': return 'bg-blue-100 text-blue-600'
    case 'PROCESSING': return 'bg-yellow-100 text-yellow-600'
    case 'DONE': return 'bg-green-100 text-green-600'
    case 'CANCELLED': return 'bg-red-100 text-red-600'
    default: return 'bg-gray-100 text-gray-600'
  }
}

export default function Repair() {
  const navigate = useNavigate()
  const { nickname } = useAuthStore()
  
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [activeMenu, setActiveMenu] = useState('repair')
  const [loading, setLoading] = useState(false)
  const [refreshing, setRefreshing] = useState(false)
  
  // 数据状态
  const [repairOrders, setRepairOrders] = useState<RepairOrder[]>([])
  const [selectedStatus, setSelectedStatus] = useState<RepairStatus | 'ALL'>('ALL')
  const [unclosedCount, setUnclosedCount] = useState(0)
  
  // 模态框
  const [showStatusModal, setShowStatusModal] = useState(false)
  const [selectedOrder, setSelectedOrder] = useState<RepairOrder | null>(null)
  const [newStatus, setNewStatus] = useState<RepairStatus>('CONFIRMED')
  const [updating, setUpdating] = useState(false)

  // 菜单项
  const menuItems = [
    { id: 'dashboard', label: '仪表盘', icon: LayoutDashboard, path: '/dashboard' },
    { id: 'monitoring', label: '实时监测', icon: Activity, path: '/monitoring' },
    { id: 'digital-twin', label: '数字孪生', icon: Map, path: '/digital-twin' },
    { id: 'repair', label: '报修管理', icon: FileText, path: '/repair' },
    { id: 'settings', label: '系统设置', icon: Settings, path: '' },
    { id: 'help', label: '帮助中心', icon: HelpCircle, path: '' },
  ]

  const handleMenuClick = (item: typeof menuItems[0]) => {
    if (item.path && item.path !== '/repair') {
      navigate(item.path)
    } else if (item.id !== 'repair') {
      setActiveMenu(item.id)
    }
  }

  // 获取报修单数据
  const fetchRepairOrders = async () => {
    setLoading(true)
    try {
      if (selectedStatus === 'ALL') {
        // 获取所有状态的报修单
        const allOrders: RepairOrder[] = []
        for (const status of ['DRAFT', 'CONFIRMED', 'PROCESSING', 'DONE', 'CANCELLED'] as RepairStatus[]) {
          try {
            const res = await repairApi.getByStatus(status, 1, 100) as any
            if (res?.data) {
              allOrders.push(...res.data)
            }
          } catch (e) {
            console.error(`获取${status}状态报修单失败:`, e)
          }
        }
        // 按时间倒序
        allOrders.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
        setRepairOrders(allOrders)
      } else {
        const res = await repairApi.getByStatus(selectedStatus, 1, 100) as any
        setRepairOrders(res?.data || [])
      }
    } catch (error) {
      console.error('获取报修单失败:', error)
    } finally {
      setLoading(false)
    }
  }

  // 获取未解决数量
  const fetchUnclosedCount = async () => {
    try {
      const res = await repairApi.getUnclosedCount() as any
      setUnclosedCount(res?.data || 0)
    } catch (error) {
      console.error('获取未解决数量失败:', error)
    }
  }

  // 刷新数据
  const handleRefresh = async () => {
    setRefreshing(true)
    await Promise.all([fetchRepairOrders(), fetchUnclosedCount()])
    setRefreshing(false)
  }

  // 打开状态修改弹窗
  const handleOpenStatusModal = (order: RepairOrder) => {
    setSelectedOrder(order)
    setNewStatus(order.status as RepairStatus)
    setShowStatusModal(true)
  }

  // 修改状态
  const handleChangeStatus = async () => {
    if (!selectedOrder) return
    setUpdating(true)
    try {
      await repairApi.changeStatus(newStatus, selectedOrder.id)
      setShowStatusModal(false)
      handleRefresh()
    } catch (error) {
      console.error('修改状态失败:', error)
    } finally {
      setUpdating(false)
    }
  }

  // 初始化
  useEffect(() => {
    handleRefresh()
  }, [selectedStatus])

  return (
    <div className="h-screen bg-gray-50 flex overflow-hidden">
      {/* 侧边栏 */}
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

        {/* 导航菜单 */}
        <nav className="flex-1 p-2 overflow-y-auto">
          <p className="px-4 mb-2 text-xs font-medium text-white/40 uppercase">菜单</p>
          <ul className="space-y-1">
            {menuItems.map((item) => {
              const Icon = item.icon
              return (
                <li key={item.id}>
                  <button
                    onClick={() => handleMenuClick(item)}
                    className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 ${
                      activeMenu === item.id
                        ? 'bg-white/20 text-white border border-white/30 shadow-lg'
                        : 'text-white/70 hover:bg-white/10 hover:text-white'
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
        </nav>

        {/* 用户信息 */}
        <div className="p-4 border-t border-white/10 flex-shrink-0">
          <div className={`flex items-center gap-3 ${sidebarOpen ? '' : 'justify-center'}`}>
            <div className="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center">
              <span className="text-white font-medium">{nickname?.charAt(0) || '用户'}</span>
            </div>
            {sidebarOpen && (
              <div className="flex-1 min-w-0">
                <p className="text-white text-sm font-medium truncate">{nickname || '用户'}</p>
              </div>
            )}
          </div>
        </div>
      </aside>

      {/* 主内容区 */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* 顶部Header */}
        <header className="bg-white shadow-sm border-b border-gray-200 flex-shrink-0">
          <div className="flex items-center justify-between px-6 py-4">
            <div className="flex items-center gap-4">
              <button
                onClick={() => setSidebarOpen(!sidebarOpen)}
                className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
              >
                {sidebarOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
              </button>
              <h2 className="text-xl font-semibold text-gray-900">报修管理</h2>
            </div>
            <div className="flex items-center gap-4">
              <button
                onClick={handleRefresh}
                disabled={refreshing}
                className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors disabled:opacity-50"
              >
                <RefreshCw className={`w-4 h-4 ${refreshing ? 'animate-spin' : ''}`} />
                刷新
              </button>
            </div>
          </div>
        </header>

        {/* 内容区域 */}
        <main className="flex-1 overflow-auto p-6">
          {/* 统计卡片 */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
            <div className="bg-white rounded-xl p-5 shadow-sm">
              <div className="flex items-center gap-3 mb-2">
                <div className="p-2.5 bg-red-100 rounded-xl">
                  <AlertTriangle className="w-5 h-5 text-red-600" />
                </div>
                <span className="text-sm font-medium text-gray-700">待确认</span>
              </div>
              <p className="text-2xl font-bold text-gray-900">{unclosedCount}</p>
            </div>
            <div className="bg-white rounded-xl p-5 shadow-sm">
              <div className="flex items-center gap-3 mb-2">
                <div className="p-2.5 bg-blue-100 rounded-xl">
                  <Clock className="w-5 h-5 text-blue-600" />
                </div>
                <span className="text-sm font-medium text-gray-700">已确认</span>
              </div>
              <p className="text-2xl font-bold text-gray-900">
                {repairOrders.filter(o => o.status === 'CONFIRMED').length}
              </p>
            </div>
            <div className="bg-white rounded-xl p-5 shadow-sm">
              <div className="flex items-center gap-3 mb-2">
                <div className="p-2.5 bg-yellow-100 rounded-xl">
                  <Activity className="w-5 h-5 text-yellow-600" />
                </div>
                <span className="text-sm font-medium text-gray-700">待确认</span>
              </div>
              <p className="text-2xl font-bold text-gray-900">
                {repairOrders.filter(o => o.status === 'PROCESSING').length}
              </p>
            </div>
            <div className="bg-white rounded-xl p-5 shadow-sm">
              <div className="flex items-center gap-3 mb-2">
                <div className="p-2.5 bg-green-100 rounded-xl">
                  <CheckCircle className="w-5 h-5 text-green-600" />
                </div>
                <span className="text-sm font-medium text-gray-700">已完成</span>
              </div>
              <p className="text-2xl font-bold text-gray-900">
                {repairOrders.filter(o => o.status === 'DONE').length}
              </p>
            </div>
          </div>

          {/* 筛选 */}
          <div className="bg-white rounded-xl p-4 shadow-sm mb-6">
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2">
                <Filter className="w-4 h-4 text-gray-500" />
                <span className="text-sm text-gray-600">状态筛选:</span>
              </div>
              <div className="flex flex-wrap gap-2">
                <button
                  onClick={() => setSelectedStatus('ALL')}
                  className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                    selectedStatus === 'ALL'
                      ? 'bg-primary-600 text-white'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                  }`}
                >
                  全部
                </button>
                {STATUS_OPTIONS.map((option) => (
                  <button
                    key={option.value}
                    onClick={() => setSelectedStatus(option.value)}
                    className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                      selectedStatus === option.value
                        ? 'bg-primary-600 text-white'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    }`}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* 报修单列表 */}
          <div className="bg-white rounded-xl shadow-sm overflow-hidden">
            {loading ? (
              <div className="p-8 text-center text-gray-500">
                <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2" />
                加载中...
              </div>
            ) : repairOrders.length === 0 ? (
              <div className="p-8 text-center text-gray-500">
                暂无报修单数据
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-gray-50 border-b border-gray-200">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">设备编号</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">报修人</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">联系方式</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">故障描述</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">严重程度</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">状态</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">创建时间</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">操作</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    {repairOrders.map((order) => (
                      <tr key={order.id} className="hover:bg-gray-50">
                        <td className="px-6 py-4 text-sm text-gray-900 font-mono">{order.deviceCode}</td>
                        <td className="px-6 py-4 text-sm text-gray-900">{order.reportName}</td>
                        <td className="px-6 py-4 text-sm text-gray-600">{order.contactInfo}</td>
                        <td className="px-6 py-4 text-sm text-gray-600 max-w-xs truncate" title={order.desc}>
                          {order.desc}
                        </td>
                        <td className="px-6 py-4">
                          <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${
                            severityColors[order.severity]?.bg || 'bg-gray-100'
                          } ${severityColors[order.severity]?.text || 'text-gray-600'}`}>
                            {severityLabels[order.severity] || '未知'}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${getStatusColor(order.status as RepairStatus)}`}>
                            {statusLabels[order.status as RepairStatus] || order.status}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-sm text-gray-600">
                          {order.createdAt ? new Date(order.createdAt).toLocaleString('zh-CN') : '-'}
                        </td>
                        <td className="px-6 py-4">
                          <button
                            onClick={() => handleOpenStatusModal(order)}
                            className="text-primary-600 hover:text-primary-800 text-sm font-medium"
                          >
                            修改状态
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </main>
      </div>

      {/* 修改状态弹窗 */}
      {showStatusModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4">
            <div className="px-6 py-4 border-b border-gray-200">
              <h3 className="text-lg font-semibold text-gray-900">修改报修单状态</h3>
            </div>
            <div className="p-6">
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">设备编号</label>
                <p className="text-gray-900 font-mono">{selectedOrder?.deviceCode}</p>
              </div>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">故障描述</label>
                <p className="text-gray-600 text-sm">{selectedOrder?.desc}</p>
              </div>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">选择新状态</label>
                <div className="grid grid-cols-2 gap-2">
                  {STATUS_OPTIONS.map((option) => (
                    <button
                      key={option.value}
                      onClick={() => setNewStatus(option.value)}
                      className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                        newStatus === option.value
                          ? 'bg-primary-600 text-white'
                          : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      }`}
                    >
                      {option.label}
                    </button>
                  ))}
                </div>
              </div>
            </div>
            <div className="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
              <button
                onClick={() => setShowStatusModal(false)}
                className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
              >
                取消
              </button>
              <button
                onClick={handleChangeStatus}
                disabled={updating || newStatus === selectedOrder?.status}
                className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors disabled:opacity-50"
              >
                {updating ? '提交中...' : '确认'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
