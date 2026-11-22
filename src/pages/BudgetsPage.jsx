import { useState } from 'react'
import BudgetForm from '../components/BudgetForm.jsx'
import BudgetPlanForm from '../components/BudgetPlanForm.jsx'
import { expenseCategories } from '../data/expenseData.js'
import DashboardLayout from '../layouts/DashboardLayout.jsx'
import { calculateBudgetUsage } from '../services/financialCalculations.js'
import { getExpectedIncomeForPeriod } from '../services/incomeSchedule.js'

function formatAmount(amount) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount)
}

function getPeriodExpenses(expenses, period) {
  const today = new Date()
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  if (period === 'weekly') start.setDate(start.getDate() - 6)

  return expenses.filter(expense => {
    const date = new Date(`${expense.date}T00:00:00`)
    if (period === 'weekly') return date >= start && date <= today
    if (period === 'monthly') return date.getFullYear() === today.getFullYear() && date.getMonth() === today.getMonth()
    return date.getFullYear() === today.getFullYear()
  })
}

function getPeriodIncome(income, period) {
  const today = new Date()
  let start
  let end
  if (period === 'weekly') {
    end = new Date(today.getFullYear(), today.getMonth(), today.getDate())
    start = new Date(end)
    start.setDate(start.getDate() - 6)
  } else if (period === 'monthly') {
    start = new Date(today.getFullYear(), today.getMonth(), 1)
    end = new Date(today.getFullYear(), today.getMonth() + 1, 0)
  } else {
    start = new Date(today.getFullYear(), 0, 1)
    end = new Date(today.getFullYear(), 11, 31)
  }
  const toIso = date => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  return getExpectedIncomeForPeriod(income, period, toIso(start), toIso(end))
}

