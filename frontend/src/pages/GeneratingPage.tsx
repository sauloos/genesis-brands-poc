import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'

const STEPS = [
  'Analysing your brand signals…',
  'Crafting your brand story…',
  'Writing your tagline and mission…',
  'Building your colour palette…',
  'Selecting typography…',
  'Generating your logo concept…',
  'Assembling your brand identity…'
]

export default function GeneratingPage() {
  const { brandId } = useParams<{ brandId: string }>()
  const navigate = useNavigate()
  const [stepIndex, setStepIndex] = useState(0)
  const [triggered, setTriggered] = useState(false)

  // Trigger generation once on mount
  useEffect(() => {
    if (triggered) return
    setTriggered(true)
    api.triggerGeneration(brandId!).catch(console.error)
  }, [brandId, triggered])

  // Rotate through step labels for visual effect
  useEffect(() => {
    const interval = setInterval(() => {
      setStepIndex(i => (i + 1) % STEPS.length)
    }, 2800)
    return () => clearInterval(interval)
  }, [])

  // Poll for READY status
  useEffect(() => {
    const poll = setInterval(async () => {
      try {
        const status = await api.getBrandStatus(brandId!)
        if (status.status === 'READY') {
          clearInterval(poll)
          navigate(`/brand/${brandId}`)
        } else if (status.status === 'FAILED') {
          clearInterval(poll)
          navigate('/', { state: { error: status.error } })
        }
      } catch (e) {
        // keep polling
      }
    }, 3000)
    return () => clearInterval(poll)
  }, [brandId, navigate])

  const progress = ((stepIndex + 1) / STEPS.length) * 100

  return (
    <div className="flex flex-col items-center justify-center min-h-screen px-4">
      <div className="max-w-md w-full space-y-10 text-center">

        {/* Spinner */}
        <div className="flex justify-center">
          <div className="w-16 h-16 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin" />
        </div>

        {/* Status text */}
        <div className="space-y-2">
          <h2 className="text-2xl font-semibold text-white">Building your brand</h2>
          <p className="text-gray-400 h-6 transition-all">{STEPS[stepIndex]}</p>
        </div>

        {/* Progress bar */}
        <div className="w-full bg-gray-800 rounded-full h-1.5">
          <div
            className="bg-indigo-600 h-1.5 rounded-full transition-all duration-1000"
            style={{ width: `${progress}%` }}
          />
        </div>

        <p className="text-gray-600 text-sm">This takes about 60–90 seconds</p>
      </div>
    </div>
  )
}
