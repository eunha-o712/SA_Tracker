import './LoadingIntro.css'

function LoadingIntro({ onEnded }) {
  return (
    <div className="loading-intro">
      <video
        className="loading-video"
        autoPlay
        muted
        playsInline
        onEnded={onEnded}
      >
        <source src="/video/loading.mp4" type="video/mp4" />
      </video>
    </div>
  )
}

export default LoadingIntro