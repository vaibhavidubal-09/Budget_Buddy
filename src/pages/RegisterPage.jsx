import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useNavigate } from 'react-router-dom'
import AuthField from '../components/AuthField.jsx'
import AuthLayout from '../components/AuthLayout.jsx'
import { registerAccount, saveAuthSession } from '../services/authApiService.js'

export default function RegisterPage({ theme, onThemeChange, onAuthenticated }) {
  const [message, setMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [password, setPassword] = useState('')
  const navigate = useNavigate()

  const passwordRules = [
    ['At least 8 characters', password.length >= 8],
    ['A lowercase and an uppercase letter', /[a-z]/.test(password) && /[A-Z]/.test(password)],
    ['A number', /[0-9]/.test(password)],
    ['A symbol, such as ! or #', /[^A-Za-z0-9\s]/.test(password)],
  ]
  const passwordIsStrong = passwordRules.every(([, isMet]) => isMet)

  async function handleSubmit(event) {
    event.preventDefault()
    setMessage('')
    setIsSubmitting(true)
    const formData = new FormData(event.currentTarget)
    if (!passwordIsStrong) {
      setMessage('Please make your password stronger by meeting each requirement below.')
      setIsSubmitting(false)
      return
    }
    const fullName = `${formData.get('firstName')} ${formData.get('lastName')}`.trim()
    try {
      const response = await registerAccount({ fullName, email: formData.get('email'), password: formData.get('password') })
      onAuthenticated(saveAuthSession(response))
      navigate('/dashboard')
    } catch (error) {
      setMessage(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout eyebrow="A FRESH START" title="Let's get started." description="Create your space for calmer money habits." theme={theme} onThemeChange={onThemeChange}>
      <form className="auth-form" onSubmit={handleSubmit}>
        <div className="auth-name-row">
          <AuthField id="firstName" label="First name" placeholder="Aarav" autoComplete="given-name" required />
          <AuthField id="lastName" label="Last name" placeholder="Shah" autoComplete="family-name" required />
        </div>
        <AuthField id="email" label="Email address" type="email" placeholder="you@example.com" autoComplete="email" required />
        <AuthField id="password" label="Create a password" type="password" placeholder="Make it hard to guess" autoComplete="new-password" minLength={8} maxLength={72} value={password} onChange={event => setPassword(event.target.value)} aria-describedby="password-guidance" required />
        <div className="password-guidance" id="password-guidance" aria-live="polite">
          <strong>{passwordIsStrong ? 'Strong password' : 'For a stronger password, include:'}</strong>
          <ul>{passwordRules.map(([rule, isMet]) => <li className={isMet ? 'password-rule-met' : ''} key={rule}><span aria-hidden="true">{isMet ? '+' : '-'}</span> {rule}</li>)}</ul>
        </div>
        <button className="button auth-submit" type="submit" disabled={isSubmitting}>{isSubmitting ? 'Creating account…' : 'Create my account'} <span aria-hidden="true">&#8594;</span></button>
        {message && <p className="auth-message" role="alert">{message}</p>}
        <p className="auth-switch">Already have an account? <Link to="/login">Log in</Link></p>
      </form>
    </AuthLayout>
  )
}
