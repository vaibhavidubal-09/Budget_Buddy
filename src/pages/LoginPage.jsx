import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useNavigate } from 'react-router-dom'
import AuthField from '../components/AuthField.jsx'
import AuthLayout from '../components/AuthLayout.jsx'
import { loginAccount, saveAuthSession } from '../services/authApiService.js'

export default function LoginPage({ theme, onThemeChange, onAuthenticated }) {
  const [message, setMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const navigate = useNavigate()

  async function handleSubmit(event) {
    event.preventDefault()
    setMessage('')
    setIsSubmitting(true)
    const formData = new FormData(event.currentTarget)
    try {
      const response = await loginAccount({ email: formData.get('email'), password: formData.get('password') })
      onAuthenticated(saveAuthSession(response))
      navigate('/dashboard')
    } catch (error) {
      setMessage(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout eyebrow="WELCOME BACK" title="Good to see you." description="Log in and get back to your money picture." theme={theme} onThemeChange={onThemeChange}>
      <form className="auth-form" onSubmit={handleSubmit}>
        <AuthField id="email" label="Email address" type="email" placeholder="you@example.com" autoComplete="email" required />
        <AuthField id="password" label="Password" type="password" placeholder="Enter your password" autoComplete="current-password" required />
        <button className="button auth-submit" type="submit" disabled={isSubmitting}>{isSubmitting ? 'Logging in…' : 'Log in'} <span aria-hidden="true">&#8594;</span></button>
        {message && <p className="auth-message" role="alert">{message}</p>}
        <p className="auth-switch">New to BudgetBuddy? <Link to="/register">Create an account</Link></p>
      </form>
    </AuthLayout>
  )
}
