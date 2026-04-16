import { useState, useRef, useEffect } from 'react'
import { MessageSquare, X, Send, Loader2, Sparkles } from 'lucide-react'
import { aiApi } from '../../api/aiApi'
import { useTheme } from '../../context/ThemeContext'

export default function ChatbotWidget() {
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState([
    { role: 'assistant', content: 'Hi! I\'m your Smart Campus assistant. Ask me anything — available rooms, how to raise a ticket, your bookings, and more.' }
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef(null)
  const { dark } = useTheme()

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const send = async () => {
    if (!input.trim() || loading) return
    const userMsg = { role: 'user', content: input }
    const history = messages.map(m => ({ role: m.role, content: m.content }))
    setMessages(prev => [...prev, userMsg])
    setInput('')
    setLoading(true)
    try {
      const res = await aiApi.chat(input)
      const reply = res.data?.data || 'Sorry, I could not get a response.'
      setMessages(prev => [...prev, { role: 'assistant', content: reply }])
    } catch {
      setMessages(prev => [...prev, { role: 'assistant', content: 'Sorry, something went wrong. Please try again.' }])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed bottom-6 right-6 z-50 flex flex-col items-end gap-3">
      {/* Chat window */}
      {open && (
        <div className={`
          w-80 rounded-2xl overflow-hidden shadow-2xl border flex flex-col
          ${dark ? 'bg-[#16162a] border-[#2a2a45]' : 'bg-white border-indigo-100'}
        `} style={{ height: '420px' }}>
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 bg-gradient-to-r from-indigo-600 to-purple-600">
            <div className="flex items-center gap-2">
              <Sparkles size={14} className="text-white" />
              <span className="text-sm font-semibold text-white font-display">Campus Assistant</span>
            </div>
            <button onClick={() => setOpen(false)} className="text-white/70 hover:text-white transition-colors">
              <X size={16} />
            </button>
          </div>

          {/* Messages */}
          <div className="flex-1 overflow-y-auto px-4 py-3 space-y-3">
            {messages.map((msg, i) => (
              <div key={i} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                <div className={`
                  max-w-[85%] rounded-2xl px-3 py-2 text-xs leading-relaxed font-body
                  ${msg.role === 'user'
                    ? 'bg-gradient-to-r from-indigo-500 to-purple-600 text-white rounded-br-sm'
                    : dark
                      ? 'bg-[#1e1e35] text-gray-300 rounded-bl-sm'
                      : 'bg-indigo-50 text-gray-700 rounded-bl-sm'
                  }
                `}>
                  {msg.content}
                </div>
              </div>
            ))}
            {loading && (
              <div className="flex justify-start">
                <div className={`rounded-2xl rounded-bl-sm px-4 py-2.5 ${dark ? 'bg-[#1e1e35]' : 'bg-indigo-50'}`}>
                  <div className="flex gap-1">
                    <span className="w-1.5 h-1.5 rounded-full bg-indigo-400 animate-bounce" style={{ animationDelay: '0ms' }} />
                    <span className="w-1.5 h-1.5 rounded-full bg-indigo-400 animate-bounce" style={{ animationDelay: '150ms' }} />
                    <span className="w-1.5 h-1.5 rounded-full bg-indigo-400 animate-bounce" style={{ animationDelay: '300ms' }} />
                  </div>
                </div>
              </div>
            )}
            <div ref={bottomRef} />
          </div>

          {/* Input */}
          <div className={`px-3 py-3 border-t ${dark ? 'border-[#2a2a45]' : 'border-indigo-100'}`}>
            <div className={`flex items-center gap-2 rounded-xl px-3 py-2 border ${dark ? 'bg-[#1e1e35] border-[#2a2a45]' : 'bg-indigo-50 border-indigo-100'}`}>
              <input
                value={input}
                onChange={e => setInput(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && send()}
                placeholder="Ask anything..."
                className={`flex-1 bg-transparent text-xs outline-none font-body ${dark ? 'text-white placeholder-gray-600' : 'text-gray-800 placeholder-gray-400'}`}
              />
              <button
                onClick={send}
                disabled={loading || !input.trim()}
                className={`p-1 rounded-lg transition-all ${loading || !input.trim() ? 'text-gray-600' : 'text-indigo-400 hover:text-indigo-300'}`}
              >
                {loading ? <Loader2 size={14} className="animate-spin" /> : <Send size={14} />}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Toggle button */}
      <button
        onClick={() => setOpen(o => !o)}
        className="w-12 h-12 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 text-white flex items-center justify-center shadow-lg hover:shadow-indigo-500/40 hover:scale-105 transition-all duration-200 glow-primary"
      >
        {open ? <X size={20} /> : <MessageSquare size={20} />}
      </button>
    </div>
  )
}
