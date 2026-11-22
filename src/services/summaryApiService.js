import { getAuthToken } from './authApiService.js'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export async function getMonthlySummaryFromApi() {
  const response = await fetch(`${API_BASE_URL}/summary/monthly`, { headers: getAuthToken() ? { Authorization: `Bearer ${getAuthToken()}` } : {} })
  if (!response.ok) throw new Error(`Could not load monthly summary (${response.status})`)
  return response.json()
}

export async function getPeriodSummaryFromApi(period) {
  const token = getAuthToken()
  const response = await fetch(`${API_BASE_URL}/summary/period?period=${encodeURIComponent(period)}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  if (!response.ok) throw new Error(`Could not load ${period} summary (${response.status})`)
  return response.json()
}
