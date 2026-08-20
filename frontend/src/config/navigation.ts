import {
  Activity,
  FileText,
  HelpCircle,
  LayoutDashboard,
  Map,
  Wrench,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

export interface NavigationItem {
  id: string
  label: string
  path: string
  icon: LucideIcon
}

// 所有业务页面共用这一份导航配置，避免不同页面菜单不一致或点击无响应。
export const NAV_ITEMS: NavigationItem[] = [
  { id: 'dashboard', label: '仪表盘', icon: LayoutDashboard, path: '/dashboard' },
  { id: 'monitoring', label: '实时监测', icon: Activity, path: '/monitoring' },
  { id: 'digital-twin', label: '数字孪生', icon: Map, path: '/digital-twin' },
  { id: 'repair', label: '报修管理', icon: Wrench, path: '/repair' },
  { id: 'reports', label: '数据报表', icon: FileText, path: '/reports' },
  { id: 'help', label: '帮助中心', icon: HelpCircle, path: '/help' },
]

export function getActiveNavigation(pathname: string): NavigationItem {
  return NAV_ITEMS.find((item) => pathname === item.path || pathname.startsWith(`${item.path}/`)) ?? NAV_ITEMS[0]
}
