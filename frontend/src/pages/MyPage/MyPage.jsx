import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api, { getApiErrorMessage } from '../../api/api'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import NavBar from '../../components/NavBar/NavBar'
import { readAuthSession, saveAuthSession } from '../../utils/authSession'
import '../PlayerPage/PlayerPage.css'
import './MyPage.css'

function MyPage() {
  const navigate = useNavigate()
  const [session, setSession] = useState(readAuthSession)
  const [user, setUser] = useState(() => readAuthSession()?.user || null)
  const [linkNickname, setLinkNickname] = useState('')
  const [loading, setLoading] = useState(true)
  const [pending, setPending] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [linkFeedback, setLinkFeedback] = useState(null)
  const [ouidHelpOpen, setOuidHelpOpen] = useState(false)
  const ouidHelpButtonRef = useRef(null)

  const displayName = useMemo(
    () => user?.suddenNickname || user?.displayName || user?.loginId || user?.email || 'UNKNOWN',
    [user]
  )
  const badgeStatus = getBadgeStatus(user)

  const updateSession = useCallback((baseSession, nextUser) => {
    const nextSession = { ...baseSession, user: nextUser }
    saveAuthSession(nextSession)
    setSession(nextSession)
    setUser(nextUser)
  }, [])

  useEffect(() => {
    const currentSession = readAuthSession()
    if (!currentSession) {
      navigate('/login')
      return
    }

    api.get('/api/auth/me')
      .then(({ data }) => {
        updateSession(currentSession, data)
      })
      .catch((requestError) => {
        setError(getApiErrorMessage(requestError, '계정 정보를 불러오지 못했습니다.'))
      })
      .finally(() => setLoading(false))
  }, [navigate, updateSession])

  useEffect(() => {
    if (!ouidHelpOpen) return undefined

    const previousOverflow = document.body.style.overflow
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        setOuidHelpOpen(false)
        requestAnimationFrame(() => ouidHelpButtonRef.current?.focus())
      }
    }

    document.body.style.overflow = 'hidden'
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.body.style.overflow = previousOverflow
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [ouidHelpOpen])

  const clearNotice = () => {
    setMessage('')
    setError('')
    setLinkFeedback(null)
  }

  const handleLinkSuddenAccount = async (event) => {
    event.preventDefault()
    setPending('link')
    clearNotice()

    try {
      const { data } = await api.patch('/api/auth/sudden-account/link', {
        suddenNickname: linkNickname,
      })
      updateSession(session || readAuthSession(), data)
      setLinkNickname('')
      setLinkFeedback({
        type: 'success',
        message: '서든 계정이 연결되었습니다. 주황색 하트 인증마크가 활성화되었습니다.',
      })
    } catch (requestError) {
      setLinkFeedback({
        type: 'error',
        message: getApiErrorMessage(
          requestError,
          '서든 계정을 연결하지 못했습니다. 닉네임을 확인한 뒤 다시 시도해 주세요.'
        ),
      })
    } finally {
      setPending('')
    }
  }

  const handleSyncNickname = async () => {
    setPending('sync')
    clearNotice()

    try {
      const { data } = await api.patch('/api/auth/sudden-nickname/sync')
      updateSession(session || readAuthSession(), data)
      setMessage('저장된 Nexon OUID에서 현재 닉네임을 불러왔습니다.')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, '닉네임을 동기화하지 못했습니다.'))
    } finally {
      setPending('')
    }
  }

  const handleClanNoneChange = async (event) => {
    const noClan = event.target.checked
    setPending('clan')
    clearNotice()

    try {
      const { data } = await api.patch('/api/auth/clan-status', { noClan })
      updateSession(session || readAuthSession(), data)
      setMessage(noClan
        ? '현재 클랜 상태를 소속 없음으로 저장했습니다.'
        : '넥슨 API의 클랜 정보를 다시 사용합니다.')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, '클랜 상태를 변경하지 못했습니다.'))
    } finally {
      setPending('')
    }
  }

  const handlePasswordReset = async () => {
    setPending('password')
    clearNotice()

    try {
      const { data } = await api.post('/api/auth/password-reset/request', { email: user?.email })
      setMessage(data?.message || '가입한 이메일로 비밀번호 재설정 링크를 전송했습니다.')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, '비밀번호 재설정 링크를 전송하지 못했습니다.'))
    } finally {
      setPending('')
    }
  }

  return (
    <div className="player-shell">
      <Header />
      <NavBar />

      <main className="player-page">
        <div className="player-container">
          <section className="mypage-panel">
            <div className="mypage-panel__header">
              <div>
                <span>ACCOUNT CONTROL</span>
                <h1>MY PAGE</h1>
                <p>가입 시 내부 ID가 생성됩니다. 서든어택 계정을 연결하면 닉네임과 인증마크가 표시됩니다.</p>
              </div>

              <Link className="mypage-outline-button" to={user?.suddenNickname ? `/player/${encodeURIComponent(user.suddenNickname)}` : '/player'}>
                OPEN PROFILE
              </Link>
            </div>

            {loading ? (
              <div className="mypage-state">LOADING ACCOUNT...</div>
            ) : (
              <>
                <div className="mypage-identity">
                  <div>
                    <span>DISPLAY NAME</span>
                    <strong>
                      {displayName}
                      <AccountBadge status={badgeStatus} />
                    </strong>
                  </div>
                  <p>{getBadgeCopy(badgeStatus)}</p>
                </div>

                <div className="mypage-grid">
                  <InfoCard label="EMAIL" value={user?.email || '-'} />
                  <InfoCard label="INTERNAL ID" value={user?.loginId || '-'} />
                  <InfoCard label="SUDDEN NICKNAME" value={user?.suddenNickname || 'UNLINKED'} />
                  <InfoCard label="OUID" value={user?.ouid || '-'} muted />
                </div>

                <div className="mypage-actions">
                  <form className="mypage-link-card" onSubmit={handleLinkSuddenAccount}>
                    <div>
                      <div className="mypage-link-title-row">
                        <h2>SUDDEN ACCOUNT LINK</h2>
                        <button
                          ref={ouidHelpButtonRef}
                          className="mypage-help-button"
                          type="button"
                          aria-label="서든 계정 연결 방법 보기"
                          aria-haspopup="dialog"
                          aria-expanded={ouidHelpOpen}
                          onClick={() => setOuidHelpOpen(true)}
                        >
                          ?
                        </button>
                      </div>
                      <p>OUID를 직접 찾을 필요 없이, 현재 서든어택 닉네임만 입력하면 자동으로 연결됩니다.</p>
                    </div>
                    <div className="mypage-link-form">
                      <input
                        value={user?.ouid ? (user?.suddenNickname || '') : linkNickname}
                        onChange={(event) => {
                          setLinkNickname(event.target.value)
                          setLinkFeedback(null)
                        }}
                        placeholder={user?.ouid ? 'Linked Sudden nickname' : 'Sudden nickname'}
                        minLength="2"
                        maxLength="20"
                        disabled={pending !== '' || Boolean(user?.ouid)}
                      />
                      <button type="submit" disabled={pending !== '' || Boolean(user?.ouid) || !linkNickname.trim()}>
                        {pending === 'link' ? 'LINKING...' : user?.ouid ? 'LINKED' : 'LINK ACCOUNT'}
                      </button>
                    </div>
                    {linkFeedback && (
                      <div
                        className={`mypage-link-feedback is-${linkFeedback.type}`}
                        role={linkFeedback.type === 'error' ? 'alert' : 'status'}
                        aria-live="polite"
                      >
                        {linkFeedback.message}
                      </div>
                    )}
                    <small>
                      이 OUID가 이미 다른 계정에 연결되어 있다면 비공개 문의게시판을 이용해 주세요.
                      {' '}<Link
                        to="/board/write"
                        state={{
                          type: 'support',
                          supportCategory: 'OUID_DISPUTE',
                          suddenNickname: linkNickname.trim(),
                        }}
                      >
                        OPEN PRIVATE INQUIRY
                      </Link>
                    </small>
                  </form>

                  <ActionCard
                    title="NEXON NICKNAME SYNC"
                    description="저장된 OUID에서 현재 닉네임을 다시 불러옵니다. 닉네임을 변경한 뒤 이용해 주세요."
                    buttonText={pending === 'sync' ? 'SYNCING...' : 'SYNC NICKNAME'}
                    disabled={pending !== '' || !user?.ouid}
                    onClick={handleSyncNickname}
                  />

                  <div className="mypage-clan-card">
                    <div>
                      <h2>CURRENT CLAN STATUS</h2>
                      <p>넥슨 Open API의 클랜 정보가 실제 상태와 다를 때 직접 보정할 수 있습니다.</p>
                    </div>
                    <label className={`mypage-clan-toggle${user?.clanNone ? ' is-active' : ''}`}>
                      <input
                        type="checkbox"
                        checked={Boolean(user?.clanNone)}
                        disabled={pending !== '' || !user?.ouid}
                        onChange={handleClanNoneChange}
                      />
                      <span className="mypage-clan-toggle__control" aria-hidden="true" />
                      <strong>현재 소속된 클랜이 없습니다</strong>
                    </label>
                    <small>
                      전적 검색에는 Open API상 마지막 클랜명이 남아 있어 자동으로 클랜명이 생성됩니다. 현재 상태가 클랜 탈퇴 상태일 때만 선택해 주세요.
                    </small>
                  </div>

                  <ActionCard
                    title="PASSWORD RESET"
                    description="가입한 이메일로 15분 동안 사용할 수 있는 비밀번호 재설정 링크를 전송합니다."
                    buttonText={pending === 'password' ? 'SENDING...' : 'SEND RESET LINK'}
                    disabled={pending !== '' || !user?.email}
                    onClick={handlePasswordReset}
                  />
                </div>

                {(message || error) && (
                  <div className={`mypage-message${error ? ' is-error' : ''}`}>
                    <span>{error || message}</span>
                  </div>
                )}
              </>
            )}
          </section>
        </div>
      </main>

      {ouidHelpOpen && (
        <div
          className="mypage-modal-backdrop"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              setOuidHelpOpen(false)
              requestAnimationFrame(() => ouidHelpButtonRef.current?.focus())
            }
          }}
        >
          <section
            className="mypage-help-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="ouid-help-title"
            aria-describedby="ouid-help-description"
          >
            <button
              className="mypage-help-modal__close"
              type="button"
              aria-label="안내 닫기"
              onClick={() => {
                setOuidHelpOpen(false)
                requestAnimationFrame(() => ouidHelpButtonRef.current?.focus())
              }}
              autoFocus
            >
              ×
            </button>
            <span className="mypage-help-modal__eyebrow">ACCOUNT LINK GUIDE</span>
            <h2 id="ouid-help-title">서든 계정 연결 방법</h2>
            <p id="ouid-help-description" className="mypage-help-modal__lead">
              OUID를 직접 찾거나 입력할 필요가 없습니다. 현재 사용 중인 서든어택 닉네임만 확인해 주세요.
            </p>
            <ol className="mypage-help-steps">
              <li>
                <strong>닉네임 확인</strong>
                <span>서든어택에 접속해 현재 사용 중인 닉네임을 확인합니다.</span>
              </li>
              <li>
                <strong>닉네임 입력</strong>
                <span>마이페이지의 입력칸에 띄어쓰기와 특수문자를 포함해 정확히 입력합니다.</span>
              </li>
              <li>
                <strong>자동 연결</strong>
                <span>
                  LINK ACCOUNT를 누르면 Nexon Open API에서
                  <br />
                  OUID를 찾아 계정에 연결합니다.
                </span>
              </li>
            </ol>
            <div className="mypage-help-modal__info">
              <strong>OUID란?</strong>
              <p>닉네임을 변경해도 같은 서든 계정을 구분할 수 있는 Nexon 고유 식별값입니다.</p>
            </div>
            <p className="mypage-help-modal__security">
              Nexon 아이디나 비밀번호는 입력하지 않으며, SATrk도 해당 정보를 수집하지 않습니다.
              <br />
              하나의 OUID는 하나의 SATrk 계정에만 연결할 수 있습니다.
            </p>
            <button
              className="mypage-help-modal__confirm"
              type="button"
              onClick={() => {
                setOuidHelpOpen(false)
                requestAnimationFrame(() => ouidHelpButtonRef.current?.focus())
              }}
            >
              CLOSE GUIDE
            </button>
          </section>
        </div>
      )}

      <Footer />
    </div>
  )
}

