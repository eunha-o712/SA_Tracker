import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import './MatchDetailPage.css'

function MatchDetailPage() {
  return (
    <div className="match-detail-shell">
      <Header />

      <main className="match-detail-page">
        <div className="match-detail-container">
          <div className="match-detail-card">
            <h1 className="match-detail-title">Match Detail</h1>
            <p className="match-detail-desc">
              매치 상세 화면은 다음 단계에서 연결합니다.
            </p>
          </div>
        </div>
      </main>

      <Footer />
    </div>
  )
}

export default MatchDetailPage
