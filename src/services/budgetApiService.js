import { getAuthToken } from './authApiService.js'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
let initialLoadPromise = null

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
    const responseText = await response.text()
    let message = responseText
    try {
      message = JSON.parse(responseText).message || responseText
    } catch {
      // Keep plain-text API errors readable too.
    }
    throw new Error(message || `API request failed (${response.status})`)
  }

  if (response.status === 204) return null
  return response.json()
}

export function getBudgetsFromApi() {
  return request('/budgets')
}

export function suggestBudgetPlanFromApi(plan) {
  return request('/budgets/suggest-plan', { method: 'POST', body: JSON.stringify(plan) })
}

export function applyBudgetPlanFromApi(plan) {
  return request('/budgets/apply-plan', { method: 'POST', body: JSON.stringify(plan) })
}

// Share the first-load request to avoid duplicate reads during React development remounts.
export function loadBudgetsFromApi() {
  if (!initialLoadPromise) {
    initialLoadPromise = getBudgetsFromApi().finally(() => { initialLoadPromise = null })
  }

  return initialLoadPromise
}

export function createBudgetInApi(budget) {
  const { category, limit, period } = budget
  return request('/budgets', { method: 'POST', body: JSON.stringify({ category, limit, period }) })
}

export function updateBudgetInApi(budget) {
  const { id, category, limit, period, version } = budget
  return request(`/budgets/${id}`, { method: 'PUT', body: JSON.stringify({ category, limit, period, version }) })
}

export function deleteBudgetFromApi(id) {
  return request(`/budgets/${id}`, { method: 'DELETE' })
}

export async function syncBudgetChanges(previousBudgets, nextBudgets) {
  const previousById = new Map(previousBudgets.map(budget => [budget.id, budget]))
  const nextById = new Map(nextBudgets.map(budget => [budget.id, budget]))
  const operations = []

  for (const budget of nextBudgets) {
    const previous = previousById.get(budget.id)
    if (!previous) operations.push(createBudgetInApi(budget))
    else if (hasBudgetChanged(previous, budget)) operations.push(updateBudgetInApi(budget))
  }

  for (const budget of previousBudgets) {
    if (!nextById.has(budget.id)) operations.push(deleteBudgetFromApi(budget.id))
  }

  await Promise.all(operations)
  return getBudgetsFromApi()
}

function hasBudgetChanged(first, second) {
  return ['category', 'limit', 'period'].some(field => String(first[field]) !== String(second[field]))
}
