import { useState } from 'react'
import { parseExpenseWithAI } from '../services/expenseParser.js'

const suggestions = [
  'I spent \u20B9180 on Uber to college',
  'I paid \u20B9500 for groceries',
  'I spent \u20B9300 watching a movie',
]

function formatAmount(amount) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(amount)
}

function getToday() {
  const today = new Date()
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
}

export default function AIExpenseParser({ onConfirm, onEdit, onBack }) {
  const [text, setText] = useState('')
  const [isProcessing, setIsProcessing] = useState(false)
  const [parsedExpense, setParsedExpense] = useState(null)
  const [error, setError] = useState('')
  const [idempotencyKey] = useState(() => globalThis.crypto?.randomUUID?.()
    || `expense-${Date.now()}-${Math.random().toString(36).slice(2)}`)

  async function understandExpense(value = text) {
    if (!value.trim()) return
    setError('')
    setParsedExpense(null)
    setIsProcessing(true)
    try {
      const result = await parseExpenseWithAI(value)
      setParsedExpense(result)
    } catch (error) {
      setError(error.message || 'AI could not understand that yet. Try including an amount and what you bought.')
    } finally {
      setIsProcessing(false)
    }
  }

  function chooseSuggestion(example) {
    setText(example)
    understandExpense(example)
  }

  function confirmExpense() {
    const { spellingSuggestion, ...expense } = parsedExpense
    onConfirm({ ...expense, idempotencyKey })
  }

  function editExpense() {
    const { spellingSuggestion, ...expense } = parsedExpense
    onEdit(expense)
  }

  return (
    <div className="smart-ai-content">
      <div className="smart-mode-heading"><button type="button" className="smart-back-button" onClick={onBack}>← All options</button><span className="smart-ai-badge">&#10022; AI ENTRY</span></div>
      <h3>Tell BudgetBuddy what you spent</h3>
      <p className="smart-mode-description">Describe it in your own words. AI will interpret it and suggest a spelling correction when it can.</p>
      <form className="ai-expense-input" onSubmit={event => { event.preventDefault(); understandExpense() }}>
        <textarea value={text} onChange={event => { setText(event.target.value); setParsedExpense(null) }} placeholder="I spent &#8377;250 on coffee at Starbucks" rows="3" maxLength={500} spellCheck="true" lang="en-IN" aria-label="Describe your expense" />
        <button className="button" type="submit" disabled={!text.trim() || isProcessing}>{isProcessing ? 'Understanding...' : 'Understand expense'}</button>
      </form>
      {isProcessing && <p className="smart-processing" role="status"><span className="smart-spinner" /> Understanding your expense...</p>}
      {error && <p className="smart-error" role="alert">{error}</p>}
      {!parsedExpense && !isProcessing && <div className="ai-suggestions"><span>TRY ONE</span>{suggestions.map(example => <button key={example} type="button" onClick={() => chooseSuggestion(example)}>{example}</button>)}</div>}
      {parsedExpense && <section className="ai-confirmation" aria-label="Confirm parsed expense">
        <div className="ai-confirmation-heading"><span>✓</span><div><strong>Here is what I understood</strong><small>Check these details before adding it.</small></div></div>
        {parsedExpense.spellingSuggestion && <p className="ai-spelling-suggestion">Did you mean “{parsedExpense.spellingSuggestion}”?</p>}
        {parsedExpense.amount > 0 ? <div className="ai-parsed-grid"><div><small>Amount</small><strong>{formatAmount(parsedExpense.amount)}</strong></div><div><small>Category</small><strong>{parsedExpense.category}</strong></div><div><small>Description</small><strong>{parsedExpense.description}</strong></div><div><small>Payment method</small><strong>{parsedExpense.paymentMethod}</strong></div><div><small>Date</small><strong>{parsedExpense.date === getToday() ? 'Today' : parsedExpense.date}</strong></div></div> : <p className="smart-error">I could not find an amount. Edit the details manually to continue.</p>}
        <div className="smart-modal-actions"><button className="expense-cancel" type="button" onClick={editExpense}>Edit</button><button className="button" type="button" disabled={parsedExpense.amount <= 0} onClick={confirmExpense}>Confirm expense</button></div>
      </section>}
    </div>
  )
}
