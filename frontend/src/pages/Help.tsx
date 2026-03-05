import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { Droplets, Menu, X, HelpCircle, Book, MessageCircle, Mail, Phone, ChevronDown, ChevronRight, Search, LayoutDashboard, Activity, Map } from 'lucide-react'

// 常见问题数据
const faqs = [
  {
    category: '账号问题',
    questions: [
      {
        question: '如何登录系统？',
        answer: '在登录页面输入用户名和密码，点击登录按钮即可进入系统。如果忘记密码，可以通过注册页面找回或联系管理员重置。'
      },
      {
        question: '如何修改个人头像？',
        answer: '登录后点击右上角头像，选择"个人中心"，在弹出的对话框中可以上传和更换头像。'
      },
      {
        question: '如何修改登录密码？',
        answer: '进入个人中心，点击"密码"标签页，输入当前密码和新密码后保存即可。'
      }
    ]
  },
  {
    category: '校区管理',
    questions: [
      {
        question: '如何切换校区？',
        answer: '在左侧边栏底部点击校区名称即可切换。当前选中的校区会高亮显示。'
      },
      {
        question: '各校区有什么区别？',
        answer: '系统目前管理三个校区：花园校区、龙子湖校区和江淮校区。每个校区的设备和水表数据是独立统计的。'
      }
    ]
  },
  {
    category: '设备管理',
    questions: [
      {
        question: '设备离线了怎么办？',
        answer: '设备离线可能是网络问题或设备故障。请检查设备电源和网络连接，如果问题持续存在，请联系维修人员处理。'
      },
      {
        question: '如何查看设备状态？',
        answer: '在"实时监测"页面可以查看所有设备的在线状态、水压、流量等实时数据。'
      },
      {
        question: '水表数据异常如何处理？',
        answer: '如果在"最近告警"中看到水表异常信息，请及时检查对应的设备，确认是否存在漏水或其他问题。'
      }
    ]
  },
  {
    category: '数据报表',
    questions: [
      {
        question: '用水量数据从哪里来？',
        answer: '用水量数据来自各校区安装的智能水表，系统每日自动采集并汇总各设备用水数据。'
      },
      {
        question: '如何导出用水报表？',
        answer: '在"数据报表"页面可以选择时间范围和校区，系统会生成相应的用水报表供下载。'
      }
    ]
  },
  {
    category: '报修管理',
    questions: [
      {
        question: '如何提交报修？',
        answer: '进入"报修管理"页面，点击"我要报修"按钮，填写故障描述和联系方式后提交即可。'
      },
      {
        question: '报修进度如何查询？',
        answer: '在"报修管理"页面的"我的报修"中可以查看所有报修记录的处理进度。'
      }
    ]
  }
]

// 联系方式数据
const contacts = [
  { icon: Phone, title: '服务热线', content: '400-888-8888', desc: '工作日 8:00-18:00' },
  { icon: Mail, title: '邮箱', content: 'support@water-campus.edu.cn', desc: '24小时内回复' },
  { icon: MessageCircle, title: '在线客服', content: '点击在线咨询', desc: '工作日 9:00-17:00' }
]

