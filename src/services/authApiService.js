const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
const TOKEN_KEY = 'budgetbuddy-auth-token'
const USER_KEY = 'budgetbuddy-auth-user'

export function getAuthToken() {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function getSignedInUser() {
  try {
    const user = sessionStorage.getItem(USER_KEY)
    return user ? JSON.parse(user) : null
  } catch {
    return null
  }
}

export function saveAuthSession(authResponse) {
  sessionStorage.setItem(TOKEN_KEY, authResponse.token)
  const user = { id: authResponse.id, fullName: authResponse.fullName, email: authResponse.email }
  sessionStorage.setItem(USER_KEY, JSON.stringify(user))
  return user
}

export function clearAuthSession() {
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
}

async function sendAuthRequest(path, body) {
  const response = await fetch(`${API_BASE_URL}/auth/${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })

  const result = await response.json()
  if (!response.ok) throw new Error(result.message || 'Authentication request failed')
  return result
}

export function registerAccount(account) {
  return sendAuthRequest('register', account)
}

export function loginAccount(credentials) {
  return sendAuthRequest('login', credentials)
}
