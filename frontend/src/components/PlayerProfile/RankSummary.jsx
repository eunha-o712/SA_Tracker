import './RankSummary.css'

function RankSummary({ rank, tier, images }) {
  return (
    <section className="record-section">
      <div className="record-section-header">
        <h2 className="record-section-title">RANK Summ.</h2>
        <span className="record-section-sub">랭킹요약</span>
      </div>

      <div className="rank-summary-grid">

        {/* 통합 계급 */}
        <article className="rank-summary-card">
          <span className="rank-summary-label">통합 계급</span>

          <div className="rank-grade-image-box">
            <img
              src={images.gradeImage || '/sa-assets/sa-grade-sample.png'}
              alt="grade"
              className="rank-grade-image"
            />
          </div>

          <div className="rank-summary-info">
            <strong className="rank-summary-value">
              {rank.grade || '-'}
            </strong>

            <span className="rank-summary-sub">
              전체 {rank.grade_ranking?.toLocaleString() || '-'}위
            </span>
          </div>
        </article>

        {/* 시즌 계급 */}
        <article className="rank-summary-card">
          <span className="rank-summary-label">시즌 계급</span>

          <img
            src={images.seasonGradeImage || '/sa-assets/sa-grade-sample.png'}
            alt="season grade"
            className="rank-season-image"
          />

          <div className="rank-summary-info">
            <strong className="rank-summary-value">
              {rank.season_grade || '-'}
            </strong>

            <span className="rank-summary-sub">
              전체 {rank.season_grade_ranking?.toLocaleString() || '-'}위
            </span>
          </div>
        </article>

        {/* 솔로 랭크전 */}
        <article className="rank-summary-card">
          <span className="rank-summary-label">솔로 랭크전</span>

          <img
            src={images.soloTierImage || '/sa-assets/sa-grade-sample.png'}
            alt="solo tier"
            className="rank-tier-image"
          />

          <div className="rank-summary-info">
            <strong className="rank-summary-value">
              {tier.solo_rank_match_tier || 'UNRANK'}
            </strong>

            <span className="rank-summary-sub">
              점수 {tier.solo_rank_match_score ?? '-'}
            </span>
          </div>
        </article>

        {/* 파티 랭크전 */}
        <article className="rank-summary-card">
          <span className="rank-summary-label">파티 랭크전</span>

          <img
            src={images.partyTierImage || '/sa-assets/sa-grade-sample.png'}
            alt="party tier"
            className="rank-tier-image"
          />

          <div className="rank-summary-info">
            <strong className="rank-summary-value">
              {tier.party_rank_match_tier || 'UNRANK'}
            </strong>

            <span className="rank-summary-sub">
              점수 {tier.party_rank_match_score ?? '-'}
            </span>
          </div>
        </article>

      </div>
    </section>
  )
}

export default RankSummary