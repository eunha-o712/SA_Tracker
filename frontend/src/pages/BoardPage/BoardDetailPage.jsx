import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import api, { getApiErrorMessage } from '../../api/api'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import NavBar from '../../components/NavBar/NavBar'
import { clearAuthSession } from '../../utils/authSession'
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

  useEffect(() => {
    let active = true
    api.get(`/api/board/posts/${id}`)
      .then(({ data }) => {
        if (active) setPost(data)
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

function formatDetailDate(value) {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? '-'
    : new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

export default BoardDetailPage
