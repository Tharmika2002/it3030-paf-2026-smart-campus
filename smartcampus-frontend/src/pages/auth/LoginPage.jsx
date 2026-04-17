import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useTheme } from '../../context/ThemeContext'
import { Zap, Sun, Moon, Shield, Sparkles, Building2 } from 'lucide-react'

export default function LoginPage() {
  const { token } = useAuth()
  const { dark, toggle } = useTheme()
  const navigate = useNavigate()

  useEffect(() => {
    if (token) navigate('/resources', { replace: true })
  }, [token])

  const handleGoogleLogin = () => {
    window.location.href = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/oauth2/authorization/google`
  }

  return (
    <div className={`min-h-screen flex ${dark ? 'bg-[#0a0a14]' : 'bg-gradient-to-br from-slate-50 to-indigo-50'}`}>

      {/* Left panel - branding */}
      <div className={`hidden lg:flex lg:w-1/2 flex-col justify-between p-12 relative overflow-hidden ${
        dark
          ? 'bg-gradient-to-br from-indigo-950 via-[#16162a] to-purple-950'
          : 'bg-gradient-to-br from-indigo-600 via-indigo-700 to-purple-700'
      }`}>
        {/* Background decoration */}
        <div className="absolute top-0 left-0 w-full h-full opacity-10">
          <div className="absolute top-20 left-20 w-64 h-64 rounded-full bg-white blur-3xl" />
          <div className="absolute bottom-20 right-10 w-48 h-48 rounded-full bg-purple-300 blur-3xl" />
        </div>

        {/* Logo */}
        <div className="relative flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-white/20 backdrop-blur-sm flex items-center justify-center">
            <Zap size={20} className="text-white" />
          </div>
          <div>
            <h1 className="font-display font-bold text-white text-lg">Smart Campus</h1>
            <p className="text-indigo-200 text-xs">Operations Hub</p>
          </div>
        </div>

        {/* Center content */}
        <div className="relative">
          <h2 className="font-display font-bold text-white text-4xl leading-tight mb-4">
            Your campus,<br />
            <span className="text-indigo-200">intelligently managed.</span>
          </h2>
          <p className="text-indigo-200 text-sm leading-relaxed mb-8 max-w-md">
            Book facilities, manage incidents, and get real-time updates across your entire campus — powered by AI.
          </p>

          {/* Feature pills */}
          <div className="flex flex-wrap gap-3">
            {[
              { icon: Building2, label: 'Smart facility booking' },
              { icon: Sparkles, label: 'AI-powered search' },
              { icon: Shield, label: 'Role-based access' },
            ].map(({ icon: Icon, label }) => (
              <div key={label} className="flex items-center gap-2 bg-white/10 backdrop-blur-sm rounded-full px-3 py-1.5">
                <Icon size={12} className="text-indigo-200" />
                <span className="text-white text-xs font-medium">{label}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Bottom */}
        <div className="relative">
          <p className="text-indigo-300 text-xs">
            SLIIT — Faculty of Computing &nbsp;·&nbsp; IT3030 PAF 2026
          </p>
        </div>
      </div>

      {/* Right panel - login form */}
      <div className="flex-1 flex flex-col items-center justify-center p-8 relative">

        {/* Theme toggle */}
        <button
          onClick={toggle}
          className={`absolute top-6 right-6 p-2 rounded-lg transition-colors ${dark ? 'text-gray-400 hover:text-white hover:bg-white/5' : 'text-gray-500 hover:bg-gray-100'}`}
        >
          {dark ? <Sun size={18} /> : <Moon size={18} />}
        </button>

        {/* Mobile logo */}
        <div className="lg:hidden flex items-center gap-3 mb-8">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center">
            <Zap size={20} className="text-white" />
          </div>
          <div>
            <h1 className={`font-display font-bold text-lg ${dark ? 'text-white' : 'text-gray-900'}`}>Smart Campus</h1>
            <p className={`text-xs ${dark ? 'text-indigo-400' : 'text-indigo-500'}`}>Operations Hub</p>
          </div>
        </div>

        {/* Login card */}
        <div className={`w-full max-w-sm rounded-3xl p-8 border ${dark ? 'bg-[#16162a] border-[#2a2a45]' : 'bg-white border-indigo-100 shadow-xl shadow-indigo-100'}`}>
          <div className="mb-8">
            <h2 className={`font-display font-bold text-2xl mb-1 ${dark ? 'text-white' : 'text-gray-900'}`}>
              Welcome back
            </h2>
            <p className={`text-sm ${dark ? 'text-gray-500' : 'text-gray-500'}`}>
              Sign in to access the Smart Campus system
            </p>
          </div>

          {/* Google Sign In */}
          <button
            onClick={handleGoogleLogin}
            className={`
              w-full flex items-center justify-center gap-3 px-4 py-3.5 rounded-xl border text-sm font-medium
              transition-all duration-200 hover:scale-[1.02] active:scale-[0.98]
              ${dark
                ? 'bg-white/5 border-[#3a3a55] text-white hover:bg-white/10 hover:border-indigo-500/50'
                : 'bg-white border-gray-200 text-gray-700 hover:bg-gray-50 hover:border-indigo-200 shadow-sm'
              }
            `}
          >
            {/* Google SVG icon */}
            <svg width="18" height="18" viewBox="0 0 48 48">
              <path fill="#FFC107" d="M43.6 20H24v8h11.3C33.7 33.4 29.3 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.8 1.2 7.9 3.1l5.7-5.7C34.1 6.5 29.3 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20c11 0 19.7-8 19.7-20 0-1.3-.1-2.7-.1-4z"/>
              <path fill="#FF3D00" d="M6.3 14.7l6.6 4.8C14.6 15.1 18.9 12 24 12c3.1 0 5.8 1.2 7.9 3.1l5.7-5.7C34.1 6.5 29.3 4 24 4 16.3 4 9.7 8.4 6.3 14.7z"/>
              <path fill="#4CAF50" d="M24 44c5.2 0 9.9-1.9 13.5-5.1l-6.2-5.2C29.5 35.5 26.9 36 24 36c-5.2 0-9.6-3.5-11.2-8.2l-6.5 5C9.6 39.4 16.3 44 24 44z"/>
              <path fill="#1976D2" d="M43.6 20H24v8h11.3c-.8 2.3-2.3 4.2-4.3 5.6l6.2 5.2c3.7-3.4 5.8-8.5 5.8-14.8 0-1.3-.1-2.7-.1-4z"/>
            </svg>
            <span>Continue with Google</span>
          </button>

          {/* Divider */}
          <div className="flex items-center gap-3 my-6">
            <div className={`flex-1 h-px ${dark ? 'bg-[#2a2a45]' : 'bg-gray-100'}`} />
            <span className={`text-xs ${dark ? 'text-gray-600' : 'text-gray-400'}`}>Secured by OAuth 2.0</span>
            <div className={`flex-1 h-px ${dark ? 'bg-[#2a2a45]' : 'bg-gray-100'}`} />
          </div>

          {/* Info */}
          <div className={`flex items-start gap-3 p-3 rounded-xl ${dark ? 'bg-indigo-500/5 border border-indigo-500/15' : 'bg-indigo-50'}`}>
            <Shield size={14} className={`flex-shrink-0 mt-0.5 ${dark ? 'text-indigo-400' : 'text-indigo-500'}`} />
            <p className={`text-xs leading-relaxed ${dark ? 'text-indigo-300' : 'text-indigo-700'}`}>
              Your SLIIT Google account will be used for authentication. No passwords stored.
            </p>
          </div>
        </div>

        <p className={`mt-6 text-xs ${dark ? 'text-gray-700' : 'text-gray-400'}`}>
          IT3030 Programming Applications and Frameworks · SLIIT 2026
        </p>
      </div>
    </div>
  )
}
