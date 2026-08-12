import { useCallback, useEffect, useRef, useState } from 'react'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import LoadingIntro from '../../components/LoadingIntro/LoadingIntro'
import NavBar from '../../components/NavBar/NavBar'
import SearchBar from '../../components/SearchBar/SearchBar'
import PlayerFavorites from '../../components/Favorites/PlayerFavorites'
import './HomePage.css'

function HomePage() {
  const [showIntro, setShowIntro] = useState(() => (
    !window.matchMedia('(prefers-reduced-motion: reduce)').matches
    && !sessionStorage.getItem('hasSeenIntro')
  ))
  const [canSkipIntro, setCanSkipIntro] = useState(false)
  const introReady = useRef({ minimum: false, data: false })

  const dismissIntro = useCallback(() => {
    sessionStorage.setItem('hasSeenIntro', 'true')
    setShowIntro(false)
  }, [])

  const completeIntro = useCallback(() => {
    if (!introReady.current.minimum || !introReady.current.data) return
    dismissIntro()
  }, [dismissIntro])

  useEffect(() => {
    if (!showIntro) return undefined

    let active = true
    introReady.current.data = true
    const minimumTimer = window.setTimeout(() => {
      if (!active) return
      introReady.current.minimum = true
      setCanSkipIntro(true)
      completeIntro()
    }, 2_500)

    return () => {
      active = false
      window.clearTimeout(minimumTimer)
    }
  }, [completeIntro, showIntro])

  const handleIntroEnd = () => {
    introReady.current.minimum = true
    completeIntro()
  }

  if (showIntro) {
    return (
      <LoadingIntro
        canSkip={canSkipIntro}
        onEnded={handleIntroEnd}
        onSkip={dismissIntro}
      />
    )
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
