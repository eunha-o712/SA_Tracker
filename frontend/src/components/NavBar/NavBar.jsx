import "./NavBar.css";

function NavBar() {
  const menus = ["PROFILE", "WEAPONS", "MATCHES", "RANKING", "CLAN"];

  return (
    <nav className="sa-navbar">
      <div className="sa-nav-dot left" />

      {menus.map((menu) => (
        <a key={menu} href="#">
          {menu}
        </a>
      ))}

      <div className="sa-nav-dot right" />
    </nav>
  );
}

export default NavBar;