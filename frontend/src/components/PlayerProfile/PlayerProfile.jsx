import './PlayerProfile.css'

const fmt = (value, suffix = '') => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-'
  return `${Number(value).toFixed(1)}${suffix}`
}

function PlayerProfile({ basic, recent, name }) {
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
                  alt="승률"
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
            <span className="player-profile-tag">#칭호 : {basic.title_name || 'NO TITLE'}</span>
            <span className="player-profile-tag">#CLAN : {basic.clan_name || 'NO CLAN'}</span>
            <span className="player-profile-tag">#매너지수 : {basic.manner_grade || '-'}</span>
          </div>
        </div>

        <div className="player-profile-date">
          Day of First Shot : {basic.user_date_create?.slice(0, 10) || 'YYYY-MM-DD'}
        </div>
      </div>
    </section>
  )
}

export default PlayerProfile