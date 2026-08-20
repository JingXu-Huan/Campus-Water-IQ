import { useLocation, useNavigate } from 'react-router-dom'
import { NAV_ITEMS } from '@/config/navigation'

interface NavigationMenuProps {
  collapsed?: boolean
}

export default function NavigationMenu({ collapsed = false }: NavigationMenuProps) {
  const navigate = useNavigate()
  const { pathname } = useLocation()

  return (
    <ul className="space-y-1" aria-label="主导航">
      {NAV_ITEMS.map((item) => {
        const Icon = item.icon
        const active = pathname === item.path || pathname.startsWith(`${item.path}/`)

        return (
          <li key={item.id}>
            <button
              type="button"
              onClick={() => navigate(item.path)}
              title={collapsed ? item.label : undefined}
              aria-current={active ? 'page' : undefined}
              className={`app-nav-item w-full flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200 ${
                active ? 'app-nav-item-active' : 'app-nav-item-idle'
              } ${collapsed ? 'justify-center' : ''}`}
            >
              <Icon className="w-[18px] h-[18px] flex-shrink-0" />
              {!collapsed && <span className="font-medium text-sm">{item.label}</span>}
            </button>
          </li>
        )
      })}
    </ul>
  )
}
