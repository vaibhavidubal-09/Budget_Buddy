import { useRef, useState } from 'react'
import { expenseCategories, paymentMethods } from '../data/expenseData.js'
import { suggestExpenseCategoryFromApi } from '../services/categoryApiService.js'
import { calculateSplitShares, sharesMatchTotal } from '../services/expenseSplit.js'

function getToday() {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export default function ExpenseForm({ expense, onSave, onCancel }) {
  const initialSplit = (() => { try { return expense?.splitDetails ? JSON.parse(expense.splitDetails) : null } catch { return null } })()
  const [splitEnabled, setSplitEnabled] = useState(Boolean(initialSplit))
  const [splitMode, setSplitMode] = useState(initialSplit?.mode || 'equal')
  const [payerIndex, setPayerIndex] = useState(initialSplit?.payerIndex ?? 0)
  const [participants, setParticipants] = useState(initialSplit?.participants || [
    { name: 'Me', upiId: '', share: '' }, { name: '', upiId: '', share: '' },
  ])
  const [formData, setFormData] = useState(() => expense || {
    description: '', amount: '', category: '', paymentMethod: paymentMethods[0], date: getToday(),
  })
  const [categoryStatus, setCategoryStatus] = useState(expense ? 'ready' : 'waiting')
  const [categoryError, setCategoryError] = useState('')
  const [aiCategorized, setAiCategorized] = useState(false)
  const [formMessage, setFormMessage] = useState('')
  const requestId = useRef(0)
  const categorizedDescription = useRef(expense?.description || '')
  const idempotencyKey = useRef(expense?.idempotencyKey || globalThis.crypto?.randomUUID?.()
    || `expense-${Date.now()}-${Math.random().toString(36).slice(2)}`)

  async function requestCategory(descriptionValue) {
    const description = descriptionValue.trim()
    if (!description) return null
    if (description === categorizedDescription.current && formData.category) return formData.category

    const currentRequest = ++requestId.current
    setCategoryStatus('loading')
    setCategoryError('')
    setFormMessage('')
    try {
      const result = await suggestExpenseCategoryFromApi(description)
      if (currentRequest !== requestId.current) return null
      categorizedDescription.current = description
      setFormData(current => ({ ...current, category: result.category }))
      setAiCategorized(true)
      setCategoryStatus('ready')
      return result.category
    } catch {
      if (currentRequest === requestId.current) {
        setCategoryStatus('error')
        setCategoryError('AI could not choose a category. Try again or choose one manually.')
      }
      return null
    }
  }

  function handleChange(event) {
    const { name, value } = event.target
    setFormData(current => name === 'description'
      ? { ...current, description: value, category: current.description === value ? current.category : '' }
      : { ...current, [name]: value })
    if (name === 'description') {
      requestId.current += 1
      categorizedDescription.current = ''
      setAiCategorized(false)
      setCategoryStatus('waiting')
      setCategoryError('')
      setFormMessage('')
    } else if (name === 'category') {
      setAiCategorized(false)
      setCategoryStatus('ready')
    }
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (categoryStatus === 'loading') {
      setFormMessage('Please wait for AI to finish categorizing this expense.')
      return
    }
    if (!formData.category) {
      const category = await requestCategory(formData.description)
      if (!category) return
      onSave({ ...formData, category, amount: Number(formData.amount), splitDetails: getSplitDetails(),
        ...(!expense?.id ? { idempotencyKey: idempotencyKey.current } : {}) })
      return
    }
    onSave({ ...formData, amount: Number(formData.amount), splitDetails: getSplitDetails(),
      ...(!expense?.id ? { idempotencyKey: idempotencyKey.current } : {}) })
  }

  function getShares() { return calculateSplitShares(Number(formData.amount || 0), participants, splitMode) }

  function getSplitDetails() {
    if (!splitEnabled) return null
    return JSON.stringify({ mode: splitMode, payerIndex, participants: participants.map((person, index) => ({ ...person, share: (getShares()[index] / 100).toFixed(2) })) })
  }

  const shares = splitEnabled ? getShares() : []
  const validCustomTotal = sharesMatchTotal(Number(formData.amount || 0), shares)

  return (
    <div className="expense-modal-backdrop">
      <section className="expense-modal" role="dialog" aria-modal="true" aria-labelledby="expense-form-title">
        <div className="expense-modal-heading"><div><p className="dashboard-eyebrow">A LITTLE MORE CLARITY</p><h2 id="expense-form-title">{expense ? 'Edit expense' : 'Add an expense'}</h2></div><button className="expense-close" type="button" aria-label="Close form" onClick={onCancel}>&#10005;</button></div>
        <form className="expense-form" onSubmit={handleSubmit}>
          <label className="expense-field expense-field-wide"><span>Description</span><input name="description" value={formData.description} onChange={handleChange} onBlur={event => { if (event.target.value.trim() && event.target.value.trim() !== categorizedDescription.current) requestCategory(event.target.value) }} placeholder="e.g. Lunch with friends" maxLength={80} spellCheck="true" lang="en-IN" autoCorrect="on" required autoFocus /></label>
          <div className="expense-form-row"><label className="expense-field"><span>Amount (&#8377;)</span><input name="amount" type="number" value={formData.amount} onChange={handleChange} placeholder="0.00" min="0.01" step="0.01" required /></label><label className="expense-field"><span>Date</span><input name="date" type="date" value={formData.date} onChange={handleChange} required /></label></div>
          <div className="expense-form-row">
            <div className="expense-field"><div className="expense-category-label-row"><label htmlFor="expense-category">Category</label><span className="expense-ai-button">{categoryStatus === 'loading' ? 'AI categorizing…' : aiCategorized ? 'AI categorized' : 'Choose category'}</span></div><select id="expense-category" name="category" value={formData.category} onChange={handleChange} disabled={categoryStatus === 'waiting' || categoryStatus === 'loading'} required><option value="">AI will choose a category</option>{expenseCategories.map(category => <option key={category}>{category}</option>)}</select>
              {categoryError && <div className="expense-ai-error" role="status"><span>{categoryError}</span><button type="button" onClick={() => requestCategory(formData.description)}>Try again</button></div>}
            </div>
            <label className="expense-field"><span>Payment method</span><select name="paymentMethod" value={formData.paymentMethod} onChange={handleChange} required>{paymentMethods.map(method => <option key={method}>{method}</option>)}</select></label>
          </div>
          <section className="expense-split-box">
            <label className="expense-split-toggle"><input type="checkbox" checked={splitEnabled} onChange={event => setSplitEnabled(event.target.checked)} /> Split this bill with others</label>
            {splitEnabled && <>
              <label className="expense-field expense-field-wide"><span>How to split?</span><select value={splitMode} onChange={event => setSplitMode(event.target.value)}><option value="equal">Split equally</option><option value="custom">Set each person's share</option></select></label>
              {participants.map((person, index) => <div className="expense-split-person" key={index}>
                <label className="expense-field"><span>Person {index + 1}</span><input value={person.name} required onChange={event => setParticipants(current => current.map((item, i) => i === index ? { ...item, name: event.target.value } : item))} placeholder={index === 0 ? 'Me' : 'Friend name'} /></label>
                <label className="expense-field"><span>UPI ID (optional)</span><input value={person.upiId} onChange={event => setParticipants(current => current.map((item, i) => i === index ? { ...item, upiId: event.target.value } : item))} placeholder="name@bank" /></label>
                {splitMode === 'custom' && <label className="expense-field"><span>Share (₹)</span><input type="number" min="0" step="0.01" required value={person.share} onChange={event => setParticipants(current => current.map((item, i) => i === index ? { ...item, share: event.target.value } : item))} /></label>}
                {participants.length > 2 && <button className="expense-cancel" type="button" onClick={() => { setParticipants(current => current.filter((_, i) => i !== index)); if (payerIndex >= index && payerIndex > 0) setPayerIndex(payerIndex - 1) }}>Remove</button>}
              </div>)}
              <button className="expense-cancel" type="button" onClick={() => setParticipants(current => [...current, { name: '', upiId: '', share: '' }])}>Add person</button>
              <label className="expense-field expense-field-wide"><span>Who paid the whole bill?</span><select value={payerIndex} onChange={event => setPayerIndex(Number(event.target.value))}>{participants.map((person, index) => <option key={index} value={index}>{person.name || `Person ${index + 1}`}</option>)}</select></label>
              <p className="expense-form-hint">Shares: {participants.map((person, index) => `${person.name || `Person ${index + 1}`} ₹${(shares[index] / 100).toFixed(2)}`).join(' · ')}</p>
              {splitMode === 'custom' && !validCustomTotal && <p className="expense-ai-error" role="alert">Shares must add up to the full bill amount (₹{Number(formData.amount || 0).toFixed(2)}).</p>}
            </>}
          </section>
          {formMessage && <p className="expense-ai-error" role="status">{formMessage}</p>}
          <div className="expense-form-actions"><button className="expense-cancel" type="button" onClick={onCancel}>Cancel</button><button className="button expense-save" type="submit" disabled={splitEnabled && (participants.length < 2 || (splitMode === 'custom' && !validCustomTotal))}>{expense ? 'Save changes' : 'Add expense'}</button></div>
        </form>
      </section>
    </div>
  )
}
