import './LoadingIntro.css'

function LoadingIntro({ canSkip, onEnded, onSkip }) {
  return (
    <div className="loading-intro" role="status" aria-label="서비스를 준비하고 있습니다.">
      <video
        className="loading-video"
        autoPlay
        muted
        playsInline
        aria-hidden="true"
        onEnded={onEnded}
        onError={onEnded}
      >
        <source src="/video/loading.mp4" type="video/mp4" />
      </video>
      {canSkip && (
        <button className="loading-intro__skip" type="button" onClick={onSkip}>
          바로 시작하기
        </button>
      )}
    </div>
  )
}

export default LoadingIntro
