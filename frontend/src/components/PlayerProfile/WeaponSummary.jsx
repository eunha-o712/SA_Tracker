import './WeaponSummary.css'

const fmt = (value, suffix = '') => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-'
  return `${Number(value).toFixed(1)}${suffix}`
}

function WeaponSummary({ recent }) {
  return (
    <section className="record-section">
      <div className="record-section-header">
        <h2 className="record-section-title">WEAPON K/D</h2>
        <span className="record-section-sub">무기동향</span>
      </div>

      <div className="weapon-summary-grid">

        {/* 돌격 */}
        <article className="weapon-summary-card">
          <div className="weapon-summary-top">
            <div>
              <span className="weapon-summary-label">돌격</span>

              <div className="weapon-summary-value">
                {fmt(recent.recent_assault_rate, '%')}
              </div>
            </div>

            <img
              src="/sa-assets/sa-recordroom-rifle.png"
              alt="rifle"
              className="weapon-summary-image weapon-rifle"
            />
          </div>

          <div className="weapon-summary-progress">
            <span
              style={{
                width: `${recent.recent_assault_rate || 0}%`
              }}
            />
          </div>
        </article>

        {/* 저격 */}
        <article className="weapon-summary-card">
          <div className="weapon-summary-top">
            <div>
              <span className="weapon-summary-label">저격</span>

              <div className="weapon-summary-value">
                {fmt(recent.recent_sniper_rate, '%')}
              </div>
            </div>

            <img
              src="/sa-assets/sa-recordroom-sniper.png"
              alt="sniper"
              className="weapon-summary-image weapon-sniper"
            />
          </div>

          <div className="weapon-summary-progress">
            <span
              style={{
                width: `${recent.recent_sniper_rate || 0}%`
              }}
            />
          </div>
        </article>

        {/* 특수 */}
        <article className="weapon-summary-card">
          <div className="weapon-summary-top">
            <div>
              <span className="weapon-summary-label">특수</span>

              <div className="weapon-summary-value">
                {fmt(recent.recent_special_rate, '%')}
              </div>
            </div>

            <img
              src="/sa-assets/sa-recordroom-sg870.png"
              alt="special"
              className="weapon-summary-image weapon-shotgun"
            />
          </div>

          <div className="weapon-summary-progress">
            <span
              style={{
                width: `${recent.recent_special_rate || 0}%`
              }}
            />
          </div>
        </article>

      </div>
    </section>
  )
}

export default WeaponSummary