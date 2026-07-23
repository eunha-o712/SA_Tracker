import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import api, { getApiErrorMessage } from '../../api/api'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import NavBar from '../../components/NavBar/NavBar'
import { clearAuthSession, readAuthSession } from '../../utils/authSession'
import BoardPostImage from './BoardPostImage'
import '../PlayerPage/PlayerPage.css'
import './BoardPage.css'

function BoardDetailPage() {
  const { id } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const [post, setPost] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [adminError, setAdminError] = useState('')
  const [pending, setPending] = useState('')
  const [supportForm, setSupportForm] = useState({ status: 'IN_PROGRESS', response: '' })
  const [disputeForm, setDisputeForm] = useState({
    resolution: 'TRANSFER_TO_CLAIMANT',
    accountAction: 'KEEP',
    verifyClaimant: false,
    response: '',
    reason: '',
  })
  const [ownerStatusForm, setOwnerStatusForm] = useState({ status: 'ACTIVE', reason: '' })
  const isAdmin = Boolean(readAuthSession()?.user?.admin)

  useEffect(() => {
    let active = true
    api.get(`/api/board/posts/${id}`)
      .then(({ data }) => {
        if (active) {
          setPost(data)
          setSupportForm({
            status: data?.supportStatus === 'OPEN' ? 'IN_PROGRESS' : data?.supportStatus || 'IN_PROGRESS',
            response: data?.adminResponse || '',
          })
          setDisputeForm((current) => ({ ...current, response: data?.adminResponse || '' }))
          setOwnerStatusForm({
            status: data?.claimedOwnerAccountStatus || 'ACTIVE',
            reason: '',
          })
        }
      })
      .catch((requestError) => {
        if (!active) return
        if (requestError?.response?.status === 401) {
          clearAuthSession()
          navigate('/login', { replace: true, state: { from: location } })
          return
        }
        setError(getApiErrorMessage(requestError, '게시글을 불러오지 못했습니다.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [id, location, navigate])

  const boardPath = post?.type === 'SUPPORT' ? '/board/support' : '/board/free'

  const handleSupportUpdate = async (event) => {
    event.preventDefault()
    try {
      setPending('support')
      setAdminError('')
      const { data } = await api.patch(`/api/admin/board/posts/${post.id}/support`, supportForm)
      setPost(data)
      setSupportForm({ status: data.supportStatus, response: data.adminResponse || '' })
    } catch (requestError) {
      setAdminError(getApiErrorMessage(requestError, '문의 상태를 변경하지 못했습니다.'))
    } finally {
      setPending('')
    }
  }

  const handleDisputeResolution = async (event) => {
    event.preventDefault()
    if (!window.confirm('선택한 내용으로 OUID 분쟁을 종결할까요? 연결 및 계정 상태가 즉시 변경됩니다.')) return
    try {
      setPending('dispute')
      setAdminError('')
      const { data } = await api.patch(`/api/admin/board/posts/${post.id}/ouid-dispute`, disputeForm)
      setPost(data)
      setSupportForm({ status: data.supportStatus, response: data.adminResponse || '' })
    } catch (requestError) {
      setAdminError(getApiErrorMessage(requestError, 'OUID 분쟁을 처리하지 못했습니다.'))
    } finally {
      setPending('')
    }
  }

  const handleOwnerStatusUpdate = async (event) => {
    event.preventDefault()
    if (!window.confirm('분쟁 대상 계정의 이용 상태를 변경할까요?')) return
    try {
      setPending('owner-status')
      setAdminError('')
      const { data } = await api.patch(`/api/admin/board/posts/${post.id}/claimed-owner-status`, ownerStatusForm)
      setPost(data)
      setOwnerStatusForm({ status: data.claimedOwnerAccountStatus || 'ACTIVE', reason: '' })
    } catch (requestError) {
      setAdminError(getApiErrorMessage(requestError, '계정 상태를 변경하지 못했습니다.'))
    } finally {
      setPending('')
    }
  }

  return (
    <div className="player-shell">
      <Header />
      <NavBar />
      <main className="player-page board-page">
        <div className="player-container board-container banner-content-layout">
          <div className="record-banner board-banner"><img src="/sa-assets/sa-board-banner.png" alt="SA-TRACKER 게시글" /></div>
          <section className="record-section board-detail-section">
            {loading && <div className="board-list-state" aria-busy="true">게시글을 불러오는 중입니다.</div>}
            {!loading && error && <div className="board-list-state is-error" role="alert">{error}</div>}
            {!loading && post && (
              <>
                <header className="board-detail-header">
                  <div>
                    <span>{post.notice ? 'NOTICE' : post.privatePost ? 'PRIVATE SUPPORT' : 'COMMUNITY'}</span>
                    <h1>{post.title}</h1>
                  </div>
                  <dl>
                    <div><dt>작성자</dt><dd>{post.authorName}</dd></div>
                    <div><dt>작성일</dt><dd>{formatDetailDate(post.createdAt)}</dd></div>
                    <div><dt>조회</dt><dd>{post.viewCount ?? 0}</dd></div>
                  </dl>
                </header>
                {post.privatePost && <div className="board-private-notice">이 문의글과 첨부 이미지는 작성자와 관리자만 확인할 수 있습니다.</div>}
                {post.type === 'SUPPORT' && !post.notice && (
                  <section className="board-support-summary">
                    <div>
                      <span>문의 유형</span>
                      <strong>{supportCategoryLabel(post.supportCategory)}</strong>
                    </div>
                    <div>
                      <span>처리 상태</span>
                      <strong className={`support-status status-${String(post.supportStatus || 'OPEN').toLowerCase()}`}>
                        {supportStatusLabel(post.supportStatus)}
                      </strong>
                    </div>
                    {post.supportCategory === 'OUID_DISPUTE' && (
                      <div>
                        <span>분쟁 닉네임</span>
                        <strong>{post.claimedSuddenNickname || '-'}</strong>
                      </div>
                    )}
                  </section>
                )}
                <div className="board-detail-content">{post.content}</div>
                {Array.isArray(post.imageUrls) && post.imageUrls.length > 0 && (
                  <div className="board-detail-images">
                    {post.imageUrls.map((imageUrl, index) => (
                      <BoardPostImage
                        key={imageUrl}
                        imageUrl={imageUrl}
                        privatePost={post.privatePost}
                        alt={`${post.title} 첨부 이미지 ${index + 1}`}
                      />
                    ))}
                  </div>
                )}
                {post.adminResponse && (
                  <section className="board-admin-response">
                    <span>ADMIN RESPONSE</span>
                    <h2>관리자 답변</h2>
                    <p>{post.adminResponse}</p>
                    {post.handledByName && <small>{post.handledByName} · {formatDetailDate(post.handledAt)}</small>}
                  </section>
                )}
                {post.type === 'SUPPORT' && Array.isArray(post.actionLogs) && post.actionLogs.length > 0 && (
                  <section className="board-support-history">
                    <h2>처리 이력</h2>
                    <ol>
                      {post.actionLogs.map((log) => (
                        <li key={log.id}>
                          <span>{supportActionLabel(log.action)}</span>
                          <p>{log.note}</p>
                          <small>{log.actorName || '회원'} · {formatDetailDate(log.createdAt)}</small>
                        </li>
                      ))}
                    </ol>
                  </section>
                )}
                {isAdmin && post.privatePost && (
                  <section className="board-support-admin-panel">
                    <div className="board-support-admin-heading">
                      <span>ADMIN CONTROL</span>
                      <h2>문의 처리</h2>
                    </div>

                    {post.supportCategory === 'OUID_DISPUTE' ? (
                      <>
                        <div className="board-dispute-owner">
                          <span>현재 연결 계정</span>
                          <strong>{post.claimedOwnerName || '현재 연결 없음'}</strong>
                          <small>{post.claimedOwnerAccountStatus ? accountStatusLabel(post.claimedOwnerAccountStatus) : '-'}</small>
                        </div>
                        {['RESOLVED', 'REJECTED'].includes(post.supportStatus) ? (
                          <>
                            <div className="board-closed-dispute">
                              <strong>이 분쟁은 종결되었습니다.</strong>
                              <p>{resolutionLabel(post.resolutionAction)} · {accountActionLabel(post.accountSanctionAction)}</p>
                            </div>
                            {post.claimedOwnerId && (
                              <form className="board-admin-form board-owner-status-form" onSubmit={handleOwnerStatusUpdate}>
                                <label>
                                  <span>기존 연결 계정 상태 재조정</span>
                                  <select value={ownerStatusForm.status} onChange={(event) => setOwnerStatusForm((current) => ({ ...current, status: event.target.value }))}>
                                    <option value="ACTIVE">정상</option>
                                    <option value="SUSPENDED">일시정지</option>
                                    <option value="BANNED">차단</option>
                                  </select>
                                </label>
                                <label>
                                  <span>변경 사유</span>
                                  <input
                                    value={ownerStatusForm.reason}
                                    onChange={(event) => setOwnerStatusForm((current) => ({ ...current, reason: event.target.value }))}
                                    maxLength={500}
                                    placeholder={ownerStatusForm.status === 'ACTIVE' ? '정상 복구 시 생략 가능' : '5자 이상 입력'}
                                    required={ownerStatusForm.status !== 'ACTIVE'}
                                  />
                                </label>
                                <button type="submit" disabled={pending !== ''}>{pending === 'owner-status' ? '변경 중...' : '계정 상태 변경'}</button>
                              </form>
                            )}
                          </>
                        ) : (
                          <form className="board-admin-form" onSubmit={handleDisputeResolution}>
                            <label>
                              <span>OUID 처리</span>
                              <select value={disputeForm.resolution} onChange={(event) => setDisputeForm((current) => ({ ...current, resolution: event.target.value }))}>
                                <option value="TRANSFER_TO_CLAIMANT">문의 작성자에게 이전</option>
                                <option value="UNLINK_EXISTING">기존 연결만 해제</option>
                                <option value="KEEP_EXISTING">기존 연결 유지</option>
                                <option value="REJECT">문의 반려</option>
                              </select>
                            </label>
                            <label>
                              <span>기존 연결 계정 상태</span>
                              <select value={disputeForm.accountAction} onChange={(event) => setDisputeForm((current) => ({ ...current, accountAction: event.target.value }))}>
                                <option value="KEEP">현재 상태 유지</option>
                                <option value="ACTIVATE">정상 상태로 복구</option>
                                <option value="SUSPEND">일시정지</option>
                                <option value="BAN">계정 차단</option>
                              </select>
                            </label>
                            <label className="board-admin-checkbox">
                              <input
                                type="checkbox"
                                checked={disputeForm.verifyClaimant}
                                disabled={disputeForm.resolution !== 'TRANSFER_TO_CLAIMANT'}
                                onChange={(event) => setDisputeForm((current) => ({ ...current, verifyClaimant: event.target.checked }))}
                              />
                              <span>증빙 확인 후 문의 작성자에게 초록 하트 부여</span>
                            </label>
                            <label>
                              <span>내부 처리 사유</span>
                              <textarea
                                value={disputeForm.reason}
                                onChange={(event) => setDisputeForm((current) => ({ ...current, reason: event.target.value }))}
                                maxLength={1000}
                                rows={4}
                                placeholder="증빙 확인 내용과 판단 사유를 기록해주세요."
                                required
                              />
                            </label>
                            <label>
                              <span>사용자 안내 답변</span>
                              <textarea
                                value={disputeForm.response}
                                onChange={(event) => setDisputeForm((current) => ({ ...current, response: event.target.value }))}
                                maxLength={5000}
                                rows={6}
                                placeholder="문의 작성자에게 보여줄 처리 결과를 입력해주세요."
                                required
                              />
                            </label>
                            <button type="submit" disabled={pending !== ''}>{pending === 'dispute' ? '처리 중...' : '분쟁 처리 및 종결'}</button>
                          </form>
                        )}
                      </>
                    ) : (
                      <form className="board-admin-form" onSubmit={handleSupportUpdate}>
                        <label>
                          <span>처리 상태</span>
                          <select value={supportForm.status} onChange={(event) => setSupportForm((current) => ({ ...current, status: event.target.value }))}>
                            <option value="OPEN">접수</option>
                            <option value="IN_PROGRESS">확인 중</option>
                            <option value="RESOLVED">처리 완료</option>
                            <option value="REJECTED">반려</option>
                          </select>
                        </label>
                        <label>
                          <span>관리자 답변</span>
                          <textarea
                            value={supportForm.response}
                            onChange={(event) => setSupportForm((current) => ({ ...current, response: event.target.value }))}
                            maxLength={5000}
                            rows={6}
                            placeholder="문의 작성자에게 보여줄 답변을 입력해주세요."
                          />
                        </label>
                        <button type="submit" disabled={pending !== ''}>{pending === 'support' ? '저장 중...' : '문의 상태 저장'}</button>
                      </form>
                    )}
                    {adminError && <div className="board-write-error" role="alert">{adminError}</div>}
                  </section>
                )}
                <div className="board-detail-actions"><Link to={boardPath}>목록으로</Link></div>
              </>
            )}
          </section>
        </div>
      </main>
      <Footer />
    </div>
  )
}

function supportCategoryLabel(value) {
  return value === 'OUID_DISPUTE' ? 'OUID 연결 분쟁' : '일반 문의'
}

function supportStatusLabel(value) {
  return {
    OPEN: '접수',
    IN_PROGRESS: '확인 중',
    RESOLVED: '처리 완료',
    REJECTED: '반려',
  }[value] || '접수'
}

function supportActionLabel(value) {
  return {
    SUPPORT_OPENED: '문의 접수',
    OUID_DISPUTE_OPENED: 'OUID 분쟁 접수',
    SUPPORT_STATUS_UPDATED: '문의 상태 변경',
    OUID_DISPUTE_RESOLVED: 'OUID 분쟁 종결',
    CLAIMED_OWNER_STATUS_UPDATED: '분쟁 대상 계정 상태 변경',
  }[value] || value
}

function resolutionLabel(value) {
  return {
    TRANSFER_TO_CLAIMANT: '문의 작성자에게 이전',
    UNLINK_EXISTING: '기존 연결 해제',
    KEEP_EXISTING: '기존 연결 유지',
    REJECT: '문의 반려',
  }[value] || '-'
}

function accountActionLabel(value) {
  return {
    KEEP: '계정 상태 유지',
    ACTIVATE: '정상 상태 복구',
    SUSPEND: '계정 일시정지',
    BAN: '계정 차단',
  }[value] || '계정 상태 유지'
}

function accountStatusLabel(value) {
  return {
    ACTIVE: '정상',
    SUSPENDED: '일시정지',
    BANNED: '차단',
  }[value] || value
}

function formatDetailDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? '-'
    : new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

export default BoardDetailPage
