import { useState } from 'react'
import DashboardLayout from '../layouts/DashboardLayout.jsx'

function getInitials(name) {
  return name.trim().split(/\s+/).slice(0, 2).map(part => part[0]?.toUpperCase() || '').join('') || 'BB'
}

export default function ProfilePage({ theme, onThemeChange, onLogout, profile, onProfileChange }) {
  const [formData, setFormData] = useState(profile)
  const [saved, setSaved] = useState(false)

  function handleChange(event) {
    const { name, value } = event.target
    setFormData({ ...formData, [name]: value })
    setSaved(false)
  }

  function handleSubmit(event) {
    event.preventDefault()
    onProfileChange(formData)
    setSaved(true)
  }

  return (
    <DashboardLayout title="Profile" theme={theme} onThemeChange={onThemeChange} onLogout={onLogout} profile={profile}>
      <section className="profile-welcome"><div className="profile-avatar-large">{getInitials(profile.fullName)}</div><div><p className="dashboard-eyebrow">YOUR BUDGETBUDDY ACCOUNT</p><h1>Your profile</h1><p>Keep the details that make this space yours up to date.</p></div><span className="profile-student-tag">Student account</span></section>
      <section className="profile-panel">
        <div className="profile-panel-heading"><div><h2>Personal details</h2><p>These details are saved in this browser for the demo.</p></div><span className="profile-edit-symbol">&#9998;</span></div>
        <form className="profile-form" onSubmit={handleSubmit}>
          <label className="profile-field"><span>Full name</span><input name="fullName" value={formData.fullName} onChange={handleChange} autoComplete="name" required maxLength={80} /></label>
          <label className="profile-field"><span>Email address</span><input name="email" type="email" value={formData.email} onChange={handleChange} autoComplete="email" required /></label>
          <label className="profile-field"><span>College or university</span><input name="college" value={formData.college} onChange={handleChange} placeholder="Your school" maxLength={100} /></label>
          <label className="profile-field"><span>Program</span><input name="course" value={formData.course} onChange={handleChange} placeholder="Your program" maxLength={80} /></label>
          <label className="profile-field"><span>Study year</span><select name="year" value={formData.year} onChange={handleChange}><option>1st year</option><option>2nd year</option><option>3rd year</option><option>4th year</option><option>Graduate</option></select></label>
          <div className="profile-form-actions">{saved && <span role="status">Profile updated.</span>}<button className="button" type="submit">Save profile</button></div>
        </form>
      </section>
      <p className="dashboard-demo-note">Profile details are saved in this browser. No account service is connected.</p>
    </DashboardLayout>
  )
}
