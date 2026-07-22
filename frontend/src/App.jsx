import { BrowserRouter, Navigate, Routes, Route, useLocation, useParams } from 'react-router-dom'
import HomePage from './pages/HomePage/HomePage'
import PlayerPage from './pages/PlayerPage/PlayerPage'
import MatchDetailPage from './pages/MatchDetailPage/MatchDetailPage'
import WeaponsPage from './pages/WeaponsPage/WeaponsPage'
import RankingPage from './pages/RankingPage/RankingPage'
import ClanPage from './pages/ClanPage/ClanPage'
import ComparePage from './pages/ComparePage/ComparePage'
import RecordRoomPage from './pages/RecordRoomPage/RecordRoomPage'
import AuthPage from './pages/AuthPage/AuthPage'
import MyPage from './pages/MyPage/MyPage'
import PrivacyPage from './pages/PrivacyPage/PrivacyPage'
import BoardPage from './pages/BoardPage/BoardPage'
import BoardWritePage from './pages/BoardPage/BoardWritePage'
import BoardAdminPage from './pages/BoardPage/BoardAdminPage'
import BoardDetailPage from './pages/BoardPage/BoardDetailPage'
import { readAuthSession } from './utils/authSession'

function RequireAuth({ children }) {
  const location = useLocation()

  if (!readAuthSession()) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return children
}

function BoardPageRoute() {
  const { type } = useParams()
  const page = <BoardPage />
  return type === 'support' ? <RequireAuth>{page}</RequireAuth> : page
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/player" element={<RecordRoomPage />} />
        <Route path="/player/:name" element={<PlayerPage />} />
        <Route path="/weapons" element={<WeaponsPage />} />
        <Route path="/weapons/:name" element={<WeaponsPage />} />
        <Route path="/ranking" element={<RankingPage />} />
        <Route path="/ranking/:name" element={<RankingPage />} />
        <Route path="/clan" element={<RequireAuth><ClanPage /></RequireAuth>} />
        <Route path="/clan/:name" element={<RequireAuth><ClanPage /></RequireAuth>} />
        <Route path="/login" element={<AuthPage />} />
        <Route path="/mypage" element={<RequireAuth><MyPage /></RequireAuth>} />
        <Route path="/compare" element={<ComparePage />} />
        <Route path="/match" element={<MatchDetailPage />} />
        <Route path="/match/:matchId" element={<MatchDetailPage />} />
        <Route path="/privacy" element={<PrivacyPage />} />
        <Route path="/board" element={<Navigate to="/board/free" replace />} />
        <Route path="/board/write" element={<RequireAuth><BoardWritePage /></RequireAuth>} />
        <Route path="/board/admin" element={<RequireAuth><BoardAdminPage /></RequireAuth>} />
        <Route path="/board/post/:id" element={<BoardDetailPage />} />
        <Route path="/board/:type" element={<BoardPageRoute />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
