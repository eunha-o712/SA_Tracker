import './PlayerProfile.css'

const fmt = (value, suffix = '') => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-'
  return `${Number(value).toFixed(1)}${suffix}`
}

function PlayerProfile({ basic, recent, name, clanName: resolvedClanName, favoriteId, favoritePending, favoriteMessage, onFavoriteToggle, onCompare }) {
  const titleName = cleanProfileText(basic.title_name)
  const clanName = cleanProfileText(resolvedClanName)
  const mannerGrade = cleanProfileText(basic.manner_grade)

  return (
    <section className="record-section">
      <div className="player-profile">
        <div className="player-profile-avatar">
          <img src="/sa-assets/sa-profile-basic.png" alt="profile" />
        </div>

        <div className="player-profile-main">
          <div className="player-profile-top">
            <h1 className="player-profile-name">{basic.user_name || name}</h1>

            <div className="player-profile-metrics">
              <div className="player-profile-metric">
                <img
                  src="/sa-assets/sa-stat-winrate.png"
                  alt="win rate"
                  className="player-profile-metric-icon"
                />
                <span>승률</span>
                <strong>{fmt(recent.recent_win_rate, '%')}</strong>
              </div>

              <div className="player-profile-metric">
                <img
                  src="/sa-assets/sa-stat-kd.png"
                  alt="K/D"
                  className="player-profile-metric-icon"
                />
                <span>K/D</span>
                <strong>{fmt(recent.recent_kill_death_rate, '%')}</strong>
              </div>
            </div>
          </div>

          <div className="player-profile-tags">
            {titleName && <span className="player-profile-tag">#칭호 : {titleName}</span>}
            <span className="player-profile-tag">#클랜 : {clanName || '-'}</span>
            {mannerGrade && <span className="player-profile-tag">#매너지수 : {mannerGrade}</span>}
          </div>
        </div>

        <div className="player-profile-side">
          <div className="player-profile-date">최초 전투일 : {basic.user_date_create?.slice(0, 10) || 'YYYY-MM-DD'}</div>
          <button className={favoriteId ? 'player-favorite-button active' : 'player-favorite-button'} type="button" disabled={favoritePending} onClick={onFavoriteToggle}>
            {favoritePending ? '저장 중' : favoriteId ? '즐겨찾기 완료' : '즐겨찾기 추가'}
          </button>
          <button className="player-compare-button" type="button" onClick={onCompare}>플레이어 비교</button>
          <span className="player-favorite-message" aria-live="polite">{favoriteMessage}</span>
        </div>
      </div>
    </section>
  )
}

function cleanProfileText(value) {
  if (value === null || value === undefined) return ''

  const normalized = String(value).trim()
  if (!normalized) return ''

  const lowered = normalized.toLowerCase()
  if (['-', 'null', 'undefined', 'none', 'no clan', 'no title'].includes(lowered)) {
    return ''
  }

  return normalized
}

export default PlayerProfile
