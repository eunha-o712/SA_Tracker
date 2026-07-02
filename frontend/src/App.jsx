import { BrowserRouter, Routes, Route } from 'react-router-dom'
import HomePage from './pages/HomePage/HomePage'
import PlayerPage from './pages/PlayerPage/PlayerPage'
import MatchDetailPage from './pages/MatchDetailPage/MatchDetailPage'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/player/:name" element={<PlayerPage />} />
        <Route path="/match" element={<MatchDetailPage />} />
        <Route path="/match/:matchId" element={<MatchDetailPage />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App