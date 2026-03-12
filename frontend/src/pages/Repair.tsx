import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { repairApi, RepairOrder, RepairStatus, statusLabels, severityLabels, severityColors } from '@/api/repair'
import { 
  Droplets, LayoutDashboard, Activity, Map, FileText, Settings, HelpCircle, 
  Menu, X, RefreshCw, CheckCircle, Clock, AlertTriangle, Filter, Zap, Smile
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
    { id: 'help', label: '帮助中心', icon: HelpCircle, path: '/help' },
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
            <div className="glass-card rounded-2xl p-5 hover:shadow-lg transition-shadow">
              <div className="flex items-center justify-between mb-3">
                <div className="p-2.5 bg-gradient-to-br from-red-400 to-red-600 rounded-xl shadow-lg">
                  <AlertTriangle className="w-5 h-5 text-white" />
                </div>
              </div>
              <p className="text-3xl font-bold text-gray-900">{unclosedCount}</p>
              <p className="text-sm text-gray-500 mt-1">待确认报修</p>
            </div>
            <div className="glass-card rounded-2xl p-5 hover:shadow-lg transition-shadow">
              <div className="flex items-center justify-between mb-3">
                <div className="p-2.5 bg-gradient-to-br from-blue-400 to-blue-600 rounded-xl shadow-lg">
                  <Clock className="w-5 h-5 text-white" />
                </div>
              </div>
              <p className="text-3xl font-bold text-gray-900">
                {repairOrders.filter(o => o.status === 'CONFIRMED').length}
              </p>
              <p className="text-sm text-gray-500 mt-1">已确认待处理</p>
            </div>
            <div className="glass-card rounded-2xl p-5 hover:shadow-lg transition-shadow">
              <div className="flex items-center justify-between mb-3">
                <div className="p-2.5 bg-gradient-to-br from-yellow-400 to-yellow-600 rounded-xl shadow-lg">
                  <Activity className="w-5 h-5 text-white" />
                </div>
              </div>
              <p className="text-3xl font-bold text-gray-900">
                {repairOrders.filter(o => o.status === 'PROCESSING').length}
              </p>
              <p className="text-sm text-gray-500 mt-1">正在处理</p>
            </div>
            <div className="glass-card rounded-2xl p-5 hover:shadow-lg transition-shadow">
              <div className="flex items-center justify-between mb-3">
                <div className="p-2.5 bg-gradient-to-br from-green-400 to-green-600 rounded-xl shadow-lg">
                  <CheckCircle className="w-5 h-5 text-white" />
                </div>
              </div>
              <p className="text-3xl font-bold text-gray-900">
                {repairOrders.filter(o => o.status === 'DONE').length}
              </p>
              <p className="text-sm text-gray-500 mt-1">已完成</p>
            </div>
          </div>

          {/* 筛选 */}
          <div className="glass-card rounded-2xl p-4 mb-6">
            <div className="flex items-center justify-between flex-wrap gap-4">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-gradient-to-br from-primary-400 to-primary-600 rounded-xl">
                  <Filter className="w-4 h-4 text-white" />
                </div>
                <span className="text-sm font-medium text-gray-700">状态筛选</span>
              </div>
              <div className="flex flex-wrap gap-2">
                <button
                  onClick={() => setSelectedStatus('ALL')}
                  className={`px-4 py-2 rounded-xl text-sm font-medium transition-all duration-200 shadow-sm ${
                    selectedStatus === 'ALL'
                      ? 'bg-gradient-to-r from-primary-500 to-primary-600 text-white shadow-primary-300'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200 hover:shadow-md'
                  }`}
                >
                  全部
                </button>
                {STATUS_OPTIONS.map((option) => (
                  <button
                    key={option.value}
                    onClick={() => setSelectedStatus(option.value)}
                    className={`px-4 py-2 rounded-xl text-sm font-medium transition-all duration-200 shadow-sm ${
                      selectedStatus === option.value
                        ? 'bg-gradient-to-r from-primary-500 to-primary-600 text-white shadow-primary-300'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200 hover:shadow-md'
                    }`}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* 报修单列表 */}
          <div className="bg-white rounded-2xl shadow-sm overflow-hidden border border-gray-100">
            {loading ? (
              <div className="p-12 text-center">
                <div className="relative">
                  <div className="w-12 h-12 border-4 border-primary-100 border-t-primary-600 rounded-full animate-spin mx-auto"></div>
                </div>
                <p className="text-gray-500 mt-4">正在加载报修数据...</p>
              </div>
            ) : repairOrders.length === 0 ? (
              <div className="p-16 text-center">
                <div className="relative inline-block mb-6">
                  <div className="p-6 bg-gradient-to-br from-green-100 to-green-200 rounded-full">
                    <CheckCircle className="w-16 h-16 text-green-600" />
                  </div>
                  <div className="absolute -top-2 -right-2 w-8 h-8 bg-green-400 rounded-full flex items-center justify-center animate-bounce">
                    <Zap className="w-4 h-4 text-white" />
                  </div>
                </div>
                <h3 className="text-xl font-bold text-gray-900 mb-2">一切正常运行中！</h3>
                <p className="text-gray-500 mb-6 max-w-sm mx-auto">目前没有待处理的报修单，设备运行状态良好，继续保持！</p>
                <div className="inline-flex items-center gap-2 px-4 py-2 bg-green-50 rounded-full">
                  <Smile className="w-5 h-5 text-green-600" />
                  <span className="text-sm font-medium text-green-700">设备运行良好</span>
                </div>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-gradient-to-r from-gray-50 to-gray-100 border-b border-gray-200">
                    <tr>
                      <th className="px-6 py-4 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">
                        <div className="flex items-center gap-2">
                          <div className="w-1.5 h-1.5 bg-primary-500 rounded-full"></div>
                          设备编号
                        </div>
                      </th>
                      <th className="px-6 py-4 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">
                        <div className="flex items-center gap-2">
                          <div className="w-1.5 h-1.5 bg-blue-500 rounded-full"></div>
                          报修人
                        </div>
                      </th>
                      <th className="px-6 py-4 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">
                        <div className="flex items-center gap-2">
                          <div className="w-1.5 h-1.5 bg-purple-500 rounded-full"></div>
                          联系方式
                        </div>
                      </th>
                      <th className="px-6 py-4 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">
                        <div className="flex items-center gap-2">
                          <div className="w-1.5 h-1.5 bg-yellow-500 rounded-full"></div>
                          故障描述
                        </div>
                      </th>
                      <th className="px-6 py-4 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">
                        <div className="flex items-center gap-2">
                          <div className="w-1.5 h-1.5 bg-red-500 rounded-full"></div>
                          严重程度
                        </div>
                      </th>
                      <th className="px-6 py-4 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">
                        <div className="flex items-center gap-2">
                          <div className="w-1.5 h-1.5 bg-green-500 rounded-full"></div>
                          状态
                        </div>
                      </th>
                      <th className="px-6 py-4 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">
                        <div className="flex items-center gap-2">
                          <div className="w-1.5 h-1.5 bg-cyan-500 rounded-full"></div>
                          创建时间
                        </div>
                      </th>
                      <th className="px-6 py-4 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">操作</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {repairOrders.map((order) => (
                      <tr key={order.id} className="hover:bg-gradient-to-r hover:from-primary-50/50 hover:to-transparent transition-all duration-200 group">
                        <td className="px-6 py-4">
                          <span className="inline-flex items-center px-3 py-1.5 rounded-lg bg-gray-100 text-gray-800 font-mono text-sm group-hover:bg-primary-100 group-hover:text-primary-700 transition-colors">
                            {order.deviceCode}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-3">
                            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-white text-sm font-medium">
                              {order.reportName?.charAt(0) || '?'}
                            </div>
                            <span className="text-sm font-medium text-gray-900">{order.reportName}</span>
                          </div>
                        </td>
                        <td className="px-6 py-4">
                          <span className="text-sm text-gray-600">{order.contactInfo}</span>
                        </td>
                        <td className="px-6 py-4">
                          <span className="text-sm text-gray-600 max-w-[200px] truncate block" title={order.desc}>
                            {order.desc}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <span className={`inline-flex items-center px-3 py-1.5 text-xs font-bold rounded-full shadow-sm ${
                            severityColors[order.severity]?.bg || 'bg-gray-100'
                          } ${severityColors[order.severity]?.text || 'text-gray-600'}`}>
                            {severityLabels[order.severity] || '未知'}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <span className={`inline-flex items-center px-3 py-1.5 text-xs font-bold rounded-full shadow-sm ${getStatusColor(order.status as RepairStatus)}`}>
                            {statusLabels[order.status as RepairStatus] || order.status}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <span className="text-sm text-gray-500">
                            {order.createdAt ? new Date(order.createdAt).toLocaleString('zh-CN') : '-'}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <button
                            onClick={() => handleOpenStatusModal(order)}
                            className="inline-flex items-center gap-1.5 px-4 py-2 bg-gradient-to-r from-primary-500 to-primary-600 text-white rounded-lg text-sm font-medium shadow-sm hover:shadow-lg hover:shadow-primary-300/50 transition-all duration-200 hover:-translate-y-0.5"
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
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 animate-fade-in">
          <div className="bg-white rounded-3xl shadow-2xl w-full max-w-md mx-4 overflow-hidden animate-scale-in">
            {/* 弹窗头部 */}
            <div className="px-6 py-5 bg-gradient-to-r from-primary-500 to-primary-600">
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-bold text-white">修改报修单状态</h3>
                <button
                  onClick={() => setShowStatusModal(false)}
                  className="p-1.5 rounded-lg bg-white/20 hover:bg-white/30 transition-colors"
                >
                  <X className="w-5 h-5 text-white" />
                </button>
              </div>
            </div>
            
            <div className="p-6">
              {/* 设备信息卡片 */}
              <div className="bg-gradient-to-br from-gray-50 to-gray-100 rounded-2xl p-4 mb-5">
                <div className="flex items-center gap-3 mb-3">
                  <div className="p-2 bg-gradient-to-br from-primary-400 to-primary-600 rounded-xl">
                    <Droplets className="w-4 h-4 text-white" />
                  </div>
                  <span className="text-sm font-medium text-gray-600">设备信息</span>
                </div>
                <p className="text-gray-900 font-mono text-lg font-bold ml-11">{selectedOrder?.deviceCode}</p>
              </div>
              
              {/* 故障描述 */}
              <div className="mb-5">
                <label className="block text-sm font-medium text-gray-600 mb-2">故障描述</label>
                <div className="bg-amber-50 border border-amber-100 rounded-xl p-3">
                  <p className="text-gray-700 text-sm">{selectedOrder?.desc}</p>
                </div>
              </div>
              
              {/* 状态选择 */}
              <div className="mb-2">
                <label className="block text-sm font-medium text-gray-600 mb-3">
                  <span className="flex items-center gap-2">
                    <span className="w-1.5 h-1.5 bg-primary-500 rounded-full"></span>
                    选择新状态
                  </span>
                </label>
                <div className="grid grid-cols-2 gap-3">
                  {STATUS_OPTIONS.map((option) => (
                    <button
                      key={option.value}
                      onClick={() => setNewStatus(option.value)}
                      className={`px-4 py-3 rounded-xl text-sm font-medium transition-all duration-200 shadow-sm ${
                        newStatus === option.value
                          ? 'bg-gradient-to-r from-primary-500 to-primary-600 text-white shadow-lg shadow-primary-300/50 -translate-y-0.5'
                          : 'bg-gray-100 text-gray-600 hover:bg-gray-200 hover:shadow-md'
                      }`}
                    >
                      {option.label}
                    </button>
                  ))}
                </div>
              </div>
            </div>
            
            <div className="px-6 py-4 bg-gray-50 border-t border-gray-100 flex justify-end gap-3">
              <button
                onClick={() => setShowStatusModal(false)}
                className="px-5 py-2.5 text-gray-600 hover:bg-gray-200 rounded-xl transition-colors font-medium"
              >
                取消
              </button>
              <button
                onClick={handleChangeStatus}
                disabled={updating || newStatus === selectedOrder?.status}
                className="px-5 py-2.5 bg-gradient-to-r from-primary-500 to-primary-600 text-white rounded-xl hover:shadow-lg hover:shadow-primary-300/50 transition-all duration-200 font-medium disabled:opacity-50 disabled:hover:shadow-none"
              >
                {updating ? (
                  <span className="flex items-center gap-2">
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    提交中...
                  </span>
                ) : '确认修改'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
