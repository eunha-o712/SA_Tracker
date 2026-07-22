import './PlayerPageSkeleton.css'

function PlayerPageSkeleton() {
  return (
    <div
      className="player-skeleton"
      role="status"
      aria-live="polite"
      aria-busy="true"
    >
      <span className="player-skeleton-status">플레이어 전적을 불러오는 중입니다.</span>

      <div className="player-skeleton-banner player-skeleton-shimmer" />

      <section className="record-section player-skeleton-profile">
        <div className="player-skeleton-avatar player-skeleton-shimmer" />
        <div className="player-skeleton-profile-main">
          <div className="player-skeleton-line title player-skeleton-shimmer" />
          <div className="player-skeleton-metrics">
            <div className="player-skeleton-line metric player-skeleton-shimmer" />
            <div className="player-skeleton-line metric player-skeleton-shimmer" />
          </div>
          <div className="player-skeleton-tags">
            <div className="player-skeleton-line tag player-skeleton-shimmer" />
            <div className="player-skeleton-line tag player-skeleton-shimmer" />
            <div className="player-skeleton-line tag short player-skeleton-shimmer" />
          </div>
        </div>
        <div className="player-skeleton-line date player-skeleton-shimmer" />
      </section>

      <SkeletonSection titleWidth="190px" cardCount={4} layout="rank" />
      <SkeletonSection titleWidth="230px" cardCount={3} layout="weapon" />
    </div>
  )
}

function SkeletonSection({ titleWidth, cardCount, layout }) {
  return (
    <section className="record-section player-skeleton-section">
      <div className="record-section-header">
        <div
          className="player-skeleton-heading player-skeleton-shimmer"
          style={{ width: titleWidth }}
        />
        <div className="player-skeleton-heading-sub player-skeleton-shimmer" />
      </div>

      <div className={`player-skeleton-card-grid ${layout}`}>
        {Array.from({ length: cardCount }, (_, index) => (
          <div className="player-skeleton-card" key={index}>
            <div className="player-skeleton-line card-label player-skeleton-shimmer" />
            <div className="player-skeleton-card-icon player-skeleton-shimmer" />
            <div className="player-skeleton-card-copy">
              <div className="player-skeleton-line card-value player-skeleton-shimmer" />
              <div className="player-skeleton-line card-sub player-skeleton-shimmer" />
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}

export default PlayerPageSkeleton
