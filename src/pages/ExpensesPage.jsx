import { Fragment, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import DashboardLayout from '../layouts/DashboardLayout.jsx'
import ExpenseForm from '../components/ExpenseForm.jsx'
import SmartExpenseEntry from '../components/SmartExpenseEntry.jsx'
import { expenseCategories } from '../data/expenseData.js'
import { addExpense } from '../services/expenseService.js'
import { getExpensesPageFromApi } from '../services/expenseApiService.js'

const EXPENSE_PAGE_SIZE = 20

function formatAmount(amount) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(amount)
}

function formatDate(date) {
  return new Date(`${date}T00:00:00`).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

function getSplit(expense) {
  try { return expense.splitDetails ? JSON.parse(expense.splitDetails) : null } catch { return null }
}

function payViaUpi(expense, split, person) {
  const payer = split.participants[split.payerIndex]
  if (!payer?.upiId) {
    window.alert('Add the payer’s UPI ID by editing this expense, then try again.')
    return
  }
  const uri = `upi://pay?pa=${encodeURIComponent(payer.upiId)}&pn=${encodeURIComponent(payer.name)}&am=${Number(person.share).toFixed(2)}&cu=INR&tn=${encodeURIComponent(`Split: ${expense.description}`)}`
  window.location.href = uri
}

export default function ExpensesPage({ theme, onThemeChange, onLogout, expenses, onExpensesChange, onImportExpenses, apiStatus = 'offline', profile }) {
  const [searchParams, setSearchParams] = useSearchParams()
  const [searchTerm, setSearchTerm] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('All categories')
  const [isSmartEntryOpen, setIsSmartEntryOpen] = useState(false)
  const [isEditFormOpen, setIsEditFormOpen] = useState(false)
  const [editingExpense, setEditingExpense] = useState(null)
  const [successMessage, setSuccessMessage] = useState('')
  const [serverPage, setServerPage] = useState(null)
  const [serverPageIndex, setServerPageIndex] = useState(0)
  const [pageLoading, setPageLoading] = useState(false)
  const [pageError, setPageError] = useState('')

  const requestedMode = searchParams.get('add')
  useEffect(() => {
    if (requestedMode) setIsSmartEntryOpen(true)
  }, [requestedMode])

  useEffect(() => {
    if (apiStatus !== 'connected') return undefined
    let ignoreResponse = false
    const timer = window.setTimeout(async () => {
      setPageLoading(true)
      setPageError('')
      try {
        const result = await getExpensesPageFromApi({
          page: serverPageIndex,
          size: EXPENSE_PAGE_SIZE,
          category: categoryFilter === 'All categories' ? undefined : categoryFilter,
          search: searchTerm,
        })
        if (!ignoreResponse) setServerPage(result)
      } catch {
        if (!ignoreResponse) setPageError('Could not load this page of expenses. Please try again.')
      } finally {
        if (!ignoreResponse) setPageLoading(false)
      }
    }, 250)
    return () => { ignoreResponse = true; window.clearTimeout(timer) }
  }, [apiStatus, serverPageIndex, categoryFilter, searchTerm, expenses])

  const filteredExpenses = expenses.filter(expense => {
    const matchesSearch = `${expense.description} ${expense.category} ${expense.paymentMethod}`.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesCategory = categoryFilter === 'All categories' || expense.category === categoryFilter
    return matchesSearch && matchesCategory
  })
  const visibleExpenses = apiStatus === 'connected' ? (serverPage?.items || []) : filteredExpenses

  const totalSpent = expenses.reduce((total, expense) => total + Number(expense.amount), 0)

  function openAddForm() {
    setEditingExpense(null)
    setSuccessMessage('')
    setIsSmartEntryOpen(true)
  }

  function openEditForm(expense) {
    setEditingExpense(expense)
    setIsEditFormOpen(true)
  }

  async function saveExpense(expenseOrList) {
    if (!editingExpense && Array.isArray(expenseOrList)) {
      const result = await onImportExpenses(expenseOrList)
      if (!result) {
        setSuccessMessage('Could not import transactions. Check the backend connection and try again.')
        return
      }
      setSuccessMessage(`${result.importedCount} transactions added; ${result.duplicateCount} duplicates skipped.`)
      setIsSmartEntryOpen(false)
      if (searchParams.has('add')) setSearchParams({}, { replace: true })
      return true
    }
    const nextExpenses = editingExpense
      ? expenses.map(item => item.id === editingExpense.id ? { ...expenseOrList, id: item.id } : item)
      : addExpense(expenses, expenseOrList)
    const saved = await onExpensesChange(nextExpenses)
    if (!saved) {
      setSuccessMessage('Could not save. Check the backend connection and try again.')
      return
    }
    if (editingExpense) {
      setSuccessMessage('Expense updated.')
    } else {
      setSuccessMessage('Expense added.')
    }
    setIsSmartEntryOpen(false)
    setIsEditFormOpen(false)
    setEditingExpense(null)
    if (searchParams.has('add')) setSearchParams({}, { replace: true })
    return true
  }

  async function deleteExpense(expense) {
    const confirmed = window.confirm(`Delete "${expense.description}"?`)
    if (confirmed) {
      const saved = await onExpensesChange(expenses.filter(item => item.id !== expense.id))
      setSuccessMessage(saved ? 'Expense deleted.' : 'Could not delete. Check the backend connection and try again.')
      return saved
    }
    return false
  }

  async function toggleSplitPaid(expense, participantIndex) {
    const split = getSplit(expense)
    if (!split) return
    const participants = split.participants.map((person, index) => index === participantIndex
      ? { ...person, paid: !person.paid }
      : person)
    const nextExpenses = expenses.map(item => item.id === expense.id
      ? { ...item, splitDetails: JSON.stringify({ ...split, participants }) }
      : item)
    const saved = await onExpensesChange(nextExpenses)
    setSuccessMessage(saved
      ? `${participants[participantIndex].name} payment status updated.`
      : 'Could not update payment status. Check the backend connection and try again.')
  }

  return (
    <DashboardLayout title="Expenses" theme={theme} onThemeChange={onThemeChange} onLogout={onLogout} profile={profile}>
      <section className="expenses-welcome">
        <div><p className="dashboard-eyebrow">THE LITTLE THINGS, ALL IN ONE PLACE</p><h1>Your expenses</h1><p>Every purchase tells you a little more about your habits.</p></div>
        <button className="button add-expense-button" type="button" onClick={openAddForm} disabled={apiStatus !== 'connected'}><span aria-hidden="true">&#43;</span> Add expense</button>
      </section>
      <p className={`expense-api-status ${apiStatus}`} role="status">
        {apiStatus === 'connected' ? 'Connected to the backend. Expenses are saved in MySQL.' : apiStatus === 'checking' ? 'Connecting to the BudgetBuddy backend…' : 'Backend unavailable. Expenses cannot be loaded or saved right now.'}
      </p>
      {successMessage && <p className="expense-success-message" role="status">✓ {successMessage}</p>}

      <section className="expense-stats" aria-label="Expense summary">
        <article><span className="expense-stat-icon stat-peach">&#8599;</span><div><small>Spent this month</small><strong>{formatAmount(totalSpent)}</strong></div></article>
        <article><span className="expense-stat-icon stat-lilac">&#9638;</span><div><small>Transactions</small><strong>{expenses.length}</strong></div></article>
        <article className="expense-allowance-note"><span>&#10033;</span><p><strong>You're keeping track.</strong><br />That is how better habits start.</p></article>
      </section>

      <section className="expense-list-panel">
        <div className="expense-list-heading"><div><h2>All expenses</h2><p>{serverPage ? `${serverPage.totalElements} matching transactions` : `${filteredExpenses.length} ${filteredExpenses.length === 1 ? 'purchase' : 'purchases'} in your list`}</p></div><span className="expense-month-label">October 2026 <b>&#9662;</b></span></div>
        <div className="expense-filters">
          <label className="expense-search"><span aria-hidden="true">&#9906;</span><input type="search" value={searchTerm} onChange={event => { setSearchTerm(event.target.value); setServerPageIndex(0) }} placeholder="Search expenses" aria-label="Search expenses" /></label>
          <label className="category-filter"><span>Category</span><select value={categoryFilter} onChange={event => { setCategoryFilter(event.target.value); setServerPageIndex(0) }}><option>All categories</option>{expenseCategories.map(category => <option key={category}>{category}</option>)}</select></label>
        </div>
        {pageError && <p className="form-error" role="alert">{pageError}</p>}
        {pageLoading && <p className="expense-page-status" role="status">Loading expenses…</p>}
        <div className="expense-table-wrap">
          <table className="expense-table">
            <thead><tr><th>Expense</th><th>Category</th><th>Payment</th><th>Date</th><th>Amount</th><th><span className="sr-only">Actions</span></th></tr></thead>
            <tbody>
              {visibleExpenses.map(expense => (
                <Fragment key={expense.id}>
                <tr key={expense.id}>
                  <td><strong>{expense.description}</strong><small>{expense.category}</small>{getSplit(expense) && <small>Split bill · {getSplit(expense).participants.length} people</small>}</td>
                  <td><span className="expense-category-tag">{expense.category}</span></td>
                  <td>{expense.paymentMethod}</td><td>{formatDate(expense.date)}</td>
                  <td className="expense-amount">{formatAmount(expense.amount)}</td>
                  <td><div className="expense-row-actions"><button type="button" aria-label={`Edit ${expense.description}`} disabled={apiStatus !== 'connected'} onClick={() => openEditForm(expense)}>&#9998;</button><button type="button" aria-label={`Delete ${expense.description}`} disabled={apiStatus !== 'connected'} onClick={() => deleteExpense(expense)}>&#10005;</button></div></td>
                </tr>
                {getSplit(expense) && <tr className="expense-split-details-row" key={`${expense.id}-split`}><td colSpan="6"><strong>Who owes the payer</strong><div className="expense-split-due-list">{getSplit(expense).participants.map((person, index) => index === getSplit(expense).payerIndex ? null : <span key={index}>{person.name}: {formatAmount(Number(person.share))} {person.paid ? <><strong className="expense-split-paid">Paid</strong><button type="button" className="expense-upi-button expense-upi-secondary" disabled={apiStatus !== 'connected'} onClick={() => toggleSplitPaid(expense, index)}>Undo</button></> : <><button type="button" className="expense-upi-button" onClick={() => payViaUpi(expense, getSplit(expense), person)}>Pay via UPI</button><button type="button" className="expense-upi-button expense-upi-secondary" disabled={apiStatus !== 'connected'} onClick={() => toggleSplitPaid(expense, index)}>Mark paid</button></>}</span>)}</div><small>Payment status is updated manually; BudgetBuddy cannot confirm a UPI transfer.</small></td></tr>}
                </Fragment>
              ))}
            </tbody>
          </table>
          {!pageLoading && visibleExpenses.length === 0 && <p className="expense-empty">No expenses match that search. Try another word or category.</p>}
        </div>
        {serverPage && serverPage.totalPages > 1 && <nav className="expense-pagination" aria-label="Expense pages"><button type="button" disabled={serverPage.page === 0 || pageLoading} onClick={() => setServerPageIndex(current => Math.max(0, current - 1))}>Previous</button><span>Page {serverPage.page + 1} of {serverPage.totalPages}</span><button type="button" disabled={serverPage.page + 1 >= serverPage.totalPages || pageLoading} onClick={() => setServerPageIndex(current => current + 1)}>Next</button></nav>}
      </section>
      <p className="dashboard-demo-note">{apiStatus === 'connected' ? 'Your expense changes sync to the local MySQL database.' : 'Reconnect the backend to load and manage your saved expenses.'}</p>
      {isSmartEntryOpen && <SmartExpenseEntry key={requestedMode || 'choices'} initialMode={requestedMode} onSave={saveExpense} onClose={() => { setIsSmartEntryOpen(false); setSearchParams({}, { replace: true }) }} />}
      {isEditFormOpen && <ExpenseForm key={editingExpense?.id || 'edit-expense'} expense={editingExpense} onSave={saveExpense} onCancel={() => { setIsEditFormOpen(false); setEditingExpense(null) }} />}
    </DashboardLayout>
  )
}
