import { useState } from 'react'
import { suggestExpenseCategoryFromApi } from '../services/categoryApiService.js'

export default function BudgetForm({ budget, categories, period = 'monthly', onSave, onCancel }) {
  const [category, setCategory] = useState(budget?.category || categories[0] || '')
  const [limit, setLimit] = useState(budget?.limit ?? '')
  const [purpose, setPurpose] = useState('')
  const [isSuggesting, setIsSuggesting] = useState(false)
  const [suggestion, setSuggestion] = useState('')
  const [suggestionError, setSuggestionError] = useState('')

  async function suggestCategory() {
    if (!purpose.trim()) {
      setSuggestionError('Describe what you want this budget for first.')
      return
    }
    setIsSuggesting(true)
    setSuggestionError('')
    setSuggestion('')
    try {
      const result = await suggestExpenseCategoryFromApi(purpose.trim())
      if (!categories.includes(result.category)) {
        setSuggestionError('That category already has a budget for this period. Please choose another.')
        return
      }
      setCategory(result.category)
      setSuggestion(`${result.category} suggested. You can change it.`)
    } catch {
      setSuggestionError('Could not get a suggestion. Choose a category manually or try again.')
    } finally {
      setIsSuggesting(false)
    }
  }

  function handleSubmit(event) {
    event.preventDefault()
    onSave({ category, limit: Number(limit) })
  }

  return (
    <div className="expense-modal-backdrop">
      <section className="expense-modal" role="dialog" aria-modal="true" aria-labelledby="budget-form-title">
        <div className="expense-modal-heading"><div><p className="dashboard-eyebrow">MAKE A PLAN THAT FITS</p><h2 id="budget-form-title">{budget ? 'Edit category budget' : 'Add a category budget'}</h2></div><button className="expense-close" type="button" aria-label="Close form" onClick={onCancel}>&#10005;</button></div>
        <form className="expense-form" onSubmit={handleSubmit}>
          <label className="expense-field expense-field-wide"><span>What is this budget for?</span><input value={purpose} onChange={event => { setPurpose(event.target.value); setSuggestion(''); setSuggestionError('') }} placeholder="e.g. groceries or bus travel" maxLength={120} /></label>
          <div className="expense-field expense-field-wide"><div className="expense-category-label-row"><label htmlFor="budget-category">Category</label><button className="expense-ai-button" type="button" onClick={suggestCategory} disabled={isSuggesting || categories.length === 0}>{isSuggesting ? 'Gemini is suggesting…' : 'Suggest with AI'}</button></div><select id="budget-category" value={category} onChange={event => { setCategory(event.target.value); setSuggestion('') }} required>{categories.map(item => <option key={item}>{item}</option>)}</select>{suggestion && <small role="status">{suggestion}</small>}{suggestionError && <p className="expense-ai-error" role="alert">{suggestionError}</p>}</div>
          <label className="expense-field expense-field-wide"><span>{period[0].toUpperCase() + period.slice(1)} limit (&#8377;)</span><input type="number" value={limit} onChange={event => setLimit(event.target.value)} placeholder="e.g. 3000" min="1" step="1" required /></label>
          <div className="expense-form-actions"><button className="expense-cancel" type="button" onClick={onCancel}>Cancel</button><button className="button expense-save" type="submit">{budget ? 'Save budget' : 'Add budget'}</button></div>
        </form>
      </section>
    </div>
  )
}
