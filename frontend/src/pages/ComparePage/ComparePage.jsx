import { useSearchParams } from 'react-router-dom'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import NavBar from '../../components/NavBar/NavBar'
import '../PlayerPage/PlayerPage.css'
import ComparePanel from './ComparePanel'

function ComparePage() {
  const [params] = useSearchParams()
  const left = params.get('left')?.trim() ?? ''
  const right = params.get('right')?.trim() ?? ''

  return (
    <div className="player-shell">
      <Header />
      <NavBar />
      <main className="player-page">
        <div className="player-container compare-container banner-content-layout">
          <div className="record-banner compare-banner">
            <img src="/sa-assets/banner-preview/no-outer-frame/sa-recordroom-banner-no-outer-frame.png" alt="Player Comparison" />
          </div>
          <ComparePanel key={`${left}|${right}`} initialLeft={left} initialRight={right} syncUrl />
        </div>
      </main>
      <Footer />
    </div>
  )
}

export default ComparePage
