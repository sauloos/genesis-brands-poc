import { useEffect, useState, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { api, BrandOutput } from '../api/client'

export default function BrandBookPage() {
  const { brandId } = useParams<{ brandId: string }>()
  const navigate = useNavigate()
  const [brand, setBrand] = useState<BrandOutput | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [exporting, setExporting] = useState(false)
  const bookRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    api.getBrandOutput(brandId!).then(setBrand).catch(() => setError('Failed to load brand.'))
  }, [brandId])

  const exportPDF = async () => {
    if (!bookRef.current) return
    setExporting(true)
    try {
      const html2pdf = (await import('html2pdf.js')).default
      await html2pdf()
        .set({
          margin: 0,
          filename: 'brand-book.pdf',
          image: { type: 'jpeg', quality: 0.98 },
          html2canvas: { scale: 2, useCORS: true },
          jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
          pagebreak: { mode: ['avoid-all', 'css', 'legacy'] }
        })
        .from(bookRef.current)
        .save()
    } finally {
      setExporting(false)
    }
  }

  if (error) return (
    <div className="flex items-center justify-center min-h-screen text-red-400">{error}</div>
  )
  if (!brand) return (
    <div className="flex items-center justify-center min-h-screen text-gray-400">Loading…</div>
  )

  const primaryColour = brand.colourPalette?.find(c => c.role === 'primary')?.hex || '#1a1a2e'
  const neutralColour = brand.colourPalette?.find(c => c.role === 'neutral')?.hex || '#333333'
  const year = new Date().getFullYear()

  // Helper to determine if a colour is light (for text contrast)
  const isLightColour = (hex: string): boolean => {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
    if (!result) return false
    const r = parseInt(result[1], 16)
    const g = parseInt(result[2], 16)
    const b = parseInt(result[3], 16)
    const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
    return luminance > 0.5
  }

  return (
    <div className="min-h-screen bg-gray-950">
      {/* Controls */}
      <div className="fixed top-4 right-4 z-50 flex gap-3">
        <button
          onClick={() => navigate(`/brand/${brandId}/output`)}
          className="px-4 py-2 bg-gray-800 text-gray-300 rounded-lg text-sm hover:bg-gray-700 transition-colors"
        >
          ← Back
        </button>
        <button
          onClick={exportPDF}
          disabled={exporting}
          className="px-4 py-2 bg-indigo-600 text-white rounded-lg text-sm hover:bg-indigo-500 transition-colors disabled:opacity-50"
        >
          {exporting ? 'Exporting…' : 'Download PDF'}
        </button>
      </div>

      {/* Brand Book Content */}
      <div ref={bookRef} className="bg-white">

        {/* Page 1: Cover */}
        <section
          className="h-[297mm] w-[210mm] mx-auto relative overflow-hidden"
          style={{ backgroundColor: primaryColour }}
        >
          {/* Decorative gradient overlay */}
          <div
            className="absolute inset-0"
            style={{
              background: `linear-gradient(135deg, ${primaryColour} 0%, ${primaryColour}ee 50%, ${primaryColour}cc 100%)`
            }}
          />
          {/* Decorative shapes */}
          <div
            className="absolute -right-32 -top-32 w-96 h-96 rounded-full opacity-10"
            style={{ backgroundColor: '#ffffff' }}
          />
          <div
            className="absolute -left-16 bottom-48 w-64 h-64 rounded-full opacity-5"
            style={{ backgroundColor: '#ffffff' }}
          />

          <div className="absolute inset-0 flex flex-col">
            {/* Header */}
            <div className="p-12 flex justify-between items-start relative z-10">
              <div>
                <h1 className="text-4xl font-light text-white tracking-wide">BRAND BOOK</h1>
                <p className="text-xl text-white/70 mt-1">{year}</p>
              </div>
              {brand.logoImageUrl ? (
                <img src={brand.logoImageUrl} alt="Logo" className="w-24 h-24 object-contain bg-white rounded-lg p-2" />
              ) : (
                <div className="w-24 h-24 bg-white/20 rounded-lg flex items-center justify-center">
                  <span className="text-white/60 text-xs">Logo</span>
                </div>
              )}
            </div>

            {/* Center content */}
            <div className="flex-1 flex items-center justify-center relative z-10">
              {brand.logoImageUrl && (
                <img src={brand.logoImageUrl} alt="Logo" className="w-48 h-48 object-contain bg-white rounded-2xl p-6 shadow-2xl" />
              )}
            </div>

            {/* Tagline */}
            <div className="p-12 relative z-10">
              <p className="text-3xl text-white font-light italic leading-relaxed">"{brand.tagline}"</p>
            </div>
          </div>
        </section>

        {/* Page 2: Brand Philosophy */}
        <section
          className="h-[297mm] w-[210mm] mx-auto flex items-center justify-center p-16 relative overflow-hidden"
          style={{ backgroundColor: neutralColour }}
        >
          {/* Decorative element */}
          <div
            className="absolute right-0 top-0 w-1/3 h-full opacity-10"
            style={{ backgroundColor: primaryColour }}
          />
          <div className="max-w-lg text-center relative z-10">
            <p className="text-2xl text-white/90 font-light leading-relaxed">
              <span className="font-semibold text-white">Brand is more</span> than just a set of
              rules, it's a story. A story that communicates the very{' '}
              <span className="font-semibold text-white">core of your business</span> and{' '}
              <span className="font-semibold text-white">work ethic</span>. Brand doesn't just
              demonstrate the who, what, where or how but also{' '}
              <span className="font-semibold text-white">why</span>.
            </p>
          </div>
        </section>

        {/* Page 3: Mission & Vision */}
        <section className="h-[297mm] w-[210mm] mx-auto bg-white p-16 flex flex-col">
          <div className="border-l-4 border-gray-900 pl-8 mb-16">
            <h2 className="text-sm font-semibold text-gray-400 uppercase tracking-widest mb-4">Mission Statement</h2>
            <p className="text-xl text-gray-800 leading-relaxed">{brand.missionStatement}</p>
          </div>

          <div className="border-l-4 pl-8 mb-16" style={{ borderColor: primaryColour }}>
            <h2 className="text-sm font-semibold text-gray-400 uppercase tracking-widest mb-4">Brand Story</h2>
            <p className="text-lg text-gray-700 leading-relaxed">{brand.brandStory}</p>
          </div>

          <div className="border-l-4 border-gray-300 pl-8">
            <h2 className="text-sm font-semibold text-gray-400 uppercase tracking-widest mb-4">Elevator Pitch</h2>
            <p className="text-lg text-gray-600 leading-relaxed italic">{brand.elevatorPitch}</p>
          </div>
        </section>

        {/* Page 4: Logo */}
        <section className="h-[297mm] w-[210mm] mx-auto bg-white p-16">
          <h2 className="text-4xl font-light text-gray-900 mb-2">OUR LOGO.</h2>
          <p className="text-gray-500 mb-12 max-w-md">
            The emblem, the icon, the embossing. The logo is the lynchpin of the brand.
            It must be treated with great care and recognised by those who utilise it.
          </p>

          <div className="grid grid-cols-2 gap-8">
            {/* Main logo on white */}
            <div className="flex flex-col items-center p-8 border border-gray-200 rounded-xl">
              {brand.logoImageUrl ? (
                <img src={brand.logoImageUrl} alt="Logo" className="w-32 h-32 object-contain mb-4" />
              ) : (
                <div className="w-32 h-32 bg-gray-100 rounded-lg flex items-center justify-center mb-4">
                  <span className="text-gray-400 text-sm">Logo</span>
                </div>
              )}
              <p className="text-sm text-gray-500">Primary Logo</p>
            </div>

            {/* Logo on dark */}
            <div
              className="flex flex-col items-center p-8 rounded-xl"
              style={{ backgroundColor: primaryColour }}
            >
              {brand.logoImageUrl ? (
                <div className="w-32 h-32 mb-4 flex items-center justify-center bg-white/10 rounded-lg p-2">
                  <img src={brand.logoImageUrl} alt="Logo" className="w-full h-full object-contain" style={{ filter: 'brightness(0) invert(1)' }} />
                </div>
              ) : (
                <div className="w-32 h-32 bg-white/20 rounded-lg flex items-center justify-center mb-4">
                  <span className="text-white/60 text-sm">Logo</span>
                </div>
              )}
              <p className="text-sm text-white/70">White Out Logo</p>
            </div>
          </div>

          {/* Clear space diagram */}
          <div className="mt-12">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">Placement Guidelines</h3>
            <p className="text-sm text-gray-500 max-w-lg">
              The space around the logo should be kept clear. The clearance area depends on
              the size of the logo itself. No items or text should be placed in the clear zone.
            </p>
          </div>
        </section>

        {/* Page 5: Colours */}
        <section className="h-[297mm] w-[210mm] mx-auto bg-white p-16">
          <h2 className="text-4xl font-light text-gray-900 mb-2">COLOURS.</h2>
          <p className="text-gray-500 mb-8 max-w-md">
            The colours are as much an integral part of the brand as the imagery or the logo.
            Every care should be taken to get them right.
          </p>

          <div className="grid grid-cols-3 gap-6">
            {brand.colourPalette?.map((colour) => (
              <div key={colour.hex} className="space-y-3">
                <div
                  className="w-full aspect-square rounded-xl shadow-lg"
                  style={{ backgroundColor: colour.hex }}
                />
                <div>
                  <p className="font-semibold text-gray-800">{colour.name}</p>
                  <p className="text-xs text-gray-400 uppercase tracking-wider">{colour.role}</p>
                </div>
                <div className="text-xs text-gray-500 space-y-0.5">
                  <p><span className="font-medium">HEX:</span> {colour.hex}</p>
                  <p><span className="font-medium">RGB:</span> {hexToRgb(colour.hex)}</p>
                </div>
                <p className="text-xs text-gray-400 italic">{colour.rationale}</p>
              </div>
            ))}
          </div>
        </section>

        {/* Page 6: Typography */}
        <section className="h-[297mm] w-[210mm] mx-auto bg-white p-16">
          <h2 className="text-4xl font-light text-gray-900 mb-2">TYPEFACE.</h2>
          <p className="text-gray-500 mb-12 max-w-md">
            The typography is a key part of your brand. Our typeface makes up all the body copy
            and writing that goes in publications. Sticking to this font means everything looks uniform.
          </p>

          <div className="grid grid-cols-2 gap-12">
            {/* Heading font */}
            <div>
              <div className="border-l-4 pl-6 mb-6" style={{ borderColor: primaryColour }}>
                <p className="text-xs text-gray-400 uppercase tracking-widest mb-2">Heading Font</p>
                <p className="text-3xl font-bold text-gray-900">{brand.typography?.primaryFont}</p>
              </div>
              <p className="text-sm text-gray-500 mb-4">{brand.typography?.primaryUsage}</p>
              <div className="space-y-2">
                <p className="text-2xl text-gray-800">ABCDEFGHIJKLMNOPQRSTUVWXYZ</p>
                <p className="text-2xl text-gray-800">abcdefghijklmnopqrstuvwxyz</p>
                <p className="text-2xl text-gray-800">0123456789!@#$%</p>
              </div>
            </div>

            {/* Body font */}
            <div>
              <div className="border-l-4 border-gray-300 pl-6 mb-6">
                <p className="text-xs text-gray-400 uppercase tracking-widest mb-2">Body Font</p>
                <p className="text-3xl text-gray-900">{brand.typography?.secondaryFont}</p>
              </div>
              <p className="text-sm text-gray-500 mb-4">{brand.typography?.secondaryUsage}</p>
              <div className="space-y-2 text-gray-600">
                <p>ABCDEFGHIJKLMNOPQRSTUVWXYZ</p>
                <p>abcdefghijklmnopqrstuvwxyz</p>
                <p>0123456789!@#$%</p>
              </div>
            </div>
          </div>
        </section>

        {/* Page 7: Tone of Voice */}
        <section className="h-[297mm] w-[210mm] mx-auto p-16" style={{ backgroundColor: primaryColour }}>
          <h2 className="text-4xl font-light text-white mb-8">TONE OF VOICE.</h2>

          <div className="bg-white/10 backdrop-blur rounded-2xl p-8 mb-8">
            <p className="text-white/90 leading-relaxed whitespace-pre-line">{brand.toneGuide}</p>
          </div>

          <div className="mt-auto pt-16 text-center">
            <p className="text-white/50 text-sm">
              Generated by Genesis Brands • {year}
            </p>
          </div>
        </section>

      </div>
    </div>
  )
}

function hexToRgb(hex: string): string {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  if (!result) return 'N/A'
  return `${parseInt(result[1], 16)}, ${parseInt(result[2], 16)}, ${parseInt(result[3], 16)}`
}
