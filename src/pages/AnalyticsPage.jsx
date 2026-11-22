import { useEffect, useState } from 'react'
import DashboardLayout from '../layouts/DashboardLayout.jsx'
import { getPeriodSummaryFromApi } from '../services/summaryApiService.js'

function formatAmount(amount) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount)
}

function formatDifference(current, previous) {
  const difference = current - previous
  return `${difference > 0 ? '+' : ''}${formatAmount(difference)}`
}

function getPeriodBounds(period, today, offset = 0) {
  if (period === 'weekly') {
    const start = new Date(today.getFullYear(), today.getMonth(), today.getDate())
    const daysSinceMonday = (start.getDay() + 6) % 7
    start.setDate(start.getDate() - daysSinceMonday - offset * 7)
    const end = new Date(start)
    end.setDate(start.getDate() + 6)
    return { start, end }
  }
  if (period === 'monthly') {
    const start = new Date(today.getFullYear(), today.getMonth() - offset, 1)
    return { start, end: new Date(start.getFullYear(), start.getMonth() + 1, 0) }
  }
  const start = new Date(today.getFullYear() - offset, 0, 1)
  return { start, end: new Date(start.getFullYear(), 11, 31) }
}

function toIsoDate(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function formatPeriodLabel(period, date) {
  if (period === 'weekly') return date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })
  if (period === 'monthly') return date.toLocaleDateString('en-IN', { month: 'short' })
  return String(date.getFullYear())
}

const categoryColors = {
  'Food & Dining': 'food',
  Transportation: 'travel',
  Education: 'study',
  'Bills & Subscriptions': 'bills',
  Entertainment: 'fun',
  Shopping: 'shopping',
  Healthcare: 'health',
  Other: 'fun',
}

