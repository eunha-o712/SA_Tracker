import './CombatSummary.css'

function CombatSummary({ userName }) {
  return (
    <section className="record-section">
      <div className="record-section-header">
        <h2 className="record-section-title">COMBAT Summ.</h2>
        <span className="record-section-sub">AI 요약</span>
      </div>

      <div className="combat-summary-box">
        <strong>{userName}</strong>님의 최근 전투 기록을 분석한 한 줄 총평이 이곳에 표시됩니다.
      </div>
    </section>
  )
}

export default CombatSummary