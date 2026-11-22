import Navbar from '../components/Navbar.jsx'
import HeroStory from '../components/HeroStory.jsx'
import ProblemSection from '../components/ProblemSection.jsx'
import HowItWorks from '../components/HowItWorks.jsx'
import DashboardPreview from '../components/DashboardPreview.jsx'
import StudentFeatures from '../components/StudentFeatures.jsx'
import AIAssistantPreview from '../components/AIAssistantPreview.jsx'
import FinalCTA from '../components/FinalCTA.jsx'
import Footer from '../components/Footer.jsx'

export default function Landing({ theme, onThemeChange }) {
  return (
    <div className="landing-page" id="top">
      <Navbar theme={theme} onThemeChange={onThemeChange} />
      <main>
        <HeroStory />
        <ProblemSection />
        <HowItWorks />
        <DashboardPreview />
        <StudentFeatures />
        <AIAssistantPreview />
        <FinalCTA />
      </main>
      <Footer />
    </div>
  )
}
