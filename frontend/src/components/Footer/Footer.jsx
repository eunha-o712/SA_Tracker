import './Footer.css'

function Footer() {
  return (
    <footer className="site-footer">
      <div className="site-footer__inner">
        <img
          src="/sa-assets/sa-logo-light-female.png"
          alt="SA-Tracker footer logo"
          className="site-footer__logo"
        />
        <p className="site-footer__text">
          SA-Tracker · Search your records, review your matches, track your flow.
        </p>
      </div>
    </footer>
  )
}

export default Footer
