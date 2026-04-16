import { Bell, Search } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import { useTheme } from '../../context/ThemeContext'

export default function Navbar({ title, subtitle }) {
  const { dark } = useTheme()

  return (
    <header className={`
      fixed top-0 left-64 right-0 h-16 z-30 flex items-center justify-between px-6
      ${dark
        ? 'bg-[#0f0f1a]/80 border-b border-[#2a2a45] backdrop-blur-md'
        : 'bg-white/80 border-b border-indigo-100 backdrop-blur-md'
      }
    `}>
      <div>
        <h2 className={`font-display font-semibold text-lg leading-tight ${dark ? 'text-white' : 'text-gray-900'}`}>
          {title}
        </h2>
        {subtitle && (
          <p className={`text-xs ${dark ? 'text-indigo-400' : 'text-indigo-500'}`}>{subtitle}</p>
        )}
      </div>

      <div className="flex items-center gap-3">
        <button className={`
          p-2 rounded-lg transition-colors relative
          ${dark ? 'text-gray-400 hover:text-white hover:bg-white/5' : 'text-gray-500 hover:bg-gray-100'}
        `}>
          <Bell size={18} />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-indigo-500 rounded-full" />
        </button>
      </div>
    </header>
  )
}
