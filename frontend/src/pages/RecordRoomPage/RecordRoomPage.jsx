import { Navigate } from 'react-router-dom'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import NavBar from '../../components/NavBar/NavBar'
import SearchBar from '../../components/SearchBar/SearchBar'
import { readAuthSession } from '../../utils/authSession'
import '../PlayerPage/PlayerPage.css'
import './RecordRoomPage.css'

function RecordRoomPage() {
  const session = readAuthSession()
  const accountName = session?.user?.suddenNickname || session?.user?.displayName || ''

  if (accountName) {
    return <Navigate to={`/player/${encodeURIComponent(accountName)}`} replace />
  }

  return (
    <div className="player-shell">
      <Header />
      <NavBar />

      <main className="player-page record-room-page">
        <div className="player-container banner-content-layout">
          <div className="record-banner">
            <img src="/sa-assets/banner-preview/no-outer-frame/sa-recordroom-banner-no-outer-frame.png" alt="SA Record Room" />
          </div>

          <div className="player-compact-search" aria-label="플레이어 검색">
            <SearchBar compact />
          </div>

          <section className="record-section record-room-search-section">
            <div className="record-section-header">
              <h1 className="record-section-title">PLAYER PROFILE</h1>
              <span className="record-section-sub">레코드 룸</span>
            </div>
            <div className="record-room-empty-body">
              <span>RECORD ROOM</span>
              <strong>닉네임으로 기록을 조회하세요.</strong>
              <p>대상의 기본 정보를 알 수 있습니다.</p>
            </div>
          </section>
        </div>
      </main>

      <Footer />
    </div>
  )
}

export default RecordRoomPage
