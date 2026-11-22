import { allInitialBudgets } from '../data/budgetData.js'
import { readStoredValue, saveStoredValue } from './browserStorage.js'

export function getBudgets() {
  return readStoredValue('budgetbuddy-budgets', allInitialBudgets)
}

export function saveBudgets(budgets) {
  saveStoredValue('budgetbuddy-budgets', budgets)
}
