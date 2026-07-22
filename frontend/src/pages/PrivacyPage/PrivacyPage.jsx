import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import NavBar from '../../components/NavBar/NavBar'
import PrivacyPolicyContent from '../../components/PrivacyPolicy/PrivacyPolicyContent'
import './PrivacyPage.css'

function PrivacyPage() {
  return (
    <div className="privacy-shell">
      <Header />
      <NavBar />
      <main className="privacy-page">
        <PrivacyPolicyContent />
      </main>
      <Footer />
    </div>
  )
}

export default PrivacyPage
