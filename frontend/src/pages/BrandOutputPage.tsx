import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { api, BrandOutput } from '../api/client'

export default function BrandOutputPage() {
  const { brandId } = useParams<{ brandId: string }>()
  const navigate = useNavigate()
  const [brand, setBrand] = useState<BrandOutput | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.getBrandOutput(brandId!).then(setBrand).catch(() => setError('Failed to load brand output.'))
  }, [brandId])

  if (error) return (
    <div className="flex items-center justify-center min-h-screen text-red-400">{error}</div>
  )
  if (!brand) return (
    <div className="flex items-center justify-center min-h-screen text-gray-400">Loading…</div>
  )

  return (
    <div className="max-w-3xl mx-auto px-4 py-12 space-y-12">

      {/* Hero */}
      <div className="text-center space-y-3">
        <p className="text-indigo-400 text-sm font-medium uppercase tracking-widest">Your Brand Identity</p>
        <h1 className="text-4xl font-bold text-white">"{brand.tagline}"</h1>
        <p className="text-gray-400 text-lg">{brand.missionStatement}</p>
      </div>

      {/* Logo */}
      {brand.logoImageUrl && (
        <section className="flex flex-col items-center space-y-3">
          <h2 className="text-xs font-semibold text-gray-500 uppercase tracking-widest">Logo Concept</h2>
          <img
            src={brand.logoImageUrl}
            alt="Logo concept"
            className="w-64 h-64 object-contain rounded-2xl bg-white p-4"
          />
        </section>
      )}

      {/* Colour Palette */}
      {brand.colourPalette?.length > 0 && (
        <section className="space-y-4">
          <h2 className="text-xs font-semibold text-gray-500 uppercase tracking-widest">Colour Palette</h2>
          <div className="grid grid-cols-5 gap-3">
            {brand.colourPalette.map((c) => (
              <div key={c.hex} className="space-y-2">
                <div
                  className="w-full aspect-square rounded-xl border border-gray-700"
                  style={{ backgroundColor: c.hex }}
                />
                <div className="space-y-0.5">
                  <p className="text-xs font-medium text-gray-300">{c.name}</p>
                  <p className="text-xs text-gray-500 font-mono">{c.hex}</p>
                  <p className="text-xs text-gray-600">{c.role}</p>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Typography */}
      {brand.typography && (
        <section className="space-y-4">
          <h2 className="text-xs font-semibold text-gray-500 uppercase tracking-widest">Typography</h2>
          <div className="grid grid-cols-2 gap-4">
            <div className="bg-gray-900 rounded-xl p-4 space-y-1 border border-gray-800">
              <p className="text-xs text-gray-500">Primary</p>
              <p className="text-lg font-semibold text-white">{brand.typography.primaryFont}</p>
              <p className="text-xs text-gray-400">{brand.typography.primaryUsage}</p>
            </div>
            <div className="bg-gray-900 rounded-xl p-4 space-y-1 border border-gray-800">
              <p className="text-xs text-gray-500">Secondary</p>
              <p className="text-lg font-semibold text-white">{brand.typography.secondaryFont}</p>
              <p className="text-xs text-gray-400">{brand.typography.secondaryUsage}</p>
            </div>
          </div>
        </section>
      )}

      {/* Copy */}
      <section className="space-y-6">
        <h2 className="text-xs font-semibold text-gray-500 uppercase tracking-widest">Brand Copy</h2>
        <CopyBlock label="Brand Story" text={brand.brandStory} />
        <CopyBlock label="Elevator Pitch" text={brand.elevatorPitch} />
        <CopyBlock label="Tone of Voice" text={brand.toneGuide} />
      </section>

      {/* Restart */}
      <div className="flex justify-center pt-4">
        <button
          onClick={() => navigate('/')}
          className="px-6 py-3 border border-gray-700 text-gray-400 hover:text-white
                     hover:border-gray-500 rounded-xl text-sm transition-colors"
        >
          Build another brand
        </button>
      </div>
    </div>
  )
}

function CopyBlock({ label, text }: { label: string; text: string }) {
  return (
    <div className="bg-gray-900 rounded-xl p-5 space-y-2 border border-gray-800">
      <p className="text-xs font-medium text-gray-500 uppercase tracking-wider">{label}</p>
      <p className="text-gray-300 leading-relaxed">{text}</p>
    </div>
  )
}