export default function BudgetsPage({ theme, onThemeChange, onLogout, profile, budgets = [], onBudgetsChange, onBudgetPlanApply, expenses = [], income = [], apiStatus = 'offline' }) {
  const [period, setPeriod] = useState('monthly')
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [isPlanOpen, setIsPlanOpen] = useState(false)
  const [editingBudget, setEditingBudget] = useState(null)
  const [notice, setNotice] = useState('')

  const periodBudgets = budgets.filter(budget => budget.period === period)
  const periodExpenses = getPeriodExpenses(expenses, period)
  const periodIncome = getPeriodIncome(income, period)
  function getSpentForCategory(category) {
    return periodExpenses.filter(expense => expense.category === category).reduce((total, expense) => total + Number(expense.amount), 0)
  }
  const totalBudget = periodBudgets.reduce((sum, budget) => sum + Number(budget.limit), 0)
  const totalSpent = periodBudgets.reduce((sum, budget) => sum + getSpentForCategory(budget.category), 0)
  const remaining = totalBudget - totalSpent
  const categoriesForForm = expenseCategories.filter(category => editingBudget?.category === category || !periodBudgets.some(budget => budget.category === category))

  function openAddForm() {
    setEditingBudget(null)
    setIsFormOpen(true)
  }

  async function saveBudget(values) {
    const next = editingBudget
      ? budgets.map(budget => budget.id === editingBudget.id ? { ...budget, ...values } : budget)
      : [...budgets, { ...values, id: Date.now(), period }]
    if (!await onBudgetsChange(next)) {
      setNotice('Could not save. Check the backend connection and try again.')
      return
    }
    setNotice(editingBudget ? 'Budget updated.' : 'Budget added.')
    setIsFormOpen(false)
    setEditingBudget(null)
    return true
  }

  return (
    <DashboardLayout title="Budgets" theme={theme} onThemeChange={onThemeChange} onLogout={onLogout} profile={profile}>
      <section className="expenses-welcome">
        <div><p className="dashboard-eyebrow">A PLAN FOR WHAT MATTERS</p><h1>Your budgets</h1><p>Set different spending limits for each time period.</p></div>
        <div className="budget-header-actions"><button className="button add-expense-button" type="button" onClick={() => setIsPlanOpen(true)} disabled={apiStatus !== 'connected'}><span aria-hidden="true">&#10033;</span> Plan with AI</button><button className="expense-cancel" type="button" onClick={openAddForm} disabled={categoriesForForm.length === 0 || apiStatus !== 'connected'}>Add manually</button></div>
      </section>
      {notice && <p className="expense-success-message" role="status">{notice}</p>}
      {apiStatus === 'offline' && <p className="expense-api-status offline" role="status">Backend unavailable. Budgets cannot be loaded or saved right now.</p>}
      <section className="budget-period-switcher" aria-label="Choose budget period">
        {[['weekly', 'Week'], ['monthly', 'Month'], ['yearly', 'Year']].map(([value, label]) => <button key={value} className={period === value ? 'active' : ''} type="button" aria-pressed={period === value} onClick={() => { setPeriod(value); setIsFormOpen(false); setIsPlanOpen(false); setEditingBudget(null) }}>{label}</button>)}
      </section>
      <p className="budget-income-context">Expected income this {period}: <strong>{formatAmount(periodIncome)}</strong><small>Recurring income is adjusted to this period, so a yearly payment is not counted as a whole weekly or monthly income.</small></p>
      <section className="budget-summary" aria-label={`${period} budget summary`}>
        <article className="budget-summary-main"><span className="budget-summary-icon">&#10033;</span><div><small>Total {period} budget</small><strong>{formatAmount(totalBudget)}</strong><span>{formatAmount(totalSpent)} spent so far</span></div><div className="budget-summary-ring" style={{ '--budget-progress': `${totalBudget ? Math.min(100, (totalSpent / totalBudget) * 100) : 0}%` }}><span>{totalBudget ? Math.round((totalSpent / totalBudget) * 100) : 0}%<small>used</small></span></div></article>
        <article className="budget-summary-remaining"><small>Still available</small><strong>{formatAmount(remaining)}</strong><span>Across {periodBudgets.length} category {periodBudgets.length === 1 ? 'budget' : 'budgets'}</span></article>
      </section>
      <section className="budget-category-section">
        <div className="budget-section-heading"><div><h2>Category budgets</h2><p>Spending based on your saved {period} expenses</p></div><span className="budget-section-badge">{periodBudgets.length} active</span></div>
        {periodBudgets.length ? <div className="budget-card-grid">{periodBudgets.map(budget => {
          const usage = calculateBudgetUsage(budget, periodExpenses)
          const { spent, percentage: percent, alert } = usage
          const progress = Math.min(percent, 100)
          const overBudget = percent > 100
          return <article className={`budget-category-card${overBudget ? ' over-budget' : ''}`} key={budget.id}>
            <div className="budget-card-top"><span className="budget-card-icon">&#10033;</span><button type="button" aria-label={`Edit ${budget.category} budget`} disabled={apiStatus !== 'connected'} onClick={() => { setEditingBudget(budget); setIsFormOpen(true) }}>Edit</button></div>
            <h3>{budget.category}</h3><div className="budget-values"><strong>{formatAmount(spent)} <small>spent</small></strong><span>of {formatAmount(budget.limit)}</span></div>
            <div className="budget-progress-track" role="progressbar" aria-label={`${budget.category} budget used`} aria-valuenow={Math.round(Math.min(percent, 100))} aria-valuemin="0" aria-valuemax="100"><span style={{ width: `${progress}%` }} /></div>
            <div className="budget-card-foot"><span>{overBudget ? `${formatAmount(spent - budget.limit)} over budget` : `${formatAmount(budget.limit - spent)} left`}</span><span>{Math.round(percent)}%</span></div>
            {alert && <p className={`budget-alert ${alert.level}`} role="status">{alert.message}</p>}
          </article>
        })}</div> : <p className="expense-empty">You haven't set any category budgets yet. Add one to start planning.</p>}
      </section>
      <p className="dashboard-demo-note">Spending progress comes from your saved expenses.</p>
      {isFormOpen && <BudgetForm key={editingBudget?.id || 'new-budget'} budget={editingBudget} categories={categoriesForForm} period={period} onSave={saveBudget} onCancel={() => setIsFormOpen(false)} />}
      {isPlanOpen && <BudgetPlanForm period={period} income={periodIncome} categories={expenseCategories} hasExistingBudgets={periodBudgets.length > 0} onCancel={() => setIsPlanOpen(false)} onApply={async allocations => {
        const result = await onBudgetPlanApply({
          period,
          budgets: allocations.map(allocation => ({ category: allocation.category, limit: Number(allocation.limit) })),
          expectedBudgets: periodBudgets.map(budget => ({ id: budget.id, version: budget.version })),
        })
        if (!result?.success) {
          setNotice(result?.error || 'Could not apply this plan. The saved budgets have been reloaded.')
          return false
        }
        setNotice('Budget plan saved.')
        setIsPlanOpen(false)
        return true
      }} />}
    </DashboardLayout>
  )
}
