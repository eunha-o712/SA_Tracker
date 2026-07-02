import "./Footer.css";

function Footer() {
  return (
    <footer className="footer">
      <div className="footer-container">
        <img
          src="/sa-assets/sa-footer-logo.png"
          alt="SA-TRACKER"
          className="footer-logo"
        />

        <p className="footer-description">
          서든어택 전적 조회 및 커뮤니티 서비스
        </p>

        <div className="footer-divider"></div>

        <p className="footer-notice">
          본 서비스는 NEXON Open API를 활용하여 제작되었습니다.
          <br />
          SA-TRACKER는 넥슨코리아의 공식 서비스가 아니며,
          제공되는 데이터는 NEXON Open API 기준으로 표시됩니다.
        </p>

        <div className="footer-links">
          <a href="/privacy">개인정보처리방침</a>
          <span>•</span>
          <a href="mailto:contact@satracker.com">문의하기</a>
        </div>

        <p className="footer-copy">
          © 2026 SA-TRACKER. All rights reserved.
        </p>
      </div>
    </footer>
  );
}

export default Footer;