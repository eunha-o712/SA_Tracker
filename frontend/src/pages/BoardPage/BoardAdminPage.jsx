import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api, { getApiErrorMessage } from '../../api/api'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import NavBar from '../../components/NavBar/NavBar'
import { readAuthSession, saveAuthSession } from '../../utils/authSession'
import '../PlayerPage/PlayerPage.css'
import './BoardPage.css'

function BoardAdminPage() {
  const navigate = useNavigate()
  const [posts, setPosts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [pendingId, setPendingId] = useState(null)
  const [pendingVerificationId, setPendingVerificationId] = useState(null)

  useEffect(() => {
    let active = true
    const currentSession = readAuthSession()
    Promise.all([api.get('/api/auth/me'), api.get('/api/admin/board/posts')])
      .then(([authResponse, postsResponse]) => {
        if (!active) return
        saveAuthSession({ ...currentSession, user: authResponse.data })
        setPosts(Array.isArray(postsResponse.data) ? postsResponse.data : [])
      })
      .catch((requestError) => {
        if (!active) return
        setError(getApiErrorMessage(requestError, '관리자 게시글 목록을 불러오지 못했습니다.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [])

  const handleDelete = async (post) => {
    if (!window.confirm(`“${post.title}” 글을 삭제할까요?`)) return
    try {
      setPendingId(post.id)
      setError('')
      await api.delete(`/api/admin/board/posts/${post.id}`)
      setPosts((current) => current.filter((item) => item.id !== post.id))
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, '게시글을 삭제하지 못했습니다.'))
    } finally {
      setPendingId(null)
    }
  }

  const handleVerification = async (post) => {
    const nextVerified = !post.authorVerified
    const action = nextVerified ? '수동 인증 완료' : '수동 인증 해제'
    if (!window.confirm(`${post.authorName} 회원을 ${action} 상태로 변경할까요?`)) return

    try {
      setPendingVerificationId(post.authorId)
      setError('')
      const { data } = await api.patch(`/api/admin/users/${post.authorId}/manual-verification`, {
        verified: nextVerified,
      })
      setPosts((current) => current.map((item) => item.authorId === post.authorId
        ? {
            ...item,
            authorLinked: Boolean(data?.ouid),
            authorVerified: Boolean(data?.nicknameVerified),
            authorName: data?.suddenNickname || item.authorName,
          }
        : item))
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, '회원 인증 상태를 변경하지 못했습니다.'))
    } finally {
      setPendingVerificationId(null)
    }
  }

  return (
    <div className="player-shell">
      <Header />
      <NavBar />
      <main className="player-page board-page">
        <div className="player-container board-container banner-content-layout">
          <div className="record-banner board-banner"><img src="/sa-assets/sa-board-banner.png" alt="SA-TRACKER 게시판 관리" /></div>
          <section className="record-section board-list-section board-admin-section">
            <div className="record-section-header">
              <div className="board-list-title"><h1 className="record-section-title">BOARD ADMIN</h1><p>게시글과 공지사항을 통합 관리합니다.</p></div>
              <div className="board-header-actions"><span className="record-section-sub">관리자 전용</span><button type="button" className="board-write-button" onClick={() => navigate('/board/write', { state: { type: 'free', adminMode: true } })}>글쓰기</button></div>
            </div>

            <div className="board-admin-head" aria-hidden="true"><span>구분</span><span>제목</span><span>작성자</span><span>작성일</span><span>관리</span></div>
            {loading && <div className="board-list-state" aria-busy="true">관리자 목록을 불러오는 중입니다.</div>}
            {!loading && error && <div className="board-list-state is-error" role="alert">{error}</div>}
            {!loading && !error && posts.length === 0 && <div className="board-empty-state"><span>BOARD ADMIN</span><strong>아직 등록된 게시글이 없습니다.</strong></div>}
            {!loading && posts.length > 0 && <div className="board-admin-list">{posts.map((post) => <article className={post.notice ? 'board-admin-row is-notice' : 'board-admin-row'} key={post.id}>
              <span>{post.notice ? '공지' : post.type === 'SUPPORT' ? '문의' : '자유'}</span>
              <div className="board-admin-title"><strong><Link to={`/board/post/${post.id}`}>{post.title}</Link></strong>{post.notice && <small>양쪽 게시판 상단 고정</small>}</div>
              <span className="board-admin-author">
                {post.authorName}
                {post.authorVerified && <small className="is-verified"><span className="board-admin-badge verified" aria-hidden="true" />수동 인증</small>}
                {!post.authorVerified && post.authorLinked && <small><span className="board-admin-badge linked" aria-hidden="true" />OUID 연결</small>}
              </span>
              <time dateTime={post.createdAt}>{formatAdminDate(post.createdAt)}</time>
              <div className="board-admin-actions">
                <Link to={`/board/post/${post.id}`}>보기</Link>
                {post.type === 'SUPPORT' && !post.notice && (
                  <button
                    type="button"
                    className={post.authorVerified ? 'verification is-verified' : 'verification'}
                    disabled={!post.authorLinked || pendingVerificationId === post.authorId}
                    title={post.authorLinked ? undefined : 'OUID 연결 전에는 수동 인증할 수 없습니다.'}
                    onClick={() => handleVerification(post)}
                  >
                    {pendingVerificationId === post.authorId
                      ? '처리 중'
                      : post.authorVerified ? '인증 해제' : post.authorLinked ? '인증 완료' : '연결 전'}
                  </button>
                )}
                <button type="button" className="delete" disabled={pendingId === post.id} onClick={() => handleDelete(post)}>{pendingId === post.id ? '삭제 중' : '삭제'}</button>
              </div>
            </article>)}</div>}
          </section>
        </div>
      </main>
      <Footer />
    </div>
  )
}

function formatAdminDate(value) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '-' : new Intl.DateTimeFormat('ko-KR', { dateStyle: 'short' }).format(date)
}

export default BoardAdminPage
