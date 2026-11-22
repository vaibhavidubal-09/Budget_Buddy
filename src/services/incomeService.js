import { initialIncome } from '../data/incomeData.js'
import { readStoredValue, saveStoredValue } from './browserStorage.js'

export function getIncome() {
  return readStoredValue('budgetbuddy-income', initialIncome)
}

export function saveIncome(income) {
  saveStoredValue('budgetbuddy-income', income)
}
