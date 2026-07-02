import './MatchForm.css'

const FORM_CARDS = [
  { key: 'RECENT', label: '최근 매치' },
  { key: 'CLAN', label: '클랜전' },
  { key: 'RANKED', label: '랭크전' },
  { key: 'GENERAL', label: '일반전' },
]

const RESULT_CELL_COUNT = 20

function MatchForm({ summaries = [], loading = false, error = '' }) {
  const summaryByKey = new Map(
    summaries.map((summary) => [summary.key, summary])
  )

  return (
    <section className="record-section match-form-section">
      <div className="record-section-header">
        <h2 className="record-section-title">MATCH FORM</h2>
        <span className="record-section-sub">최근 전투 흐름</span>
      </div>

      <div className="match-form-grid" aria-live="polite">
        {FORM_CARDS.map((card) => {
          const summary = summaryByKey.get(card.key)
          const results = Array.isArray(summary?.recentResults)
            ? summary.recentResults
            : []
          const matchCount = Number(summary?.matchCount) || 0
          const hasResults = !loading && !error && matchCount > 0

          return (
            <article className="match-form-card" key={card.key}>
              <div className="match-form-card-head">
                <strong>{card.label}</strong>
                <span>{hasResults ? `${formatRate(summary.winRate)}%` : '-'}</span>
              </div>

              <div className="match-form-card-sub">
                {getFormMessage(loading, error, matchCount)}
              </div>

              <div className="match-form-results">
                {Array.from({ length: RESULT_CELL_COUNT }, (_, index) => {
                  const result = results[index] || ''

                  return (
                    <span
                      className={`match-form-result ${getResultClass(result)}`}
                      key={`${card.key}-${index}`}
                      title={result ? `${index + 1}번째 최근 경기: ${getResultName(result)}` : ''}
                      aria-label={result ? `${index + 1}번째 최근 경기 ${getResultName(result)}` : undefined}
                      aria-hidden={result ? undefined : true}
                    >
                      {result || '-'}
                    </span>
                  )
                })}
              </div>

              <div className="match-form-counts">
                <span className="win">{Number(summary?.winCount) || 0}W</span>
                <span className="draw">{Number(summary?.drawCount) || 0}D</span>
                <span className="lose">{Number(summary?.loseCount) || 0}L</span>
              </div>
            </article>
          )
        })}
      </div>
    </section>
  )
}

function getFormMessage(loading, error, matchCount) {
  if (loading) return '매치 기록 집계 중'
  if (error) return error
  if (matchCount === 0) return '기록 없음'
  return `최근 ${matchCount}경기 승률`
}

function getResultClass(result) {
  if (result === 'W') return 'win'
  if (result === 'D') return 'draw'
  if (result === 'L') return 'lose'
  return 'empty'
}

function getResultName(result) {
  if (result === 'W') return '승리'
  if (result === 'D') return '무승부'
  if (result === 'L') return '패배'
  return '결과 없음'
}

function formatRate(value) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue.toFixed(1) : '0.0'
}

export default MatchForm
