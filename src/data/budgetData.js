// Category spending is sample data for the first frontend version.
export const initialBudgets = [
  { id: 1, category: 'Food & Dining', limit: 5000, spent: 3240, period: 'monthly' },
  { id: 2, category: 'Transportation', limit: 3500, spent: 1860, period: 'monthly' },
  { id: 3, category: 'Shopping', limit: 4000, spent: 2702, period: 'monthly' },
  { id: 4, category: 'Education', limit: 3000, spent: 1200, period: 'monthly' },
  { id: 5, category: 'Entertainment', limit: 2500, spent: 980, period: 'monthly' },
  { id: 6, category: 'Bills & Subscriptions', limit: 2000, spent: 640, period: 'monthly' },
]

// Weekly and yearly amounts are scaled examples, so all periods have a starting view.
const monthlyBudgets = initialBudgets
const weeklyBudgets = monthlyBudgets.map(budget => ({ ...budget, id: budget.id + 100, limit: Math.round(budget.limit / 4), spent: Math.round(budget.spent / 4), period: 'weekly' }))
const yearlyBudgets = monthlyBudgets.map(budget => ({ ...budget, id: budget.id + 200, limit: budget.limit * 12, spent: budget.spent * 12, period: 'yearly' }))

export const allInitialBudgets = [...weeklyBudgets, ...monthlyBudgets, ...yearlyBudgets]
