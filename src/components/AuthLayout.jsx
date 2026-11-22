import { Link } from 'react-router-dom'
import Brand from './Brand.jsx'

export default function AuthLayout({ eyebrow, title, description, children, theme, onThemeChange }) {
  return (
    <main className="auth-page">
      <aside className="auth-story">
        <Brand />
        <div className="auth-story-copy">
          <p className="auth-eyebrow">MONEY, WITH A LITTLE MORE CLARITY</p>
          <h2>Good habits<br />start <em>small.</em></h2>
          <p>Keep your allowance, plans, and everyday spending in one friendly place.</p>
          <div className="auth-mini-preview">
            <span className="auth-mini-icon">&#8377;</span>
            <span><small>YOUR MONTHLY ALLOWANCE</small><strong>&#8377;5,000</strong></span>
            <span className="auth-mini-spark">&#8599;</span>
          </div>
        </div>
        <p className="auth-story-foot">Built for student life. Designed for real life.</p>
        <span className="auth-circle auth-circle-one" /><span className="auth-circle auth-circle-two" />
      </aside>
      <section className="auth-main">
        <label className="auth-theme-picker"><span className="sr-only">Choose color theme</span><select value={theme} onChange={event => onThemeChange(event.target.value)} aria-label="Choose color theme"><option value="midnight">Midnight</option><option value="campus">Campus</option><option value="lime">Lime pop</option></select></label>
        <Link className="auth-back" to="/">&#8592; Back to BudgetBuddy</Link>
        <div className="auth-form-wrap">
          <p className="eyebrow">{eyebrow}</p>
          <h1>{title}</h1>
          <p className="auth-description">{description}</p>
          {children}
        </div>
        <p className="auth-footer-note">Your money details stay yours.</p>
      </section>
    </main>
  )
}
