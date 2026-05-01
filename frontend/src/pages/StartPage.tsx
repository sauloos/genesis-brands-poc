import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'

export default function StartPage() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleStart = async () => {
    setLoading(true)
    setError(null)
    try {
      const brand = await api.createBrand()
      const conversation = await api.startConversation(brand.id)
      navigate(`/questionnaire/${conversation.sessionId}/${brand.id}`, {
        state: { firstQuestion: conversation.firstQuestion }
      })
    } catch (e) {
      setError('Failed to start. Is the backend running?')
      setLoading(false)
    }
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-screen px-4">
      <div className="max-w-xl w-full text-center space-y-8">

        <div className="space-y-3">
          <h1 className="text-5xl font-bold tracking-tight text-white">
            Genesis Brands
          </h1>
          <p className="text-xl text-gray-400">
            Describe your business. Get a complete brand identity in minutes.
          </p>
        </div>

        <div className="space-y-4">
          <button
            onClick={handleStart}
            disabled={loading}
            className="w-full py-4 px-8 bg-indigo-600 hover:bg-indigo-500 disabled:bg-indigo-800
                       text-white font-semibold text-lg rounded-xl transition-colors"
          >
            {loading ? 'Starting…' : 'Build my brand →'}
          </button>
          {error && <p className="text-red-400 text-sm">{error}</p>}
        </div>

        <p className="text-gray-600 text-sm">
          No sign-up required · Takes 2–3 minutes
        </p>
      </div>
    </div>
  )
}
