import { getAuthToken } from './authApiService.js'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export async function suggestExpenseCategoryFromApi(description) {
  const response = await fetch(`${API_BASE_URL}/expenses/suggest-category`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(getAuthToken() ? { Authorization: `Bearer ${getAuthToken()}` } : {}) },
    body: JSON.stringify({ description }),
  })

  if (!response.ok) throw new Error(`Category suggestion failed (${response.status})`)
  return response.json()
}
