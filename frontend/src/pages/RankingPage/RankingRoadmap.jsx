import { formatRankDisplayName } from '../../utils/rankDisplayName'
import './RankingRoadmap.css'

const FALLBACK_IMAGE = '/sa-assets/sa-grade-sample.png'

function RankingRoadmap({ data }) {
  return (
    <section className="record-section ranking-roadmap-section">
      <div className="record-section-header">
        <h2 className="record-section-title">RANKING ROAD</h2>
        <span className="record-section-sub">현재 위치와 다음 단계</span>
      </div>

      <div className="ranking-roadmap-list">
        <RoadmapCard
          eyebrow="ACCOUNT GRADE"
          title="통합 계급"
          description="전체 통합 계급 중 현재 위치"
        >
          <ProgressionLane
            progression={data.gradeProgression}
            currentExperience={data.gradeExp}
            currentMeta={`경험치 ${formatNumber(data.gradeExp)} · 전체 ${formatRanking(data.gradeRanking)}`}
          />
        </RoadmapCard>

        <RoadmapCard
          eyebrow="SEASON GRADE"
          title="시즌 계급"
          description="현재 시즌 계급의 성장 위치"
        >
          <ProgressionLane
            progression={data.seasonGradeProgression}
            currentExperience={data.seasonGradeExp}
            currentMeta={`경험치 ${formatNumber(data.seasonGradeExp)} · 전체 ${formatRanking(data.seasonGradeRanking)}`}
          />
        </RoadmapCard>

        <RoadmapCard
          eyebrow="RANK MATCH"
          title="랭크전 티어"
          description="솔로와 파티 랭크전의 현재 티어"
          className="is-tier-road"
        >
          <ProgressionLane
            progression={data.soloTierProgression}
            currentMeta={`점수 ${formatNumber(data.soloRankMatchScore)}`}
            laneLabel="SOLO"
          />
          <ProgressionLane
            progression={data.partyTierProgression}
            currentMeta={`점수 ${formatNumber(data.partyRankMatchScore)}`}
            laneLabel="PARTY"
          />
        </RoadmapCard>
      </div>
    </section>
  )
}

function RoadmapCard({ eyebrow, title, description, className = '', children }) {
  return (
    <article className={`ranking-roadmap-card ${className}`.trim()}>
      <header className="ranking-roadmap-card-head">
        <div>
          <span>{eyebrow}</span>
          <h3>{title}</h3>
        </div>
        <p>{description}</p>
      </header>
      <div className="ranking-roadmap-lanes">{children}</div>
    </article>
  )
}

function ProgressionLane({ progression, currentMeta, currentExperience, laneLabel }) {
  const hasCurrent = Number(progression?.currentIndex) >= 0
  const position = getPosition(progression)
  const currentName = formatRankDisplayName(progression?.currentName) || '정보 없음'
  const nextName = formatRankDisplayName(progression?.nextName) || (hasCurrent ? '최고 단계' : '정보 없음')
  const experienceDetail = getExperienceDetail(progression, currentExperience)
  const nextRequirement = getNextRequirement(progression, currentExperience)

  return (
    <div className={`ranking-roadmap-lane${laneLabel ? ' has-lane-label' : ''}`}>
      {laneLabel && <strong className="ranking-roadmap-lane-label">{laneLabel}</strong>}

      <div className="ranking-roadmap-track-layout">
        <Endpoint progression={progression} side="minimum" />

        <div className="ranking-roadmap-track-wrap">
          <div className="ranking-roadmap-track" aria-label={`${currentName}의 현재 위치`}>
            <i className="ranking-roadmap-progress" style={{ width: `${position}%` }} />
            {hasCurrent && (
              <button
                className="ranking-roadmap-marker"
                style={{ '--marker-position': `${position}%` }}
                type="button"
                aria-label={`현재 ${currentName}, 다음 단계 ${nextName}`}
              >
                <img src={progression.currentImage || FALLBACK_IMAGE} alt="" />
                <span className="ranking-roadmap-tooltip" role="tooltip">
                  <b>현재 {currentName}</b>
                  <small>{currentMeta}</small>
                  <em>다음 단계 · {nextName}</em>
                </span>
              </button>
            )}
          </div>
          <div className="ranking-roadmap-position-copy">
            <span>{hasCurrent ? `STEP ${progression.currentIndex + 1} / ${progression.totalCount}` : '-'}</span>
          </div>
        </div>

        <Endpoint progression={progression} side="maximum" />
      </div>

      <div className="ranking-roadmap-status">
        <div className="ranking-roadmap-current">
          <span>CURRENT</span>
          <img src={progression?.currentImage || FALLBACK_IMAGE} alt="" />
          <div>
            <strong>{currentName}</strong>
            <small>{currentMeta}</small>
            {experienceDetail && (
              <div className="ranking-roadmap-experience">
                <div>
                  <span>현재 구간</span>
                  <b>{experienceDetail.rangeLabel}</b>
                </div>
                <i>
                  <span style={{ width: `${experienceDetail.progress}%` }} />
                </i>
                <em>구간 진행률 {experienceDetail.progress.toFixed(1)}%</em>
              </div>
            )}
          </div>
        </div>

        <div className="ranking-roadmap-next">
          <span>NEXT</span>
          {progression?.nextImage && <img src={progression.nextImage} alt="" />}
          <div>
            <strong>{nextName}</strong>
            {nextRequirement && <small>{nextRequirement}</small>}
          </div>
        </div>
      </div>
    </div>
  )
}

