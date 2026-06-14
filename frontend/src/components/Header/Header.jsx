import './Header.css'

function Header() {
  return (
    <header className="global-header">
      <div className="global-header__inner">
        <img
          src="/sa-assets/sa-global-header-ver1.png"
          alt="SA-Tracker global header"
          className="global-header__image"
        />
      </div>
    </header>
  )
}

export default Header