export default function Help() {
  const navigate = useNavigate()
  const { nickname } = useAuthStore()
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [activeMenu, setActiveMenu] = useState('help')
  const [searchKeyword, setSearchKeyword] = useState('')
  const [expandedCategory, setExpandedCategory] = useState<string>('账号问题')
  const [expandedQuestion, setExpandedQuestion] = useState<string>('')

  const menuItems = [
    { id: 'dashboard', label: '仪表盘', icon: LayoutDashboard, path: '/dashboard' },
    { id: 'monitoring', label: '实时监测', icon: Activity, path: '/monitoring' },
    { id: 'digital-twin', label: '数字孪生', icon: Map, path: '/digital-twin' },
    { id: 'repair', label: '报修管理', icon: Book, path: '/repair' },
    { id: 'help', label: '帮助中心', icon: HelpCircle, path: '/help' },
  ]

  const handleMenuClick = (item: typeof menuItems[0]) => {
    if (item.path) {
      navigate(item.path)
    } else {
      setActiveMenu(item.id)
    }
  }

  // 过滤搜索结果
  const filteredFaqs = searchKeyword 
    ? faqs.map(cat => ({
        ...cat,
        questions: cat.questions.filter(q => 
          q.question.includes(searchKeyword) || q.answer.includes(searchKeyword)
        )
      })).filter(cat => cat.questions.length > 0)
    : faqs

  return (
    <div className="h-screen bg-gradient-to-br from-slate-50 to-blue-50 flex overflow-hidden">
      {/* 侧边栏 */}
      <aside className={`${sidebarOpen ? 'w-64' : 'w-20'} bg-gradient-to-b from-primary-600 to-primary-800 shadow-xl transition-all duration-300 flex flex-col h-screen`}>
        <div className="p-4 border-b border-white/10">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 bg-white/20 backdrop-blur-sm rounded-xl flex-shrink-0">
              <Droplets className="w-6 h-6 text-white" />
            </div>
            {sidebarOpen && (
              <h1 className="text-lg font-bold text-white">水务平台</h1>
            )}
          </div>
        </div>

        <nav className="flex-1 p-2 overflow-y-auto">
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

        <div className="p-2 border-t border-white/10 flex-shrink-0">
          <div className="flex items-center gap-3 px-4 py-3 rounded-xl hover:bg-white/10 transition-all cursor-pointer">
            {sidebarOpen && (
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-white truncate">
                  {nickname || '用户'}
                </p>
              </div>
            )}
          </div>
        </div>
      </aside>

      {/* 主内容区 */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* 顶部导航 */}
        <header className="bg-gradient-to-r from-primary-600 to-primary-800 shadow-lg">
          <div className="px-6 py-4 flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={() => setSidebarOpen(!sidebarOpen)}
                className="p-2 text-white/80 hover:text-white hover:bg-white/10 rounded-xl transition-all"
              >
                {sidebarOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
              </button>
              <h2 className="text-xl font-bold text-white">帮助中心</h2>
            </div>
          </div>
        </header>

        {/* 内容区 */}
        <main className="flex-1 p-6 overflow-y-auto">
          <div className="max-w-4xl mx-auto">
            {/* 搜索框 */}
            <div className="glass-card rounded-2xl p-6 mb-6">
              <div className="flex items-center gap-3 mb-4">
                <HelpCircle className="w-6 h-6 text-primary-600" />
                <h2 className="text-xl font-bold text-gray-900">常见问题</h2>
              </div>
              <div className="relative">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  type="text"
                  placeholder="搜索问题..."
                  value={searchKeyword}
                  onChange={(e) => setSearchKeyword(e.target.value)}
                  className="w-full pl-12 pr-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
                />
              </div>
            </div>

            {/* FAQ 列表 */}
            <div className="space-y-4 mb-6">
              {filteredFaqs.map((category) => (
                <div key={category.category} className="glass-card rounded-2xl overflow-hidden">
                  <button
                    onClick={() => setExpandedCategory(
                      expandedCategory === category.category ? '' : category.category
                    )}
                    className="w-full flex items-center justify-between p-5 hover:bg-gray-50 transition-colors"
                  >
                    <span className="font-semibold text-gray-900">{category.category}</span>
                    {expandedCategory === category.category ? (
                      <ChevronDown className="w-5 h-5 text-gray-400" />
                    ) : (
                      <ChevronRight className="w-5 h-5 text-gray-400" />
                    )}
                  </button>
                  
                  {expandedCategory === category.category && (
                    <div className="border-t border-gray-100">
                      {category.questions.map((item, idx) => (
                        <div key={idx} className={idx !== 0 ? 'border-t border-gray-100' : ''}>
                          <button
                            onClick={() => setExpandedQuestion(
                              expandedQuestion === `${category.category}-${idx}` ? '' : `${category.category}-${idx}`
                            )}
                            className="w-full flex items-center justify-between p-4 hover:bg-gray-50 transition-colors text-left"
                          >
                            <span className="text-gray-700 font-medium">{item.question}</span>
                            {expandedQuestion === `${category.category}-${idx}` ? (
                              <ChevronDown className="w-4 h-4 text-gray-400 flex-shrink-0" />
                            ) : (
                              <ChevronRight className="w-4 h-4 text-gray-400 flex-shrink-0" />
                            )}
                          </button>
                          {expandedQuestion === `${category.category}-${idx}` && (
                            <div className="px-4 pb-4 text-gray-600 text-sm leading-relaxed">
                              {item.answer}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>

            {/* 联系方式 */}
            <div className="glass-card rounded-2xl p-6">
              <div className="flex items-center gap-3 mb-4">
                <MessageCircle className="w-6 h-6 text-primary-600" />
                <h2 className="text-xl font-bold text-gray-900">联系我们</h2>
              </div>
              <p className="text-gray-600 mb-4">如果您没有找到需要的答案，可以通过以下方式联系我们：</p>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {contacts.map((contact, idx) => (
                  <div key={idx} className="flex items-start gap-3 p-4 bg-gray-50 rounded-xl">
                    <div className="p-2 bg-primary-100 rounded-lg">
                      <contact.icon className="w-5 h-5 text-primary-600" />
                    </div>
                    <div>
                      <p className="font-medium text-gray-900">{contact.title}</p>
                      <p className="text-sm text-primary-600">{contact.content}</p>
                      <p className="text-xs text-gray-500 mt-1">{contact.desc}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  )
}