function Endpoint({ progression, side }) {
  const isMinimum = side === 'minimum'
  const name = formatRankDisplayName(isMinimum ? progression?.minimumName : progression?.maximumName)
  const image = isMinimum ? progression?.minimumImage : progression?.maximumImage

  return (
    <div className={`ranking-roadmap-endpoint is-${side}`}>
      <span>{isMinimum ? 'MIN' : 'MAX'}</span>
      <img src={image || FALLBACK_IMAGE} alt="" />
      <strong>{name || '-'}</strong>
    </div>
  )
}

function getPosition(progression) {
  const currentIndex = Number(progression?.currentIndex)
  const totalCount = Number(progression?.totalCount)
  if (!Number.isFinite(currentIndex) || currentIndex < 0 || totalCount <= 1) return 0
  return Math.min(Math.max((currentIndex / (totalCount - 1)) * 100, 0), 100)
}

function getExperienceDetail(progression, currentExperience) {
  if (
    currentExperience === null ||
    currentExperience === undefined ||
    progression?.currentMinimumExperience === null ||
    progression?.currentMinimumExperience === undefined ||
    progression?.currentMaximumExperience === null ||
    progression?.currentMaximumExperience === undefined
  ) {
    return null
  }

  const current = Number(currentExperience)
  const minimum = Number(progression?.currentMinimumExperience)
  const maximum = Number(progression?.currentMaximumExperience)

  if (![current, minimum, maximum].every(Number.isFinite) || maximum <= minimum) return null

  const progress = Math.min(Math.max(((current - minimum) / (maximum - minimum)) * 100, 0), 100)
  return {
    progress,
    rangeLabel: `${formatNumber(minimum)} ~ ${formatNumber(maximum)} EXP`,
  }
}

function getNextRequirement(progression, currentExperience) {
  if (!progression?.nextName) return null

  const hasNextExperience =
    progression.nextMinimumExperience !== null &&
    progression.nextMinimumExperience !== undefined &&
    currentExperience !== null &&
    currentExperience !== undefined
  const nextExperience = Number(progression.nextMinimumExperience)
  const current = Number(currentExperience)
  if (hasNextExperience && Number.isFinite(nextExperience) && Number.isFinite(current)) {
    const remaining = Math.max(nextExperience - current, 0)
    return `다음 단계까지 ${formatNumber(remaining)} EXP`
  }

  const hasRankingTarget =
    progression.nextWorstRanking !== null && progression.nextWorstRanking !== undefined
  const rankingTarget = Number(progression.nextWorstRanking)
  if (hasRankingTarget && Number.isFinite(rankingTarget)) {
    return `진입 조건 · 랭킹 ${formatNumber(rankingTarget)}위 이내`
  }

  return null
}

function formatNumber(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number.toLocaleString() : '-'
}

function formatRanking(value) {
  const formatted = formatNumber(value)
  return formatted === '-' ? '-' : `${formatted}위`
}

export default RankingRoadmap
