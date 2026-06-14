import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import './PlayerFavorites.css'

const mockFavorites = [
  {
    id: 1,
    userName: '원장',
    titleName: 'NO TITLE',
    seasonGrade: '원장',
    seasonGradeImage: '/sa-assets/sa-grade-sample.png',
    clanName: '다봄',
    clanMarkImage: '/sa-assets/sa-clan-basic.png',
    lastLogin: '8H AGO',
    recent: true,
    kda: {
      kill: 48.32,
      death: 23.21,
      assist: 11.5,
    },
  },
]

function PlayerFavorites() {
  const navigate = useNavigate()
  const [favorites, setFavorites] = useState(mockFavorites)

  const handleDelete = (id) => {
    setFavorites((cur) => cur.filter((f) => f.id !== id))
  }

  return (
    <section className="player-favorites">
      <div className="player-favorites-header">
        <span>FAVORITE</span>
        <h2>PLAYERS</h2>
      </div>

      {favorites.length === 0 && (
        <div className="player-favorites-message">
          등록된 즐겨찾기가 없습니다.
        </div>
      )}

      {favorites.length > 0 && (
        <div className="player-card-grid">
          {favorites.map((fav) => (
            <article className="favorite-player-card" key={fav.id}>
              <div className="favorite-top">
                <div className="favorite-rank">
                  <img src={fav.seasonGradeImage} alt={fav.seasonGrade} />
                </div>

                <div className="favorite-name">
                  <h3>{fav.userName}</h3>
                  <p>{fav.titleName}</p>
                </div>

                <div className="favorite-login">
                  <span>
                    <i className={fav.recent ? 'active' : ''} />
                    LAST LOGIN
                  </span>
                  <strong className={fav.recent ? 'active' : ''}>
                    {fav.lastLogin}
                  </strong>
                </div>
              </div>

              <div className="favorite-divider" />

              <div className="favorite-clan">
                <div className="favorite-clan-mark">
                  <img src={fav.clanMarkImage} alt={fav.clanName} />
                </div>
                <strong>{fav.clanName}</strong>
              </div>

              <div className="favorite-divider" />

              <div className="favorite-kda">
                <span>K/D/A</span>
                <div>
                  <b>{fav.kda.kill.toFixed(2)}</b>
                  <em>/</em>
                  <b className="death">{fav.kda.death.toFixed(2)}</b>
                  <em>/</em>
                  <b className="assist">{fav.kda.assist.toFixed(2)}</b>
                </div>
              </div>

              <div className="favorite-actions">
                <button
                  type="button"
                  onClick={() =>
                    navigate(`/player/${encodeURIComponent(fav.userName)}`)
                  }
                >
                  VIEW PROFILE
                </button>

                <button
                  type="button"
                  className="remove"
                  onClick={() => handleDelete(fav.id)}
                >
                  REMOVE
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}

export default PlayerFavorites