import { Link } from 'react-router-dom'
import SummaryCard from '../components/SummaryCard.jsx'
import DashboardLayout from '../layouts/DashboardLayout.jsx'
import { summaryCards } from '../data/dashboardData.js'
import { calculateDashboardSummary } from '../services/expenseService.js'
import { calculateSafeToSpend } from '../services/financialCalculations.js'
import SafeToSpendCard from '../components/SafeToSpendCard.jsx'
import { getIncomeTotalForMonth } from '../services/incomeSchedule.js'

function formatAmount(amount) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount)
}

function formatDate(date) {
  return new Date(`${date}T00:00:00`).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })
}

function categoryColor(category, index) {
  const knownColors = { 'Food & Dining': 'food', Transportation: 'travel', Education: 'study', Entertainment: 'fun', Shopping: 'shopping', 'Bills & Subscriptions': 'bills', Healthcare: 'health' }
  return knownColors[category] || ['food', 'travel', 'study', 'fun'][index % 4]
}

export default function DashboardPage({ theme, onThemeChange, onLogout, expenses = [], income = [], budgets = [], monthlySummary, summaryApiStatus, profile }) {
  const today = new Date()
  const isCurrentMonth = item => {
    const date = new Date(`${item.date}T00:00:00`)
    return date.getFullYear() === today.getFullYear() && date.getMonth() === today.getMonth()
  }
  const thisMonthExpenses = expenses.filter(isCurrentMonth)
  const thisMonthIncome = getIncomeTotalForMonth(income, today)
  const monthlyBudget = budgets.filter(item => item.period === 'monthly').reduce((sum, item) => sum + Number(item.limit), 0)
  const localSummary = calculateDashboardSummary(thisMonthExpenses, thisMonthIncome, monthlyBudget)
  // Use the backend calculation when it is available; keep local calculation as an offline fallback.
  const summary = summaryApiStatus === 'connected' && monthlySummary
    ? {
        monthlyIncome: Number(monthlySummary.income),
        totalExpenses: Number(monthlySummary.expenses),
        currentBalance: Number(monthlySummary.balance),
        monthlyBudget: Number(monthlySummary.monthlyBudget),
        budgetRemaining: Math.max(0, Number(monthlySummary.monthlyBudget) - Number(monthlySummary.expenses)),
        budgetProgress: monthlySummary.monthlyBudget ? Math.min(100, Number(monthlySummary.expenses) / Number(monthlySummary.monthlyBudget) * 100) : 0,
        categoryTotals: monthlySummary.categorySpending.map(item => ({ category: item.category, amount: Number(item.amount) })).sort((a, b) => b.amount - a.amount),
        recentExpenses: localSummary.recentExpenses,
      }
    : localSummary
  const safeToSpend = calculateSafeToSpend({ expenses: thisMonthExpenses, income, today })
  const cards = summaryCards.map((item, index) => {
    if (index === 0) return { ...item, amount: formatAmount(summary.monthlyIncome), note: 'Across your saved income entries', change: `${income.length} income sources` }
    if (index === 1) return { ...item, amount: formatAmount(summary.totalExpenses), note: `Across ${expenses.length} purchases`, change: summary.categoryTotals[0] ? `${summary.categoryTotals[0].category} is your top spend` : 'No spending recorded yet' }
    if (index === 2) return { ...item, amount: formatAmount(summary.currentBalance), note: summary.currentBalance >= 0 ? 'Money left for this month' : 'More spent than received', change: 'Allowance minus listed expenses' }
    return { ...item, amount: formatAmount(summary.monthlyBudget), note: `${formatAmount(summary.budgetRemaining)} still available`, change: `${Math.round(summary.budgetProgress)}% used`, progress: summary.budgetProgress }
  })

  const categories = summary.categoryTotals
  let lastSegment = 0
  const donutSegments = categories.map((category, index) => {
    const colors = ['#65d8d0', '#b49cff', '#ffad9e', '#ffd280', '#77aaff', '#a9d77a', '#f58ec6']
    const end = lastSegment + (summary.totalExpenses ? category.amount / summary.totalExpenses * 100 : 0)
    const segment = `${colors[index % colors.length]} ${lastSegment}% ${end}%`
    lastSegment = end
    return segment
  })
  const donutStyle = donutSegments.length ? `conic-gradient(${donutSegments.join(', ')})` : '#334143'

  return (
    <DashboardLayout theme={theme} onThemeChange={onThemeChange} onLogout={onLogout} profile={profile}>
      <section className="dashboard-welcome">
        <div><p className="dashboard-eyebrow">YOUR STUDENT MONEY, AT A GLANCE</p><h1>Hey, {profile?.fullName?.split(' ')[0] || 'Aarav'} <span aria-hidden="true">&#10033;</span></h1><p>Here's how your month is looking so far.</p></div>
        <div className="dashboard-action-links"><Link className="dashboard-quick-add" to="/expenses?add=choices">+ Add expense</Link></div>
      </section>
      <section className="summary-grid" aria-label="Monthly money summary">
        {cards.map(item => <SummaryCard item={item} key={item.label} />)}
      </section>
      <SafeToSpendCard calculation={safeToSpend} />
      <section className="spending-overview" aria-label="Spending by category">
        <div className="spending-overview-heading"><div><p className="dashboard-eyebrow">A QUICK LOOK AT YOUR HABITS</p><h2>Spending, at a glance</h2></div><span>This month <b>&#9662;</b></span></div>
        <div className="spending-overview-body"><div className="spending-donut" style={{ background: donutStyle }} role="img" aria-label={`Category spending total ${formatAmount(summary.totalExpenses)}`}><div><small>Spent so far</small><strong>{formatAmount(summary.totalExpenses)}</strong><small>across {categories.length} categories</small></div></div><div className="spending-category-list">{categories.map((category, index) => <div className="spending-category" key={category.category}><span className={`spending-category-dot ${categoryColor(category.category, index)}`} /><div className="spending-category-info"><div><strong>{category.category}</strong><span>{formatAmount(category.amount)}</span></div><div className="spending-track"><span className={categoryColor(category.category, index)} style={{ width: `${summary.totalExpenses ? (category.amount / summary.totalExpenses) * 100 : 0}%` }} /></div></div></div>)}{categories.length === 0 && <p className="dashboard-empty-note">Add an expense to see category insights here.</p>}</div></div>
      </section>
      <section className="dashboard-recent-panel"><div className="dashboard-recent-heading"><div><h2>Recent transactions</h2><p>Your latest recorded expenses</p></div><Link to="/expenses">View all <span aria-hidden="true">&#8599;</span></Link></div>{summary.recentExpenses.length ? <div className="dashboard-recent-list">{summary.recentExpenses.map((expense, index) => <div className="dashboard-recent-row" key={expense.id}><span className={`recent-expense-icon ${categoryColor(expense.category, index)}`} aria-hidden="true">&#9679;</span><span className="recent-expense-name"><strong>{expense.description}</strong><small>{expense.category} &middot; {expense.paymentMethod}</small></span><span className="recent-expense-date">{formatDate(expense.date)}</span><strong className="recent-expense-amount">- {formatAmount(expense.amount)}</strong></div>)}</div> : <p className="dashboard-empty-note">Your new expenses will show up here.</p>}</section>
      <section className="dashboard-next-step"><span className="next-step-icon">&#10033;</span><div><p className="dashboard-eyebrow">A SMARTER LITTLE HABIT</p><h2>Tracking your spending shouldn't feel like work.</h2><p>Choose AI entry, manual entry, or import a statement.</p></div><Link className="next-step-spark" to="/expenses?add=choices" aria-label="Choose how to add an expense">&#8599;</Link></section>
      <p className="dashboard-demo-note">{summaryApiStatus === 'connected' ? 'Monthly totals are calculated by the BudgetBuddy backend from your saved entries.' : 'Showing a local calculation while the backend summary is unavailable.'}</p>
    </DashboardLayout>
  )
}
