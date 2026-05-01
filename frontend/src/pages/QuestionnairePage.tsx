import { useState, useRef, useEffect } from 'react'
import { useNavigate, useParams, useLocation } from 'react-router-dom'
import { api } from '../api/client'

interface Message {
  role: 'question' | 'answer'
  text: string
}

export default function QuestionnairePage() {
  const { sessionId, brandId } = useParams<{ sessionId: string; brandId: string }>()
  const location = useLocation()
  const navigate = useNavigate()

  const [messages, setMessages] = useState<Message[]>([
    { role: 'question', text: location.state?.firstQuestion ?? 'Tell me about your business.' }
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const handleSend = async () => {
    const answer = input.trim()
    if (!answer || loading) return

    setInput('')
    setMessages(prev => [...prev, { role: 'answer', text: answer }])
    setLoading(true)

    try {
      const res = await api.submitAnswer(sessionId!, answer)

      if (res.complete) {
        navigate(`/generating/${brandId}`)
        return
      }

      setMessages(prev => [...prev, { role: 'question', text: res.nextQuestion! }])
    } catch (e) {
      setMessages(prev => [...prev, { role: 'question', text: 'Something went wrong. Please try again.' }])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex flex-col h-screen max-w-2xl mx-auto px-4">

      {/* Header */}
      <div className="py-6 border-b border-gray-800">
        <h2 className="text-sm font-medium text-gray-400 uppercase tracking-widest">
          Brand Discovery
        </h2>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto py-6 space-y-4">
        {messages.map((msg, i) => (
          <div
            key={i}
            className={`flex ${msg.role === 'answer' ? 'justify-end' : 'justify-start'}`}
          >
            <div className={`max-w-[80%] px-4 py-3 rounded-2xl text-sm leading-relaxed
              ${msg.role === 'question'
                ? 'bg-gray-800 text-gray-100 rounded-tl-sm'
                : 'bg-indigo-600 text-white rounded-tr-sm'
              }`}>
              {msg.text}
            </div>
          </div>
        ))}

        {loading && (
          <div className="flex justify-start">
            <div className="bg-gray-800 px-4 py-3 rounded-2xl rounded-tl-sm">
              <div className="flex gap-1">
                <span className="w-2 h-2 bg-gray-500 rounded-full animate-bounce [animation-delay:-0.3s]" />
                <span className="w-2 h-2 bg-gray-500 rounded-full animate-bounce [animation-delay:-0.15s]" />
                <span className="w-2 h-2 bg-gray-500 rounded-full animate-bounce" />
              </div>
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <div className="py-4 border-t border-gray-800">
        <div className="flex gap-3">
          <input
            type="text"
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleSend()}
            placeholder="Type your answer…"
            disabled={loading}
            className="flex-1 bg-gray-800 text-gray-100 placeholder-gray-500 px-4 py-3
                       rounded-xl border border-gray-700 focus:outline-none focus:border-indigo-500"
          />
          <button
            onClick={handleSend}
            disabled={loading || !input.trim()}
            className="px-5 py-3 bg-indigo-600 hover:bg-indigo-500 disabled:bg-gray-700
                       text-white rounded-xl font-medium transition-colors"
          >
            Send
          </button>
        </div>
      </div>
    </div>
  )
}
