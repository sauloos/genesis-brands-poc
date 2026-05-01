const BASE = '/api'

async function req<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  })
  if (!res.ok) throw new Error(`API error ${res.status}: ${path}`)
  if (res.status === 204) return undefined as T
  return res.json()
}

// ── Brand ─────────────────────────────────────────────────────────────────────

export interface BrandResponse {
  id: string
  status: 'DRAFT' | 'GENERATING' | 'READY' | 'FAILED'
  createdAt: string
}

export interface StatusResponse {
  id: string
  status: 'DRAFT' | 'GENERATING' | 'READY' | 'FAILED'
  error?: string
}

export interface BrandOutput {
  businessDescription: string
  targetAudience: string
  personality: string
  toneOfVoice: string
  differentiators: string[]
  tagline: string
  missionStatement: string
  brandStory: string
  elevatorPitch: string
  toneGuide: string
  colourPalette: { hex: string; name: string; role: string; rationale: string }[]
  typography: { primaryFont: string; primaryUsage: string; secondaryFont: string; secondaryUsage: string }
  logoImageUrl: string
}

export const api = {
  createBrand: () =>
    req<BrandResponse>('/brands', { method: 'POST' }),

  getBrandStatus: (id: string) =>
    req<StatusResponse>(`/brands/${id}/status`),

  getBrandOutput: (id: string) =>
    req<BrandOutput>(`/brands/${id}/output`),

  triggerGeneration: (id: string) =>
    req<void>(`/brands/${id}/generate`, { method: 'POST' }),

  // ── Conversation ─────────────────────────────────────────────────────────

  startConversation: (brandId: string) =>
    req<{ sessionId: string; firstQuestion: string }>('/conversation/start', {
      method: 'POST',
      body: JSON.stringify({ brandId })
    }),

  submitAnswer: (sessionId: string, answer: string) =>
    req<{ nextQuestion: string | null; complete: boolean; brandId: string | null }>(
      `/conversation/${sessionId}/answer`,
      { method: 'POST', body: JSON.stringify({ answer }) }
    ),

  getSignals: (sessionId: string) =>
    req<Partial<BrandOutput>>(`/conversation/${sessionId}/signals`)
}
