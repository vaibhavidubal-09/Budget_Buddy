import { useState } from 'react'
import { suggestBudgetPlanFromApi } from '../services/budgetApiService.js'

function formatAmount(amount) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount)
}

export default function BudgetPlanForm({ period, income, categories, hasExistingBudgets, onApply, onCancel }) {
  const [savingsPurpose, setSavingsPurpose] = useState('')
  const [savingsGoal, setSavingsGoal] = useState('')
  const [allocations, setAllocations] = useState([])
  const [explanation, setExplanation] = useState('')
  const [isSuggesting, setIsSuggesting] = useState(false)
  const [isApplying, setIsApplying] = useState(false)
  const [error, setError] = useState('')
  const allocated = allocations.reduce((total, item) => total + Number(item.limit || 0), 0)
  const spendingLimit = Math.floor(income) - Number(savingsGoal || 0)
  const availableCategories = categories.filter(category => !allocations.some(item => item.category === category))

  async function getSuggestion(event) {
    event.preventDefault()
    if (!savingsPurpose.trim()) {
      setError('Tell us what you are saving for.')
      return
    }
    if (!Number.isInteger(Number(savingsGoal)) || Number(savingsGoal) < 0 || Number(savingsGoal) >= Math.floor(income)) {
      setError('Enter a whole-rupee savings amount that is less than your income for this period.')
      return
    }
    setIsSuggesting(true)
    setError('')
    try {
      const result = await suggestBudgetPlanFromApi({ period, income: Math.round(income * 100) / 100, savingsGoal: Number(savingsGoal), savingsPurpose: savingsPurpose.trim(), categories })
      setAllocations(result.allocations.map(item => ({ ...item, limit: Number(item.limit) })))
      setExplanation(result.explanation)
    } catch (requestError) {
      setError(requestError.message || 'Could not get an AI suggestion. Please try again.')
    } finally {
      setIsSuggesting(false)
    }
  }

  function updateAllocation(index, field, value) {
    setAllocations(current => current.map((item, itemIndex) => itemIndex === index ? { ...item, [field]: field === 'limit' ? value : value } : item))
  }

  function addCategory() {
    if (availableCategories.length) setAllocations(current => [...current, { category: availableCategories[0], limit: 0 }])
  }

  async function applyPlan() {
    setIsApplying(true)
    setError('')
    try {
      const result = await onApply(allocations)
      if (result === false) setError('The plan was not applied. The latest saved budgets have been reloaded.')
    } catch (requestError) {
      setError(requestError.message || 'Could not apply the plan. Please try again.')
    } finally {
      setIsApplying(false)
    }
  }

  const matchesGoal = allocations.length > 0 && allocations.every(item => Number.isInteger(Number(item.limit)) && Number(item.limit) > 0) && allocated === spendingLimit

  return (
    <div className="expense-modal-backdrop" onMouseDown={event => { if (event.target === event.currentTarget) onCancel() }}>
      <section className="expense-modal budget-plan-modal" role="dialog" aria-modal="true" aria-labelledby="budget-plan-title">
        <div className="expense-modal-heading"><div><p className="dashboard-eyebrow">SAVE FIRST, THEN PLAN</p><h2 id="budget-plan-title">Plan your {period} money</h2></div><button className="expense-close" type="button" aria-label="Close planner" onClick={onCancel}>&#10005;</button></div>
        <p className="budget-plan-intro">Tell BudgetBuddy what you are saving for and how much. AI will suggest category budgets from the money left.</p>
        <p className="budget-plan-privacy">Applying replaces budgets for this period only. Budgets in other periods are kept.</p>
        <form className="expense-form" onSubmit={getSuggestion}>
          <div className="budget-plan-context"><span>Income recorded for this {period}<strong>{formatAmount(income)}</strong><small>Calculated from your Income page</small></span></div>
          <label className="expense-field expense-field-wide"><span>What are you saving for?</span><input value={savingsPurpose} onChange={event => { setSavingsPurpose(event.target.value); setAllocations([]); setError('') }} placeholder="e.g. emergency fund, trip, new laptop" maxLength={100} required /></label>
          <label className="expense-field expense-field-wide"><span>How much do you want to save this {period}? (&#8377;)</span><input type="number" inputMode="numeric" value={savingsGoal} onChange={event => { setSavingsGoal(event.target.value); setAllocations([]); setError('') }} min="0" max={Math.max(0, Math.floor(income) - 1)} step="1" placeholder="e.g. 5000" required /><small>After saving this, AI plans the remaining {formatAmount(Math.max(0, spendingLimit))} across categories.</small></label>
          {income <= 0 && <p className="expense-ai-error" role="status">No income is scheduled for this {period}. Add income first, or use Add manually to set category budgets.</p>}
          <div className="budget-plan-actions"><button className="button expense-save" type="submit" disabled={isSuggesting || income <= 0}>{isSuggesting ? 'Making your plan...' : allocations.length ? 'Suggest again' : 'Suggest my savings plan'}</button></div>
        </form>
        {error && <p className="expense-ai-error" role="alert">{error}</p>}
        {allocations.length > 0 && <section className="budget-plan-preview" aria-label="Suggested editable budget amounts">
          <div className="budget-plan-preview-heading"><div><h3>Your suggested category budgets</h3><p>Saving for: {savingsPurpose}. {explanation}</p></div><strong>{formatAmount(allocated)} / {formatAmount(spendingLimit)} to spend</strong></div>
          {allocations.map((item, index) => <div className="budget-plan-row" key={`${item.category}-${index}`}>
            <select aria-label={`Category for budget ${index + 1}`} value={item.category} onChange={event => updateAllocation(index, 'category', event.target.value)}>{categories.map(category => <option key={category} value={category} disabled={allocations.some((other, otherIndex) => otherIndex !== index && other.category === category)}>{category}</option>)}</select>
            <label><span className="sr-only">Amount for {item.category}</span><input aria-label={`Amount for ${item.category}`} type="number" min="1" step="1" value={item.limit} onChange={event => updateAllocation(index, 'limit', event.target.value)} /></label>
            <button type="button" aria-label={`Remove ${item.category}`} onClick={() => setAllocations(current => current.filter((_, itemIndex) => itemIndex !== index))}>Remove</button>
          </div>)}
          <div className="budget-plan-preview-foot"><span>{matchesGoal ? `Your budgets leave ${formatAmount(Number(savingsGoal) || 0)} for ${savingsPurpose}.` : `Adjust amounts to total ${formatAmount(Math.max(0, spendingLimit))}, after saving ${formatAmount(Number(savingsGoal) || 0)}.`}</span>{availableCategories.length > 0 && <button className="budget-plan-add-category" type="button" onClick={addCategory}>+ Add category</button>}</div>
          <div className="expense-form-actions"><button className="expense-cancel" type="button" onClick={onCancel} disabled={isApplying}>Cancel</button><button className="button expense-save" type="button" disabled={!matchesGoal || isApplying} onClick={applyPlan}>{isApplying ? 'Applying plan...' : 'Apply this plan'}</button></div>
        </section>}
        {hasExistingBudgets && <p className="budget-plan-privacy">Applying this plan replaces the current category budgets for this {period}.</p>}
        <p className="budget-plan-privacy">Only your income total, savings amount and purpose, period, and category names are sent for the suggestion.</p>
      </section>
    </div>
  )
}
