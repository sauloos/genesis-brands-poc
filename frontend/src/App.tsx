import { Routes, Route, Navigate } from 'react-router-dom'
import StartPage from './pages/StartPage'
import QuestionnairePage from './pages/QuestionnairePage'
import GeneratingPage from './pages/GeneratingPage'
import BrandOutputPage from './pages/BrandOutputPage'
import BrandBookPage from './pages/BrandBookPage'

export default function App() {
  return (
    <div className="min-h-screen flex flex-col">
      <Routes>
        <Route path="/" element={<StartPage />} />
        <Route path="/questionnaire/:sessionId/:brandId" element={<QuestionnairePage />} />
        <Route path="/generating/:brandId" element={<GeneratingPage />} />
        <Route path="/brand/:brandId/output" element={<BrandOutputPage />} />
        <Route path="/brand/:brandId/book" element={<BrandBookPage />} />
        <Route path="/brand/:brandId" element={<Navigate to={`output`} replace />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  )
}
