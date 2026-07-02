import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import NavBar from '../../components/NavBar/NavBar'
import PlayerProfile from '../../components/PlayerProfile/PlayerProfile'
import RankSummary from '../../components/PlayerProfile/RankSummary'
import WeaponSummary from '../../components/PlayerProfile/WeaponSummary'
import MatchSummary from '../../components/PlayerProfile/MatchSummary'
import CombatSummary from '../../components/PlayerProfile/CombatSummary'
import api from '../../api/api'
import './PlayerPage.css'

function PlayerPage() {
  const { name } = useParams()
  const [playerData, setPlayerData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const fetchPlayerPageData = async () => {
      try {
        setLoading(true)
        setError('')

        const response = await api.get('/api/player', {
          params: { userName: name },
        })

        setPlayerData(response.data)
      } catch (err) {
        setError(
          err.response?.status === 429
            ? '조회가 잠시 많아서 잠깐 후 다시 시도해주세요.'
            : '플레이어 정보를 불러오지 못했습니다.'
        )
      } finally {
        setLoading(false)
      }
    }

    fetchPlayerPageData()
  }, [name])

  if (loading) {
    return (
      <div className="player-shell">
        <Header />
        <NavBar />
        <main className="player-page">
          <div className="player-container">
            <p className="player-message">불러오는 중...</p>
          </div>
        </main>
        <Footer />
      </div>
    )
  }

  if (error) {
    return (
      <div className="player-shell">
        <Header />
        <NavBar />
        <main className="player-page">
          <div className="player-container">
            <p className="player-error">{error}</p>
          </div>
        </main>
        <Footer />
      </div>
    )
  }

  const basic = playerData?.basic ?? {}
  const rank = playerData?.rank ?? {}
  const tier = playerData?.tier ?? {}
  const recent = playerData?.recent ?? {}
  const images = playerData?.images ?? {}

  return (
    <div className="player-shell">
      <Header />
      <NavBar />

      <main className="player-page">
        <div className="player-container">
          <div className="record-banner">
            <img src="/sa-assets/sa-recordroom-banner.png" alt="SA Record Room" />
          </div>

          <PlayerProfile basic={basic} recent={recent} name={name} />

          <RankSummary rank={rank} tier={tier} images={images} />

          <WeaponSummary recent={recent} />

          <MatchSummary userName={basic.user_name || name} recent={recent} />

          <CombatSummary userName={basic.user_name || name} />
        </div>
      </main>

      <Footer />
    </div>
  )
}

export default PlayerPage
