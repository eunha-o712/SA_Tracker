import './MatchCard.css'

const ASSET = '/sa-assets/'

function MatchCard({
  match,
  selected,
  onToggle,
}) {
  const result = normalizeResult(
    match.match_result,
  )

  const badge = getBadge(result)

  return (
    <article
      className={
        selected
          ? 'match-card selected'
          : 'match-card'
      }
    >
      <button
        className="match-card-main"
        type="button"
        onClick={() => {
          onToggle(match.match_id)
        }}
        aria-expanded={selected}
      >
        <div className="match-card-badge">
          {badge ? (
            <img
              src={`${ASSET}${badge}`}
              alt={getResultLabel(result)}
            />
          ) : (
            <span className="match-card-result-unknown">
              N/A
            </span>
          )}
        </div>

        <div className="match-card-info">
          <strong>
            {match.match_mode || 'UNKNOWN MODE'}
          </strong>

          <span>
            {match.match_type || 'TACTICAL MATCH'}
          </span>
        </div>

        <div className="match-card-stats">
          <Stat
            label="K"
            value={match.kill}
          />

          <Stat
            label="D"
            value={match.death}
            danger
          />

          <Stat
            label="A"
            value={match.assist}
          />
        </div>

        <div className="match-card-date">
          <img
            src={`${ASSET}sa-icon-date.png`}
            alt=""
          />

          <span>
            {formatDate(match.date_match)}
          </span>
        </div>

        <div className="match-card-detail">
          <img
            src={`${ASSET}sa-icon-detail.png`}
            alt=""
          />
        </div>
      </button>
    </article>
  )
}

function Stat({
  label,
  value,
  danger = false,
}) {
  return (
    <span
      className={
        danger
          ? 'match-card-stat danger'
          : 'match-card-stat'
      }
    >
      <em>{label}</em>
      <strong>{value ?? '-'}</strong>
    </span>
  )
}

function normalizeResult(result) {
  const value = String(result ?? '')
    .trim()
    .toUpperCase()

  if (
    value === '1' ||
    value === 'WIN' ||
    value === '승리' ||
    value === '승'
  ) {
    return 'WIN'
  }

  if (
    value === '2' ||
    value === 'LOSE' ||
    value === '패배' ||
    value === '패'
  ) {
    return 'LOSE'
  }

  if (
    value === '3' ||
    value === 'DRAW' ||
    value === '무승부' ||
    value === '무'
  ) {
    return 'DRAW'
  }

  return 'UNKNOWN'
}

function getBadge(result) {
  if (result === 'WIN') {
    return 'sa-badge-win.png'
  }

  if (result === 'LOSE') {
    return 'sa-badge-lose.png'
  }

  if (result === 'DRAW') {
    return 'sa-badge-draw.png'
  }

  return null
}

function getResultLabel(result) {
  if (result === 'WIN') return '승리'
  if (result === 'LOSE') return '패배'
  if (result === 'DRAW') return '무승부'

  return '결과 없음'
}

function formatDate(value) {
  if (!value) return '-'

  const utcDate = new Date(value)

  if (Number.isNaN(utcDate.getTime())) {
    return String(value)
      .replace('T', ' ')
      .slice(0, 16)
  }

  return new Intl.DateTimeFormat('ko-KR', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
    .format(utcDate)
    .replace(/\. /g, '-')
    .replace('.', '')
}

export default MatchCard