export default function AnalyticsPage({ theme, onThemeChange, onLogout, profile, expenses = [] }) {
  const [period, setPeriod] = useState('monthly')
  const [serverSummary, setServerSummary] = useState(null)
  const [summaryError, setSummaryError] = useState('')
  const [shareStatus, setShareStatus] = useState('')
  const today = new Date()
  useEffect(() => {
    let active = true
    setServerSummary(null)
    setSummaryError('')
    getPeriodSummaryFromApi(period)
      .then(summary => { if (active) setServerSummary(summary) })
      .catch(() => { if (active) setSummaryError('Could not load verified totals from the server. Please check your connection and try again.') })
    return () => { active = false }
  }, [period])
  const localPeriodBounds = getPeriodBounds(period, today)
  const periodStart = serverSummary ? new Date(`${serverSummary.startDate}T00:00:00`) : localPeriodBounds.start
  const periodEnd = serverSummary ? new Date(`${serverSummary.endDate}T00:00:00`) : localPeriodBounds.end
  const startIso = toIsoDate(periodStart)
  const endIso = toIsoDate(periodEnd)
  const currentExpenses = expenses.filter(item => item.date >= startIso && item.date <= endIso)
  const expenseTotal = Number(serverSummary?.expenses ?? 0)
  const incomeTotal = Number(serverSummary?.income ?? 0)
  const categorySpending = (serverSummary?.categorySpending || []).map(item => ({
    ...item, amount: Number(item.amount), color: categoryColors[item.category] || 'fun',
  })).sort((a, b) => b.amount - a.amount)
  const categoryTotal = categorySpending.reduce((sum, item) => sum + item.amount, 0)
  const chartColors = { food: '#65d8d0', travel: '#b49cff', study: '#ffad9e', bills: '#ffd280', fun: '#77aaff', shopping: '#a9d77a', health: '#f58ec6' }
  let currentAngle = 0
  const donutSlices = categorySpending.map(item => {
    const start = currentAngle
    currentAngle += categoryTotal ? item.amount / categoryTotal * 100 : 0
    return `${chartColors[item.color]} ${start}% ${currentAngle}%`
  }).join(', ')
  const trends = (serverSummary?.trends || []).map(item => ({
    label: formatPeriodLabel(period, new Date(`${item.startDate}T00:00:00`)),
    expenses: Number(item.expenses),
    income: Number(item.income),
  }))
  const maxPeriodValue = Math.max(1, ...trends.map(item => Math.max(item.income, item.expenses)))
  const budgetUsage = (serverSummary?.budgetUsage || []).map(item => ({
    category: item.category,
    limit: Number(item.limit),
    spent: Number(item.spent),
    remaining: Number(item.remaining),
  }))
  const periodLabel = period[0].toUpperCase() + period.slice(1)
  const periodName = period === 'weekly' ? 'Weekly' : period === 'monthly' ? 'Monthly' : 'Yearly'
  const formattedRange = `${periodStart.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })} - ${periodEnd.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}`
  const reportText = [
    `BudgetBuddy ${periodName.toLowerCase()} spending report (${formattedRange})`,
    `Estimated income: ${formatAmount(incomeTotal)}`,
    `Expenses: ${formatAmount(expenseTotal)}`,
    `Left after expenses: ${formatAmount(incomeTotal - expenseTotal)}`,
    'Spending by category:',
    ...categorySpending.map(item => `- ${item.category}: ${formatAmount(item.amount)}`),
  ].join('\n')

  async function shareReport() {
    setShareStatus('')
    try {
      if (navigator.share) {
        await navigator.share({ title: `BudgetBuddy ${periodName.toLowerCase()} spending report`, text: reportText })
        setShareStatus('Report shared.')
      } else {
        await navigator.clipboard.writeText(reportText)
        setShareStatus('Summary copied. Paste it into WhatsApp or a message to your parents.')
      }
    } catch (error) {
      if (error.name === 'AbortError') return
      try {
        await navigator.clipboard.writeText(reportText)
        setShareStatus('Summary copied. Paste it into WhatsApp or a message to your parents.')
      } catch {
        setShareStatus('Could not copy the summary. Use Print / Save as PDF instead.')
      }
    }
  }

  return (
    <DashboardLayout title="Analytics" theme={theme} onThemeChange={onThemeChange} onLogout={onLogout} profile={profile}>
      <section className="expenses-welcome analytics-welcome">
        <div><p className="dashboard-eyebrow">YOUR MONEY, IN PATTERNS</p><h1>Spending analytics</h1><p>See your income, spending, and budgets for a week, month, or year.</p></div>
      </section>
      <section className="budget-period-switcher analytics-period-switcher" aria-label="Choose analytics period">
        {[['weekly', 'Week'], ['monthly', 'Month'], ['yearly', 'Year']].map(([value, label]) => <button key={value} className={period === value ? 'active' : ''} type="button" aria-pressed={period === value} onClick={() => setPeriod(value)}>{label}</button>)}
      </section>
      <p className="analytics-period-caption">Server-calculated totals for your selected period.</p>
      {summaryError && <p className="form-error" role="alert">{summaryError}</p>}
      {serverSummary && <p className="analytics-period-caption">Change vs previous period: income {formatDifference(incomeTotal, Number(serverSummary.previousIncome))}; expenses {formatDifference(expenseTotal, Number(serverSummary.previousExpenses))}.</p>}
      <div className="analytics-report-actions"><button className="button" type="button" onClick={() => window.print()}>Print / Save as PDF</button><button className="expense-cancel" type="button" onClick={shareReport}>Share summary</button></div>
      {shareStatus && <p className="analytics-share-status" role="status">{shareStatus}</p>}
      <section className="analytics-summary" aria-label={`This ${period} summary`}>
        <article><span className="analytics-summary-icon income">&#8595;</span><div><small>Income</small><strong>{serverSummary ? formatAmount(incomeTotal) : "Loading..."}</strong></div></article>
        <article><span className="analytics-summary-icon expenses">&#8599;</span><div><small>Expenses</small><strong>{serverSummary ? formatAmount(expenseTotal) : "Loading..."}</strong></div></article>
        <article><span className="analytics-summary-icon savings">&#10003;</span><div><small>Left after expenses</small><strong>{serverSummary ? formatAmount(Number(serverSummary.balance)) : "Loading..."}</strong></div></article>
      </section>
      <section className="analytics-grid">
        <article className="analytics-panel category-chart-panel">
          <div className="analytics-panel-heading"><div><h2>Spending by category</h2><p>Where your money went this {period}</p></div><span>{periodLabel.toUpperCase()}</span></div>
          <div className="category-chart-content"><div className="analytics-donut" style={{ background: categoryTotal ? `conic-gradient(${donutSlices})` : 'var(--surface-raised)' }} role="img" aria-label={`Spending total ${formatAmount(categoryTotal)} by category`}><div><small>Total spent</small><strong>{formatAmount(categoryTotal)}</strong></div></div>
            <div className="analytics-category-list">{categorySpending.length ? categorySpending.map(item => <div className="analytics-category-row" key={item.category}><span className={`analytics-legend-dot ${item.color}`} /><span className="analytics-category-name">{item.category}</span><strong>{formatAmount(item.amount)}</strong></div>) : <p className="expense-empty">No expenses recorded this {period} yet.</p>}</div></div>
        </article>
        <article className="analytics-panel monthly-chart-panel">
          <div className="analytics-panel-heading"><div><h2>{periodLabel} spending trend</h2><p>Last six {period === 'weekly' ? 'weeks' : period === 'monthly' ? 'months' : 'years'}</p></div><span>6 {period === 'weekly' ? 'WEEKS' : period === 'monthly' ? 'MONTHS' : 'YEARS'}</span></div>
          <div className="monthly-chart" role="img" aria-label={`Bar chart showing expenses over the last six ${period === 'weekly' ? 'weeks' : period === 'monthly' ? 'months' : 'years'}`}>
            {trends.length ? trends.map((item, index) => <div className="monthly-chart-column" key={`${item.label}-${index}`}><div className="monthly-bar-area"><span className="monthly-bar" style={{ height: `${(item.expenses / maxPeriodValue) * 100}%` }} title={`${item.label}: ${formatAmount(item.expenses)}`} /></div><small>{item.label}</small></div>) : <p className="expense-empty">Loading trend data...</p>}
          </div><div className="monthly-chart-caption"><span className="analytics-legend-dot food" />Expenses per {period === 'weekly' ? 'week' : period === 'monthly' ? 'month' : 'year'}</div>
        </article>
        <article className="analytics-panel comparison-panel">
          <div className="analytics-panel-heading"><div><h2>Income vs expenses</h2><p>Compare money in and out by {period === 'weekly' ? 'week' : period === 'monthly' ? 'month' : 'year'}</p></div><span>INR</span></div>
          <div className="comparison-legend"><span><i className="comparison-income" />Income</span><span><i className="comparison-expenses" />Expenses</span></div>
          <div className="comparison-chart" role="img" aria-label={`Grouped bar chart comparing income and expenses over the last six ${period === 'weekly' ? 'weeks' : period === 'monthly' ? 'months' : 'years'}`}>
            {trends.map((item, index) => <div className="comparison-month" key={`${item.label}-${index}`}><div className="comparison-bars"><span className="comparison-bar comparison-income" style={{ height: `${(item.income / maxPeriodValue) * 100}%` }} title={`${item.label} income: ${formatAmount(item.income)}`} /><span className="comparison-bar comparison-expenses" style={{ height: `${(item.expenses / maxPeriodValue) * 100}%` }} title={`${item.label} expenses: ${formatAmount(item.expenses)}`} /></div><small>{item.label}</small></div>)}
          </div>
        </article>
        <article className="analytics-panel usage-panel">
          <div className="analytics-panel-heading"><div><h2>Budget usage</h2><p>How much of each category limit is used</p></div><span>{periodLabel.toUpperCase()}</span></div>
          <div className="usage-list">{!serverSummary ? <p className="expense-empty">Loading budget usage...</p> : budgetUsage.length ? budgetUsage.map(item => { const percent = item.limit ? Math.round((item.spent / item.limit) * 100) : 0; return <div className="usage-row" key={item.category}><div><strong>{item.category}</strong><span>{formatAmount(item.spent)} of {formatAmount(item.limit)} · {formatAmount(item.remaining)} remaining</span></div><div className="usage-track" role="progressbar" aria-label={`${item.category} budget used`} aria-valuenow={Math.min(percent, 100)} aria-valuemin="0" aria-valuemax="100"><i style={{ width: `${Math.min(percent, 100)}%` }} /></div>{item.remaining < 0 && <p className="budget-overrun-alert" role="alert">Over budget by {formatAmount(Math.abs(item.remaining))}.</p>}</div> }) : <p className="expense-empty">No {period} category budgets yet. Create a plan on the Budgets page.</p>}</div>
        </article>
      </section>
      <p className="dashboard-demo-note">Summary cards, category totals, six-period trends, and budget usage come from authenticated backend calculations. The printable transaction detail table uses the expense list loaded by the client.</p>
      <section className="analytics-print-report" aria-label="Printable spending report">
        <header><div className="analytics-print-brand">BudgetBuddy</div><h1>{periodName} Spending Report</h1><p>{formattedRange}</p></header>
        <div className="analytics-print-summary"><div><span>Estimated income</span><strong>{formatAmount(incomeTotal)}</strong></div><div><span>Total expenses</span><strong>{formatAmount(expenseTotal)}</strong></div><div><span>Left after expenses</span><strong>{formatAmount(incomeTotal - expenseTotal)}</strong></div></div>
        <h2>Spending by category</h2>
        {categorySpending.length ? <table><thead><tr><th>Category</th><th>Amount</th><th>Share</th></tr></thead><tbody>{categorySpending.map(item => <tr key={item.category}><td>{item.category}</td><td>{formatAmount(item.amount)}</td><td>{categoryTotal ? `${Math.round(item.amount / categoryTotal * 100)}%` : '0%'}</td></tr>)}</tbody></table> : <p>No expenses recorded for this period.</p>}
        <h2>Expense details</h2>
        {currentExpenses.length ? <table><thead><tr><th>Date</th><th>Description</th><th>Category</th><th>Paid with</th><th>Amount</th></tr></thead><tbody>{[...currentExpenses].sort((a, b) => b.date.localeCompare(a.date)).map(item => <tr key={item.id}><td>{new Date(`${item.date}T00:00:00`).toLocaleDateString('en-IN')}</td><td>{item.description}</td><td>{item.category}</td><td>{item.paymentMethod || '-'}</td><td>{formatAmount(Number(item.amount))}</td></tr>)}</tbody></table> : <p>No expenses recorded for this period.</p>}
        <footer>Generated by BudgetBuddy. Income is estimated from the schedules saved in the app.</footer>
      </section>
    </DashboardLayout>
  )
}
