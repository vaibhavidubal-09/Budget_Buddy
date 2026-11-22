import { getAuthToken } from './authApiService.js'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(getAuthToken() ? { Authorization: `Bearer ${getAuthToken()}` } : {}),
      ...options.headers,
    },
  })

  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || `API request failed (${response.status})`)
  }

  if (response.status === 204) return null
  return response.json()
}

let initialLoadPromise = null

export function getIncomeFromApi() {
  return request('/income')
}

// Share the first-load request so React's development remount cannot import twice.
export function loadIncomeFromApi() {
  if (!initialLoadPromise) {
    initialLoadPromise = getIncomeFromApi().finally(() => { initialLoadPromise = null })
  }

  return initialLoadPromise
}

export function createIncomeInApi(income) {
  const { id, ...incomeData } = income
  return request('/income', { method: 'POST', body: JSON.stringify(incomeData) })
}

export function updateIncomeInApi(income) {
  const { id, ...incomeData } = income
  return request(`/income/${id}`, { method: 'PUT', body: JSON.stringify(incomeData) })
}

export function deleteIncomeFromApi(id) {
  return request(`/income/${id}`, { method: 'DELETE' })
}

export async function syncIncomeChanges(previousIncome, nextIncome) {
  const previousById = new Map(previousIncome.map(entry => [entry.id, entry]))
  const nextById = new Map(nextIncome.map(entry => [entry.id, entry]))
  const operations = []

  for (const entry of nextIncome) {
    const previous = previousById.get(entry.id)
    if (!previous) operations.push(createIncomeInApi(entry))
    else if (hasIncomeChanged(previous, entry)) operations.push(updateIncomeInApi(entry))
  }

  for (const entry of previousIncome) {
    if (!nextById.has(entry.id)) operations.push(deleteIncomeFromApi(entry.id))
  }

  await Promise.all(operations)
  return getIncomeFromApi()
}

function hasIncomeChanged(first, second) {
  return ['source', 'amount', 'date', 'frequency', 'intervalDays'].some(field => String(first[field]) !== String(second[field]))
}
