import { getAuthToken } from './authApiService.js'
import { suggestExpenseCategory } from './expenseCategorizer.js'
import { expenseCategories } from '../data/expenseData.js'
import { previewExpenseImport } from './expenseApiService.js'

export async function importTransactions(file) {
  if (!file) throw new Error('Choose a statement first.')
  const extension = file.name.split('.').pop().toLowerCase()
  let transactions
  if (extension === 'csv' || file.type === 'text/csv') transactions = importTransactionsFromCsv(await file.text())
  else if (['pdf', 'png', 'jpg', 'jpeg', 'webp'].includes(extension) || file.type.startsWith('image/')) transactions = await importTransactionsFromDocument(file)
  else throw new Error('Choose a CSV, PDF, PNG, JPG, or WEBP transaction statement.')
  const preview = await previewExpenseImport(transactions)
  const rowStates = new Map(preview.rows.map(row => [row.rowId, row]))
  return transactions.map(transaction => ({ ...transaction, ...(rowStates.get(String(transaction.id)) || {}) }))
}

export function importTransactionsFromCsv(csvText) {
  const rows = parseCsv(csvText)
  if (rows.length < 2) throw new Error('The CSV has no transaction rows.')
  const headers = rows[0].map(normalizeHeader)
  const column = aliases => headers.findIndex(header => aliases.includes(header))
  const dateColumn = column(['date', 'transaction date', 'txn date', 'value date'])
  const descriptionColumn = column(['description', 'narration', 'remarks', 'merchant', 'details', 'transaction details', 'particulars'])
  const amountColumn = column(['amount', 'transaction amount', 'debit amount', 'withdrawal amount'])
  const debitColumn = column(['debit', 'withdrawal', 'withdrawals', 'debit amount'])
  const creditColumn = column(['credit', 'deposit', 'deposits', 'credit amount'])
  const typeColumn = column(['type', 'transaction type', 'dr cr', 'direction'])
  const categoryColumn = column(['category'])
  const paymentColumn = column(['payment method', 'payment mode', 'mode', 'channel'])
  const sourceIdColumn = column(['transaction id', 'transaction reference', 'reference number', 'reference', 'rrn', 'utr'])
  if (dateColumn < 0 || descriptionColumn < 0 || (amountColumn < 0 && debitColumn < 0)) {
    throw new Error('CSV needs Date, Description/Narration, and Amount or Debit columns.')
  }

  const transactions = []
  rows.slice(1).forEach((row, rowIndex) => {
    const description = (row[descriptionColumn] || '').trim()
    const rawDate = (row[dateColumn] || '').trim()
    if (!description || !rawDate) return
    const debitValue = debitColumn >= 0 ? parseMoney(row[debitColumn]) : null
    const creditValue = creditColumn >= 0 ? parseMoney(row[creditColumn]) : null
    const rawAmount = debitColumn >= 0 ? debitValue : parseMoney(row[amountColumn])
    const direction = typeColumn >= 0 ? (row[typeColumn] || '').toLowerCase() : ''
    if (creditValue > 0 && !(debitValue > 0)) return
    if (/(^|\W)(cr|credit|deposit|received|income)(\W|$)/.test(direction)
      && !/(^|\W)(dr|debit|withdraw|paid|purchase)(\W|$)/.test(direction)) return
    if (!Number.isFinite(rawAmount) || rawAmount === 0) return
    const date = parseDate(rawDate)
    if (!date) return
    const importedCategory = categoryColumn >= 0 ? row[categoryColumn]?.trim() : ''
    const category = expenseCategories.includes(importedCategory) ? importedCategory : suggestExpenseCategory(description)
    transactions.push({
      id: `csv-${rowIndex + 1}`,
      date,
      description: description.slice(0, 120),
      amount: Math.abs(rawAmount),
      category,
      paymentMethod: paymentColumn >= 0 ? normalizePaymentMethod(row[paymentColumn]) : inferPaymentMethod(description),
      sourceTransactionId: sourceIdColumn >= 0 ? (row[sourceIdColumn] || '').trim().slice(0, 120) || null : null,
    })
  })
  if (transactions.length === 0) throw new Error('No valid outgoing transactions were found. Check the CSV columns and dates.')
  return transactions
}

function parseCsv(text) {
  const rows = []
  let row = []
  let cell = ''
  let quoted = false
  for (let i = 0; i < text.length; i += 1) {
    const char = text[i]
    if (quoted) {
      if (char === '"' && text[i + 1] === '"') { cell += '"'; i += 1 }
      else if (char === '"') quoted = false
      else cell += char
    } else if (char === '"') quoted = true
    else if (char === ',') { row.push(cell); cell = '' }
    else if (char === '\n' || char === '\r') {
      if (char === '\r' && text[i + 1] === '\n') i += 1
      row.push(cell); cell = ''
      if (row.some(value => value.trim())) rows.push(row)
      row = []
    } else cell += char
  }
  row.push(cell)
  if (row.some(value => value.trim())) rows.push(row)
  return rows
}

function normalizeHeader(value) {
  return String(value || '').replace(/^\uFEFF/, '').trim().toLowerCase().replace(/[_-]+/g, ' ').replace(/\s+/g, ' ')
}

function parseMoney(value) {
  if (value == null || !String(value).trim()) return null
  const cleaned = String(value).replace(/[\u20B9,$\s]/g, '').replace(/,/g, '')
  const parenthesized = cleaned.startsWith('(') && cleaned.endsWith(')')
  const amount = Number(cleaned.replace(/[()]/g, ''))
  return Number.isFinite(amount) ? (parenthesized ? -amount : amount) : null
}

function parseDate(value) {
  const text = String(value || '').trim()
  let match = text.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})/)
  if (match) return validDate(match[1], match[2], match[3])
  match = text.match(/^(\d{1,2})[-/.](\d{1,2})[-/.](\d{4})$/)
  if (match) return validDate(match[3], match[2], match[1])
  return null
}

function validDate(year, month, day) {
  const date = new Date(Date.UTC(Number(year), Number(month) - 1, Number(day)))
  if (date.getUTCFullYear() !== Number(year) || date.getUTCMonth() !== Number(month) - 1 || date.getUTCDate() !== Number(day)) return null
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

function normalizePaymentMethod(value) {
  const text = String(value || '').toLowerCase()
  if (/upi|gpay|google pay|phonepe|paytm/.test(text)) return 'UPI'
  if (/cash/.test(text)) return 'Cash'
  if (/card|credit|debit/.test(text)) return 'Card'
  return 'Other'
}

function inferPaymentMethod(description) {
  return normalizePaymentMethod(description)
}

async function importTransactionsFromDocument(file) {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
  const formData = new FormData()
  formData.append('file', file)
  const token = getAuthToken()
  const response = await fetch(`${apiBaseUrl}/expenses/import-file`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: formData,
  })
  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || `Could not read the document (${response.status})`)
  }
  return (await response.json()).map((transaction, index) => ({ ...transaction, id: `image-import-${index + 1}` }))
}

