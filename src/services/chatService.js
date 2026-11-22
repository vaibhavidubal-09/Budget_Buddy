import { getAuthToken } from './authApiService.js'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export async function getAssistantReply(message, history = []) {
  const token = getAuthToken()
  const response = await fetch(`${API_BASE_URL}/assistant/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({
      message,
      history: history.slice(-8).map(item => ({ role: item.role, text: item.text })),
    }),
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || `Assistant request failed (${response.status})`)
  }

  const result = await response.json()
  return result.reply
}