import { useEffect, useState } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import api, { getApiErrorMessage } from '../../api/api'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import NavBar from '../../components/NavBar/NavBar'
import BoardPostImage from './BoardPostImage'
import '../PlayerPage/PlayerPage.css'
import './BoardPage.css'

const BOARD_CONFIG = {
  free: {
    apiType: 'FREE',
    title: 'FREE BOARD',
    subtitle: '자유게시판',
    description: '게임과 클랜에 관한 이야기를 자유롭게 나누는 공간입니다.',
    empty: '아직 등록된 자유게시판 글이 없습니다.',
  },
  support: {
    apiType: 'SUPPORT',
    title: 'SUPPORT BOARD',
    subtitle: '문의사항',
    description: '작성자와 관리자만 확인할 수 있는 비공개 문의 공간입니다.',
    empty: '아직 등록된 문의 글이 없습니다.',
  },
}

function BoardPage() {
  const { type } = useParams()
  const board = BOARD_CONFIG[type]

  if (!board) return <Navigate to="/board/free" replace />

  return <BoardPageContent key={type} type={type} board={board} />
}

function BoardPageContent({ type, board }) {
  const navigate = useNavigate()
  const [posts, setPosts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    api.get('/api/board/posts', { params: { type: board.apiType } })
      .then((response) => {
        if (active) setPosts(Array.isArray(response.data) ? response.data : [])
      })
      .catch((requestError) => {
        if (active) setError(getApiErrorMessage(requestError, '게시글을 불러오지 못했습니다.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => { active = false }
  }, [board])

  return (
    <div className="player-shell">
      <Header />
      <NavBar />

      <main className="player-page board-page">
        <div className="player-container board-container banner-content-layout">
          <div className="record-banner board-banner">
            <img src="/sa-assets/sa-board-banner.png" alt="SA-TRACKER 커뮤니티 게시판" />
          </div>

          <section className="record-section board-list-section">
            <div className="record-section-header">
              <div className="board-list-title">
                <h2 className="record-section-title">{board.title}</h2>
                <p>{board.description}</p>
              </div>
              <div className="board-header-actions">
                <span className="record-section-sub">{board.subtitle}</span>
                <button type="button" className="board-write-button" onClick={() => navigate('/board/write', { state: { type } })}>글쓰기</button>
              </div>
            </div>

            <div className="board-table-head" aria-hidden="true">
              <span>번호</span>
              <span>제목</span>
              <span>작성자</span>
              <span>작성일</span>
              <span>조회</span>
            </div>
            {loading && <div className="board-list-state" aria-busy="true">게시글을 불러오는 중입니다.</div>}
            {!loading && error && <div className="board-list-state is-error" role="alert">{error}</div>}
            {!loading && !error && posts.length > 0 && <div className="board-post-list">
              {posts.map((post, index) => <article className={post.notice ? 'board-post-row is-notice' : 'board-post-row'} key={post.id}>
                <span className="board-post-number">{posts.length - index}</span>
                <div className="board-post-title">
                  <strong>
                    {post.notice && <em className="board-notice-badge">공지</em>}
                    {post.privatePost && <em className="board-private-badge">비공개</em>}
                    <Link className="board-post-link" to={`/board/post/${post.id}`}>{post.title}</Link>
                  </strong>
                  <p>{post.content}</p>
                  {Array.isArray(post.imageUrls) && post.imageUrls.length > 0 && <div className="board-post-images">
                    {post.imageUrls.map((imageUrl, imageIndex) => (
                      <BoardPostImage
                        imageUrl={imageUrl}
                        privatePost={post.privatePost}
                        alt={`${post.title} 첨부 이미지 ${imageIndex + 1}`}
                        key={imageUrl}
                      />
                    ))}
                  </div>}
                </div>
                <span className="board-post-author">{post.authorName}</span>
                <time dateTime={post.createdAt}>{formatBoardDate(post.createdAt)}</time>
                <span className="board-post-views">{post.viewCount ?? 0}</span>
              </article>)}
            </div>}
            {!loading && !error && posts.length === 0 && <div className="board-empty-state">
              <span>{board.subtitle}</span>
              <strong>{board.empty}</strong>
              <p>글쓰기 버튼을 눌러 첫 게시글을 등록해보세요.</p>
            </div>}
          </section>
        </div>
      </main>

      <Footer />
    </div>
  )
}

function formatBoardDate(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return new Intl.DateTimeFormat('ko-KR', { month: '2-digit', day: '2-digit' }).format(date)
}

export default BoardPage
