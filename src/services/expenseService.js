import { initialExpenses } from '../data/expenseData.js'
import { readStoredValue, saveStoredValue } from './browserStorage.js'

// This temporary data function is the future place to load expenses from an API.
export function getExpenses() {
  return readStoredValue('budgetbuddy-expenses', initialExpenses)
}

export function saveExpenses(expenses) {
  saveStoredValue('budgetbuddy-expenses', expenses)
}

// Pure helper: return a new list with an id, without changing the old list.
export function addExpense(expenses, newExpense) {
  const newExpenses = Array.isArray(newExpense) ? newExpense : [newExpense]
  const savedExpenses = newExpenses.map((expense, index) => ({ ...expense, id: Date.now() + index }))
  return [...savedExpenses, ...expenses]
}

export function calculateDashboardSummary(expenses, monthlyIncome = 5000, monthlyBudget = 4000) {
  const totalExpenses = expenses.reduce((sum, expense) => sum + Number(expense.amount), 0)
  const categoryTotals = expenses.reduce((totals, expense) => {
    totals[expense.category] = (totals[expense.category] || 0) + Number(expense.amount)
    return totals
  }, {})

  return {
    monthlyIncome,
    totalExpenses,
    currentBalance: monthlyIncome - totalExpenses,
    monthlyBudget,
    budgetRemaining: Math.max(0, monthlyBudget - totalExpenses),
    budgetProgress: monthlyBudget ? Math.min(100, (totalExpenses / monthlyBudget) * 100) : 0,
    categoryTotals: Object.entries(categoryTotals).map(([category, amount]) => ({ category, amount })).sort((a, b) => b.amount - a.amount),
    recentExpenses: [...expenses].sort((a, b) => b.date.localeCompare(a.date)).slice(0, 5),
  }
}
