import './DataRefreshStatus.css'

function DataRefreshStatus({ label = '갱신 중' }) {
  return (
    <div className="data-refresh-status" role="status" aria-live="polite">
      <span className="data-refresh-indicator" aria-hidden="true" />
      <strong>{label}</strong>
    </div>
  )
}

export default DataRefreshStatus
