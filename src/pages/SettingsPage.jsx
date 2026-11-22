import { useState } from 'react'
import DashboardLayout from '../layouts/DashboardLayout.jsx'

const themeChoices = [
  { id: 'midnight', name: 'Midnight', note: 'Navy, teal, and lavender', swatches: ['#14202e', '#65d8d0', '#b49cff'] },
  { id: 'campus', name: 'Campus', note: 'Calm student-friendly greens', swatches: ['#edf4e9', '#557e50', '#d8eac9'] },
  { id: 'lime', name: 'Lime pop', note: 'Charcoal with a bright lime accent', swatches: ['#171e19', '#c9ee52', '#8bba3c'] },
]

const preferenceOptions = [
  { key: 'budgetAlerts', title: 'Budget alerts', description: 'Show a reminder when spending gets close to a category limit.' },
  { key: 'weeklySummary', title: 'Weekly summary', description: 'Keep a weekly spending recap preference for this demo.' },
  { key: 'spendingTips', title: 'Spending tips', description: 'Show helpful ideas based on your spending patterns.' },
]

export default function SettingsPage({ theme, onThemeChange, onLogout, settings, onSettingsChange, profile }) {
  const [formData, setFormData] = useState(settings)
  const [saved, setSaved] = useState(false)

  function togglePreference(key) {
    setFormData({ ...formData, [key]: !formData[key] })
    setSaved(false)
  }

  function saveSettings(event) {
    event.preventDefault()
    onSettingsChange(formData)
    setSaved(true)
  }

  return (
    <DashboardLayout title="Settings" theme={theme} onThemeChange={onThemeChange} onLogout={onLogout} profile={profile}>
      <section className="settings-welcome"><div><p className="dashboard-eyebrow">MAKE BUDGETBUDDY YOURS</p><h1>Settings</h1><p>Choose how BudgetBuddy looks and which helpful reminders you prefer.</p></div></section>

      <form className="settings-page-content" onSubmit={saveSettings}>
        <section className="settings-panel">
          <div className="settings-panel-heading"><span className="settings-heading-icon">&#9788;</span><div><h2>Appearance</h2><p>Your theme updates the app right away and is remembered on this browser.</p></div></div>
          <div className="settings-theme-options" role="group" aria-label="Choose an app theme">{themeChoices.map(choice => <button className={`settings-theme-card${theme === choice.id ? ' selected' : ''}`} key={choice.id} type="button" aria-pressed={theme === choice.id} onClick={() => onThemeChange(choice.id)}><span className="settings-theme-preview" style={{ '--swatch-background': choice.swatches[0], '--swatch-primary': choice.swatches[1], '--swatch-secondary': choice.swatches[2] }}><i /><i /><i /></span><span><strong>{choice.name}</strong><small>{choice.note}</small></span><span className="settings-theme-check">&#10003;</span></button>)}</div>
        </section>

        <section className="settings-panel">
          <div className="settings-panel-heading"><span className="settings-heading-icon">&#10022;</span><div><h2>Money reminders</h2><p>These are saved demo preferences. Notifications are not sent yet.</p></div></div>
          <div className="settings-preference-list">{preferenceOptions.map(option => <label className="settings-preference-row" key={option.key}><span><strong>{option.title}</strong><small>{option.description}</small></span><input type="checkbox" checked={Boolean(formData[option.key])} onChange={() => togglePreference(option.key)} /><i aria-hidden="true" /></label>)}</div>
        </section>
        <div className="settings-save-row">{saved && <span role="status">Preferences saved in this browser.</span>}<button className="button" type="submit">Save preferences</button></div>
      </form>
      <p className="dashboard-demo-note">Settings are stored in this browser. There is no account or notification service connected.</p>
    </DashboardLayout>
  )
}