function InfoCard({ label, value, muted = false }) {
  return (
    <div className="mypage-info-card">
      <span>{label}</span>
      <strong className={muted ? 'is-muted' : ''}>{value}</strong>
    </div>
  )
}

function ActionCard({ title, description, buttonText, disabled, onClick }) {
  return (
    <div className="mypage-action-card">
      <div>
        <h2>{title}</h2>
        <p>{description}</p>
      </div>
      <button type="button" disabled={disabled} onClick={onClick}>
        {buttonText}
      </button>
    </div>
  )
}

function AccountBadge({ status }) {
  if (status === 'none') return null
  if (status === 'admin') {
    const label = 'SA-TRACKER 관리자'
    return (
      <span className="account-heart-badge admin" aria-label={label} title={label}>
        M
      </span>
    )
  }
  const label = status === 'verified' ? '운영자 수동 확인 완료' : 'OUID 연결'
  return <span className={`account-heart-badge ${status}`} role="img" aria-label={label} title={label} />
}

function getBadgeStatus(user) {
  if (user?.admin) return 'admin'
  if (!user?.ouid) return 'none'
  return user.nicknameVerified ? 'verified' : 'linked'
}

function getBadgeCopy(status) {
  if (status === 'admin') return 'SA-TRACKER 관리자 계정입니다.'
  if (status === 'verified') return '운영자 수동 확인이 완료되었습니다. OUID 연결 사용자와 동일한 기능을 이용할 수 있습니다.'
  if (status === 'linked') return 'OUID 연결이 완료되었습니다. 수동 확인 사용자와 동일한 기능을 이용할 수 있습니다.'
  return '아직 서든어택 계정이 연결되지 않았습니다. 계정 연결 전에도 기본 서비스는 이용할 수 있습니다.'
}

export default MyPage
