import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import api, { getApiErrorMessage } from '../../api/api'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import NavBar from '../../components/NavBar/NavBar'
import { readAuthSession, subscribeToAuthSession } from '../../utils/authSession'
import '../PlayerPage/PlayerPage.css'
import './BoardPage.css'

const BOARD_OPTIONS = [
  { value: 'free', apiType: 'FREE', label: '자유게시판', description: '게임과 클랜에 관한 자유로운 이야기를 나눕니다.' },
  { value: 'support', apiType: 'SUPPORT', label: '문의사항', description: '작성자와 관리자만 확인하는 비공개 문의를 등록합니다.' },
]

const SUPPORT_CATEGORIES = [
  { value: 'GENERAL', label: '일반 문의', description: '서비스 이용, 오류, 건의사항을 문의합니다.' },
  { value: 'OUID_DISPUTE', label: 'OUID 연결 분쟁', description: '본인 서든 계정이 다른 회원에게 연결된 경우 소유권 확인을 요청합니다.' },
]

function BoardWritePage() {
  const navigate = useNavigate()
  const location = useLocation()
  const initialType = BOARD_OPTIONS.some((option) => option.value === location.state?.type) ? location.state.type : 'free'
  const initialSupportCategory = SUPPORT_CATEGORIES.some((option) => option.value === location.state?.supportCategory)
    ? location.state.supportCategory
    : 'GENERAL'
  const [type, setType] = useState(initialType)
  const [supportCategory, setSupportCategory] = useState(initialSupportCategory)
  const [suddenNickname, setSuddenNickname] = useState(location.state?.suddenNickname || '')
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [images, setImages] = useState([])
  const [session, setSession] = useState(readAuthSession)
  const [notice, setNotice] = useState(false)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')
  const selectedBoard = useMemo(() => BOARD_OPTIONS.find((option) => option.value === type), [type])
  const isAdmin = Boolean(session?.user?.admin)
  const isAdminWriteMode = isAdmin && Boolean(location.state?.adminMode)
  const imagePreviews = useMemo(() => images.map((file) => ({ file, url: URL.createObjectURL(file) })), [images])

  useEffect(() => () => {
    imagePreviews.forEach((preview) => URL.revokeObjectURL(preview.url))
  }, [imagePreviews])

  useEffect(() => subscribeToAuthSession(() => setSession(readAuthSession())), [])

  const handleImageChange = (event) => {
    const selectedImages = Array.from(event.target.files ?? [])
    event.target.value = ''
    if (images.length + selectedImages.length > 5) {
      return setError('이미지는 최대 5장까지 첨부할 수 있습니다.')
    }
    if (selectedImages.some((file) => file.size > 8 * 1024 * 1024)) {
      return setError('이미지 한 장의 크기는 8MB 이하여야 합니다.')
    }
    if (selectedImages.some((file) => !['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type))) {
      return setError('JPG, PNG, GIF, WEBP 이미지만 첨부할 수 있습니다.')
    }
    setError('')
    setImages((current) => [...current, ...selectedImages])
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    const normalizedTitle = title.trim()
    const normalizedContent = content.trim()
    if (!normalizedTitle) return setError('제목을 입력해주세요.')
    if (!normalizedContent) return setError('내용을 입력해주세요.')
    if (type === 'support' && supportCategory === 'OUID_DISPUTE' && suddenNickname.trim().length < 2) {
      return setError('연결하려는 서든어택 닉네임을 입력해주세요.')
    }
    if (type === 'support' && supportCategory === 'OUID_DISPUTE' && images.length === 0) {
      return setError('OUID 분쟁 문의에는 로그인 상태를 확인할 수 있는 증빙 이미지를 한 장 이상 첨부해주세요.')
    }

    try {
      setPending(true)
      setError('')
      const formData = new FormData()
      formData.append('type', selectedBoard.apiType)
      formData.append('title', normalizedTitle)
      formData.append('content', normalizedContent)
      if (type === 'support') {
        formData.append('supportCategory', supportCategory)
        if (supportCategory === 'OUID_DISPUTE') formData.append('suddenNickname', suddenNickname.trim())
      }
      if (isAdminWriteMode) formData.append('notice', String(notice))
      images.forEach((image) => formData.append('images', image))
      await api.post(isAdminWriteMode && notice ? '/api/admin/board/posts' : '/api/board/posts', formData)
      navigate(`/board/${selectedBoard.value}`, { replace: true })
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, '게시글을 등록하지 못했습니다.'))
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="player-shell">
      <Header />
      <NavBar />
      <main className="player-page board-page">
        <div className="player-container board-container banner-content-layout">
          <div className="record-banner board-banner">
            <img src="/sa-assets/sa-board-banner.png" alt="SA-TRACKER 커뮤니티 게시판" />
          </div>

          <section className="record-section board-write-section">
            <div className="record-section-header">
              <div className="board-list-title">
                <h1 className="record-section-title">WRITE POST</h1>
                <p>게시판을 선택하고 새로운 이야기를 작성하세요.</p>
              </div>
              <span className="record-section-sub">글쓰기</span>
            </div>

            <form className="board-write-form" onSubmit={handleSubmit}>
              <fieldset className="board-type-selector">
                <legend>게시판 선택</legend>
                <div className="board-type-options">
                  {BOARD_OPTIONS.map((option) => <label className={type === option.value ? 'board-type-option active' : 'board-type-option'} key={option.value}>
                    <input type="radio" name="boardType" value={option.value} checked={type === option.value} onChange={() => setType(option.value)} />
                    <span>{option.label}</span>
                    <small>{option.description}</small>
                  </label>)}
                </div>
              </fieldset>

              {isAdminWriteMode && <label className={notice ? 'board-notice-toggle active' : 'board-notice-toggle'}>
                <input type="checkbox" checked={notice} onChange={(event) => setNotice(event.target.checked)} />
                <span>공지사항으로 등록</span>
                <small>체크하면 자유게시판과 문의사항 양쪽 최상단에 굵은 제목으로 고정됩니다.</small>
              </label>}

              {type === 'support' && !notice && (
                <>
                  <fieldset className="board-type-selector board-support-category">
                    <legend>문의 유형</legend>
                    <div className="board-type-options">
                      {SUPPORT_CATEGORIES.map((option) => <label className={supportCategory === option.value ? 'board-type-option active' : 'board-type-option'} key={option.value}>
                        <input type="radio" name="supportCategory" value={option.value} checked={supportCategory === option.value} onChange={() => setSupportCategory(option.value)} />
                        <span>{option.label}</span>
                        <small>{option.description}</small>
                      </label>)}
                    </div>
                  </fieldset>

                  <div className="board-private-notice">
                    문의글과 첨부 이미지는 작성자 본인과 관리자만 확인할 수 있습니다.
                  </div>

                  {supportCategory === 'OUID_DISPUTE' && (
                    <div className="board-dispute-evidence">
                      <label className="board-form-field" htmlFor="dispute-nickname">
                        <span>연결하려는 서든어택 닉네임</span>
                        <input
                          id="dispute-nickname"
                          value={suddenNickname}
                          onChange={(event) => setSuddenNickname(event.target.value)}
                          maxLength={20}
                          placeholder="서든어택 닉네임"
                          required
                        />
                      </label>
                      <div className="board-evidence-guide">
                        <strong>증빙 이미지 안내</strong>
                        <p>해당 닉네임으로 로그인한 상태를 확인할 수 있는 화면을 첨부해주세요.</p>
                        <p>비밀번호, 이메일, 실명, PC 정보 등 불필요한 개인정보는 반드시 가려주세요.</p>
                      </div>
                    </div>
                  )}
                </>
              )}

              <label className="board-form-field" htmlFor="board-title">
                <span>제목</span>
                <input id="board-title" value={title} onChange={(event) => setTitle(event.target.value)} maxLength={120} placeholder="제목을 입력해주세요." autoFocus />
                <small>{title.length} / 120</small>
              </label>

              <label className="board-form-field board-content-field" htmlFor="board-content">
                <span>내용</span>
                <textarea id="board-content" value={content} onChange={(event) => setContent(event.target.value)} maxLength={5000} placeholder="내용을 입력해주세요." rows={14} />
                <small>{content.length} / 5,000</small>
              </label>

              <section className="board-image-field" aria-labelledby="board-image-label">
                <div className="board-image-field-header">
                  <div><span id="board-image-label">이미지 첨부</span><small>JPG, PNG, GIF, WEBP · 장당 8MB · 최대 5장</small></div>
                  <label className="board-image-add-button">
                    <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple onChange={handleImageChange} disabled={pending || images.length >= 5} />
                    + 이미지 추가
                  </label>
                </div>
                {imagePreviews.length > 0
                  ? <div className="board-image-previews">{imagePreviews.map((preview, index) => <figure key={`${preview.file.name}-${preview.file.lastModified}-${index}`}>
                    <img src={preview.url} alt={`${index + 1}번째 첨부 이미지 미리보기`} />
                    <figcaption>{preview.file.name}</figcaption>
                    <button type="button" aria-label={`${preview.file.name} 삭제`} onClick={() => setImages((current) => current.filter((_, imageIndex) => imageIndex !== index))}>×</button>
                  </figure>)}</div>
                  : <div className="board-image-empty">
                    {type === 'support' && supportCategory === 'OUID_DISPUTE'
                      ? 'OUID 분쟁 확인을 위한 증빙 이미지를 한 장 이상 추가해주세요.'
                      : '본문과 함께 보여줄 이미지를 추가할 수 있습니다.'}
                  </div>}
              </section>

              {error && <div className="board-write-error" role="alert">{error}</div>}
              <div className="board-write-actions">
                <button type="button" className="secondary" onClick={() => navigate(-1)} disabled={pending}>취소</button>
                <button type="submit" disabled={pending}>{pending ? '등록 중...' : `${selectedBoard.label}에 등록`}</button>
              </div>
            </form>
          </section>
        </div>
      </main>
      <Footer />
    </div>
  )
}

export default BoardWritePage
