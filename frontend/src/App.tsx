import { Routes, Route, Navigate } from 'react-router-dom'
import StartPage from './pages/StartPage'
import QuestionnairePage from './pages/QuestionnairePage'
import GeneratingPage from './pages/GeneratingPage'
import BrandOutputPage from './pages/BrandOutputPage'

export default function App() {
  return (
    <div className="min-h-screen flex flex-col">
      <Routes>
        <Route path="/" element={<StartPage />} />
        <Route path="/questionnaire/:sessionId/:brandId" element={<QuestionnairePage />} />
        <Route path="/generating/:brandId" element={<GeneratingPage />} />
        <Route path="/brand/:brandId" element={<BrandOutputPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  )
}
