import { useState } from 'react'
import { Link } from 'react-router-dom'
import Brand from './Brand.jsx'

export default function Navbar({ theme, onThemeChange }) {
  const [menuOpen, setMenuOpen] = useState(false)
  const closeMenu = () => setMenuOpen(false)

  return (
    <header className="site-header">
      <nav className="navbar" aria-label="Main navigation">
        <div className="brand-lockup"><Brand /><span>Know where your money goes before it's gone.</span></div>
        <button className={`menu-toggle${menuOpen ? ' is-open' : ''}`} type="button" aria-label={menuOpen ? 'Close navigation menu' : 'Open navigation menu'} aria-expanded={menuOpen} onClick={() => setMenuOpen(!menuOpen)}>
          <span /><span /><span />
        </button>
        <div className={`nav-content${menuOpen ? ' nav-content-open' : ''}`}>
          <div className="nav-links">
            <a href="#problem" onClick={closeMenu}>The Problem</a>
            <a href="#how-it-works" onClick={closeMenu}>How It Works</a>
            <a href="#features" onClick={closeMenu}>Features</a>
            <a href="#ai-assistant" onClick={closeMenu}>AI Assistant</a>
          </div>
          <div className="nav-actions">
            <label className="site-theme-picker"><span className="sr-only">Choose color theme</span><select value={theme} onChange={event => onThemeChange(event.target.value)} aria-label="Choose color theme"><option value="midnight">Midnight</option><option value="campus">Campus</option><option value="lime">Lime pop</option></select></label>
            <Link className="nav-login" to="/login" onClick={closeMenu}>Login</Link>
            <Link className="button button-small" to="/register" onClick={closeMenu}>Get Started <span aria-hidden="true">&#8599;</span></Link>
          </div>
        </div>
      </nav>
    </header>
  )
}
