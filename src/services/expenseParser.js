import { getAuthToken } from './authApiService.js'

function getToday() {
  const today = new Date()
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export async function parseExpenseWithAI(text) {
  const token = getAuthToken()
  const response = await fetch(API_BASE_URL + '/expenses/understand', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ description: text, today: getToday() }),
  })

  const result = await response.json()
  if (!response.ok) throw new Error(result.message || 'AI could not understand that expense.')
  return result
}
