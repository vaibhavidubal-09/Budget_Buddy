import { useState } from 'react'
import { Link, NavLink } from 'react-router-dom'
import Brand from '../components/Brand.jsx'

const navigation = [
  { label: 'Overview', icon: String.fromCodePoint(9638), to: '/dashboard' },
  { label: 'Expenses', icon: String.fromCodePoint(8599), to: '/expenses' },
  { label: 'Income', icon: String.fromCodePoint(8601), to: '/income' },
  { label: 'Budgets', icon: String.fromCodePoint(9636), to: '/budgets' },
  { label: 'Analytics', icon: String.fromCodePoint(9637), to: '/analytics' },
  { label: 'AI Assistant', icon: String.fromCodePoint(10033), to: '/assistant' },
  { label: 'Settings', icon: String.fromCodePoint(9881), to: '/settings' },
]

function getInitials(name = 'Aarav Shah') {
  return name.trim().split(/\s+/).slice(0, 2).map(part => part[0]?.toUpperCase() || '').join('') || 'BB'
}

export default function DashboardLayout({ children, title = 'Overview', theme = 'midnight', onThemeChange, onLogout, profile = { fullName: 'Aarav Shah' } }) {
  const [menuOpen, setMenuOpen] = useState(false)
  const initials = getInitials(profile.fullName)

  return (
    <div className="dashboard-shell" data-theme={theme}>
      <aside className={`dashboard-sidebar${menuOpen ? ' dashboard-sidebar-open' : ''}`}>
        <Brand />
        <p className="dashboard-nav-label">YOUR MONEY</p>
        <nav className="dashboard-nav" aria-label="Dashboard navigation">
          {navigation.map(item => item.to ? (
            <NavLink className={({ isActive }) => `dashboard-nav-item${isActive ? ' active' : ''}`} to={item.to} key={item.label} onClick={() => setMenuOpen(false)}>
              <span aria-hidden="true">{item.icon}</span>{item.label}
            </NavLink>
          ) : (
            <button className="dashboard-nav-item" type="button" disabled key={item.label} title="This page is coming in a later step">
              <span aria-hidden="true">{item.icon}</span>{item.label}<small>Later</small>
            </button>
          ))}
        </nav>
        <div className="dashboard-sidebar-bottom">
          <div className="sidebar-tip"><span>&#10033;</span><p><strong>Little steps add up.</strong><br />You're building a good habit.</p></div>
          <Link className="dashboard-user" to="/profile" onClick={() => setMenuOpen(false)} aria-label="Open your profile"><span className="user-avatar">{initials}</span><span><strong>{profile.fullName}</strong><small>Student account</small></span><span className="user-more">&#8942;</span></Link>
          <button className="dashboard-logout" type="button" onClick={onLogout}>Log out</button>
        </div>
      </aside>

      <div className="dashboard-workspace">
        <header className="dashboard-topbar">
          <button className="dashboard-menu-toggle" type="button" aria-label={menuOpen ? 'Close dashboard menu' : 'Open dashboard menu'} aria-expanded={menuOpen} onClick={() => setMenuOpen(!menuOpen)}>
            <span /><span /><span />
          </button>
          <div className="dashboard-breadcrumb"><Link to="/">Home</Link><span>/</span><strong>{title}</strong></div>
          <div className="dashboard-top-actions"><label className="theme-picker"><span className="sr-only">Choose color theme</span><select value={theme} onChange={event => onThemeChange(event.target.value)} aria-label="Choose color theme"><option value="midnight">Midnight</option><option value="campus">Campus</option><option value="lime">Lime pop</option></select></label><span className="dashboard-period">October 2026 <b>&#9662;</b></span><Link className="dashboard-top-avatar" to="/profile" aria-label="Open your profile">{initials}</Link></div>
        </header>
        <main className="dashboard-content">{children}</main>
      </div>
    </div>
  )
}
