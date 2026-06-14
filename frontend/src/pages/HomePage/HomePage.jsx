import { useState } from 'react'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import LoadingIntro from '../../components/LoadingIntro/LoadingIntro'
import NavBar from '../../components/NavBar/NavBar'
import SearchBar from '../../components/SearchBar/SearchBar'
import PlayerFavorites from '../../components/Favorites/PlayerFavorites'
import './HomePage.css'

function HomePage() {
  const [showIntro, setShowIntro] = useState(() => !sessionStorage.getItem('hasSeenIntro'))

  const handleIntroEnd = () => {
    sessionStorage.setItem('hasSeenIntro', 'true')
    setShowIntro(false)
  }

  if (showIntro) {
    return <LoadingIntro onEnded={handleIntroEnd} />
  }

  return (
    <div className="home-shell">
      <Header />

      <NavBar />

      <main className="home-page">
        <div className="home-hero-wrap">
          <img
            src="/sa-assets/sa-hero-header.png"
            alt="SA-Tracker hero"
            className="home-hero"
          />

          <div className="home-search-overlay">
            <SearchBar />
          </div>
        </div>
      </main>
      <PlayerFavorites />
      <Footer />
    </div>
  )
}

export default HomePage
