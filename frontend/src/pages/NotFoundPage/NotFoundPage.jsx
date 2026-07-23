import { Link } from 'react-router-dom'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import NavBar from '../../components/NavBar/NavBar'
import './NotFoundPage.css'

function NotFoundPage() {
  return (
    <div className="not-found-shell">
      <Header />
      <NavBar />
      <main className="not-found-page">
        <section className="not-found-panel" aria-labelledby="not-found-title">
          <p className="not-found-kicker">ROUTE NOT FOUND</p>
          <p className="not-found-code" aria-hidden="true">404</p>
          <h1 id="not-found-title">페이지를 찾을 수 없습니다.</h1>
          <p className="not-found-description">
            입력한 주소가 잘못되었거나 페이지가 이동되었습니다.
            아래 메뉴에서 원하는 화면으로 다시 이동해주세요.
          </p>
          <div className="not-found-actions">
            <Link className="not-found-link not-found-link--primary" to="/">
              메인으로 돌아가기
            </Link>
            <Link className="not-found-link not-found-link--secondary" to="/player">
              플레이어 검색
            </Link>
          </div>
        </section>
      </main>
      <Footer />
    </div>
  )
}

export default NotFoundPage
