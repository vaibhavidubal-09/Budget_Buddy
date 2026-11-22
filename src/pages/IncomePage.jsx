import { useState } from 'react'
import DashboardLayout from '../layouts/DashboardLayout.jsx'
import IncomeForm from '../components/IncomeForm.jsx'
import { getIncomeFrequencyLabel, getIncomeTotalForMonth } from '../services/incomeSchedule.js'

function formatAmount(amount) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(amount)
}

function formatDate(date) {
  return new Date(`${date}T00:00:00`).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

export default function IncomePage({ theme, onThemeChange, onLogout, profile, income: incomeList, onIncomeChange, apiStatus = 'offline' }) {
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [editingIncome, setEditingIncome] = useState(null)
  const [notice, setNotice] = useState('')

  const totalIncome = getIncomeTotalForMonth(incomeList)

  function openAddForm() {
    setEditingIncome(null)
    setIsFormOpen(true)
  }

  async function saveIncome(entry) {
    const nextIncome = editingIncome
      ? incomeList.map(item => item.id === editingIncome.id ? { ...entry, id: item.id } : item)
      : [{ ...entry, id: Date.now() }, ...incomeList]
    const saved = await onIncomeChange(nextIncome)
    if (!saved) {
      setNotice('Could not save. Check the backend connection and try again.')
      return
    }
    if (editingIncome) {
      setNotice('Income updated.')
    } else {
      setNotice('Income added.')
    }
    setIsFormOpen(false)
    setEditingIncome(null)
    return true
  }

  async function deleteIncome(entry) {
    if (window.confirm(`Delete income from "${entry.source}"?`)) {
      const saved = await onIncomeChange(incomeList.filter(item => item.id !== entry.id))
      setNotice(saved ? 'Income deleted.' : 'Could not delete. Check the backend connection and try again.')
      return saved
    }
    return false
  }

  return (
    <DashboardLayout title="Income" theme={theme} onThemeChange={onThemeChange} onLogout={onLogout} profile={profile}>
      <section className="expenses-welcome">
        <div><p className="dashboard-eyebrow">EVERY LITTLE WIN COUNTS</p><h1>Your income</h1><p>Keep track of what is coming in and where it comes from.</p></div>
        <button className="button add-expense-button" type="button" onClick={openAddForm} disabled={apiStatus !== 'connected'}><span aria-hidden="true">&#43;</span> Add income</button>
      </section>
      {notice && <p className="expense-success-message" role="status">{notice}</p>}
      {apiStatus === 'offline' && <p className="expense-api-status offline" role="status">Backend unavailable. Income cannot be loaded or saved right now.</p>}

      <section className="expense-stats" aria-label="Income summary">
        <article><span className="expense-stat-icon stat-income">&#8599;</span><div><small>Payments due this month</small><strong>{formatAmount(totalIncome)}</strong></div></article>
        <article><span className="expense-stat-icon stat-lilac">&#9638;</span><div><small>Income entries</small><strong>{incomeList.length}</strong></div></article>
        <article className="expense-allowance-note"><span>&#10033;</span><p><strong>Payment schedule</strong><br />The total includes payments scheduled for this month. Yearly income is counted only in the month it is due.</p></article>
      </section>

      <section className="expense-list-panel">
        <div className="expense-list-heading"><div><h2>Income history</h2><p>{incomeList.length} {incomeList.length === 1 ? 'entry' : 'entries'} in your list</p></div><span className="expense-month-label">October 2026 <b>&#9662;</b></span></div>
        <div className="expense-table-wrap"><table className="expense-table income-table">
          <thead><tr><th>Source</th><th>First payment</th><th>Frequency</th><th>Amount per payment</th><th><span className="sr-only">Actions</span></th></tr></thead>
          <tbody>{incomeList.map(entry => <tr key={entry.id}><td><strong>{entry.source}</strong><small>Income</small></td><td>{formatDate(entry.date)}</td><td>{getIncomeFrequencyLabel(entry)}</td><td className="income-amount">+ {formatAmount(entry.amount)}</td><td><div className="expense-row-actions"><button type="button" aria-label={`Edit ${entry.source}`} disabled={apiStatus !== 'connected'} onClick={() => { setEditingIncome(entry); setIsFormOpen(true) }}>&#9998;</button><button type="button" aria-label={`Delete ${entry.source}`} disabled={apiStatus !== 'connected'} onClick={() => deleteIncome(entry)}>&#10005;</button></div></td></tr>)}</tbody>
        </table>{incomeList.length === 0 && <p className="expense-empty">No income entries yet. Add your first income source above.</p>}</div>
      </section>
      <p className="dashboard-demo-note">{apiStatus === 'connected' ? 'Your income changes sync to the local MySQL database.' : 'Reconnect the backend to load and manage your saved income.'}</p>
      {isFormOpen && <IncomeForm key={editingIncome?.id || 'new-income'} income={editingIncome} onSave={saveIncome} onCancel={() => setIsFormOpen(false)} />}
    </DashboardLayout>
  )
}
