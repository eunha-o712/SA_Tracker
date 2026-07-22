import './DataRefreshStatus.css'

function DataRefreshStatus() {
  return (
    <div className="data-refresh-status" role="status" aria-live="polite">
      <span className="data-refresh-indicator" aria-hidden="true" />
      <div>
        <strong>갱신중</strong>
        <span>잠시만 기다려주세요.</span>
      </div>
    </div>
  )
}

export default DataRefreshStatus
