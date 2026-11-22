import { getIncomeTotalForMonth } from './incomeSchedule.js'
function getMonthEntries(entries, today) {
  return entries.filter(entry => {
    if (!entry.date) return false
    const date = new Date(`${entry.date}T00:00:00`)
    return date.getFullYear() === today.getFullYear() && date.getMonth() === today.getMonth()
  })
}

// Estimate a daily amount after current-month spending and planned expenses.
export function calculateSafeToSpend({ expenses = [], income = [], upcomingExpenses = [], today = new Date() }) {
  const monthlyIncome = getIncomeTotalForMonth(income, today)
  const monthlyExpenses = getMonthEntries(expenses, today).reduce((sum, entry) => sum + Number(entry.amount), 0)
  const remaining = monthlyIncome - monthlyExpenses
  const upcomingTotal = upcomingExpenses
    .filter(entry => !entry.dueDate || new Date(`${entry.dueDate}T00:00:00`) >= new Date(today.getFullYear(), today.getMonth(), today.getDate()))
    .reduce((sum, entry) => sum + Number(entry.amount), 0)
  const availableAfterPlans = remaining - upcomingTotal
  const daysRemaining = new Date(today.getFullYear(), today.getMonth() + 1, 0).getDate() - today.getDate() + 1
  const safeToSpendToday = Math.max(0, availableAfterPlans / daysRemaining)

  let status = 'healthy'
  if (availableAfterPlans <= 0) status = 'risk'
  else if (safeToSpendToday < 100) status = 'caution'

  return { monthlyIncome, monthlyExpenses, remaining, upcomingTotal, availableAfterPlans, daysRemaining, safeToSpendToday, status }
}

// Return actual spending and the most relevant budget warning, if any.
export function calculateBudgetUsage(budget, expenses) {
  const spent = expenses
    .filter(expense => expense.category === budget.category)
    .reduce((sum, expense) => sum + Number(expense.amount), 0)
  const limit = Number(budget.limit)
  const percentage = limit > 0 ? spent / limit * 100 : 0

  let alert = null
  if (percentage >= 100) {
    alert = { level: 'exceeded', message: `Budget exceeded by ${formatCurrency(spent - limit)}.` }
  } else if (percentage >= 90) {
    alert = { level: 'high', message: `You've used ${Math.round(percentage)}% of this budget. It's nearly reached.` }
  } else if (percentage >= 75) {
    alert = { level: 'caution', message: `You've used ${Math.round(percentage)}% of this budget.` }
  } else if (percentage >= 50) {
    alert = { level: 'watch', message: `More than half of this budget is used (${Math.round(percentage)}%).` }
  }

  return { spent, limit, percentage, alert }
}

function formatCurrency(amount) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount)
}

