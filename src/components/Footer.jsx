import { Link } from 'react-router-dom'
import Brand from './Brand.jsx'

export default function Footer() {
  return (
    <footer className="site-footer">
      <div className="footer-main">
        <div className="footer-brand"><Brand /><p>Your money. Your choices. Your control.</p></div>
        <nav className="footer-links" aria-label="Footer navigation">
          <a href="/#problem">The Problem</a><a href="/#how-it-works">How It Works</a><a href="/#features">Features</a><a href="/#ai-assistant">AI Assistant</a><Link to="/login">Login</Link>
        </nav>
      </div>
      <div className="footer-bottom"><span>&#169; 2026 BudgetBuddy</span><span>Made for student life <b>&#10033;</b></span></div>
    </footer>
  )
}
