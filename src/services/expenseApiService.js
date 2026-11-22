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

export function getExpensesFromApi() {
  return request('/expenses')
}

export function getExpensesPageFromApi({ page = 0, size = 20, category, search } = {}) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (category) params.set('category', category)
  if (search?.trim()) params.set('search', search.trim())
  return request(`/expenses/page?${params.toString()}`)
}

export function previewExpenseImport(transactions) {
  return request('/expenses/import/preview', {
    method: 'POST',
    body: JSON.stringify({ transactions: transactions.map(toImportRow) }),
  })
}

export function importExpensesFromApi(transactions) {
  return request('/expenses/import', {
    method: 'POST',
    body: JSON.stringify({ transactions: transactions.map(toImportRow) }),
  })
}

function toImportRow(transaction) {
  return {
    rowId: String(transaction.id),
    description: transaction.description,
    category: transaction.category,
    amount: Number(transaction.amount),
    paymentMethod: transaction.paymentMethod,
    date: transaction.date,
    sourceTransactionId: transaction.sourceTransactionId || null,
  }
}

// Share the first-load request so React's development remount cannot import twice.
export function loadExpensesFromApi() {
  if (!initialLoadPromise) {
    initialLoadPromise = getExpensesFromApi().finally(() => { initialLoadPromise = null })
  }

  return initialLoadPromise
}

export function createExpenseInApi(expense) {
  const { id, idempotencyKey: ignored, ...expenseData } = expense
  // Keep the key on this pending expense object so a retry reuses the same request identity.
  expense.idempotencyKey ||= globalThis.crypto?.randomUUID?.()
    || `expense-${Date.now()}-${Math.random().toString(36).slice(2)}`
  return request('/expenses', {
    method: 'POST',
    headers: { 'Idempotency-Key': expense.idempotencyKey },
    body: JSON.stringify(expenseData),
  })
}

export function updateExpenseInApi(expense) {
  const { id, ...expenseData } = expense
  return request(`/expenses/${id}`, { method: 'PUT', body: JSON.stringify(expenseData) })
}

export function deleteExpenseFromApi(id) {
  return request(`/expenses/${id}`, { method: 'DELETE' })
}

// Apply only the changes between two lists, then return the saved server list.
export async function syncExpenseChanges(previousExpenses, nextExpenses) {
  const previousById = new Map(previousExpenses.map(expense => [expense.id, expense]))
  const nextById = new Map(nextExpenses.map(expense => [expense.id, expense]))
  const operations = []

  for (const expense of nextExpenses) {
    const previous = previousById.get(expense.id)
    if (!previous) {
      operations.push(createExpenseInApi(expense))
    } else if (hasExpenseChanged(previous, expense)) {
      operations.push(updateExpenseInApi(expense))
    }
  }

  for (const expense of previousExpenses) {
    if (!nextById.has(expense.id)) operations.push(deleteExpenseFromApi(expense.id))
  }

  await Promise.all(operations)
  return getExpensesFromApi()
}

function hasExpenseChanged(first, second) {
  return ['description', 'category', 'amount', 'paymentMethod', 'date', 'splitDetails']
    .some(field => String(first[field]) !== String(second[field]))
}
