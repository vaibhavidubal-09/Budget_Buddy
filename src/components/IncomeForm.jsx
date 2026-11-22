import { useState } from 'react'

function getToday() {
  const today = new Date()
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
}

export default function IncomeForm({ income, onSave, onCancel }) {
  const [formData, setFormData] = useState(() => income
    ? { ...income, frequency: income.frequency || 'ONCE', intervalDays: income.intervalDays || '' }
    : { source: '', amount: '', date: getToday(), frequency: 'ONCE', intervalDays: '' })

  function handleChange(event) {
    const { name, value } = event.target
    setFormData({ ...formData, [name]: value })
  }

  function handleSubmit(event) {
    event.preventDefault()
    onSave({ ...formData, amount: Number(formData.amount), intervalDays: formData.frequency === 'CUSTOM' ? Number(formData.intervalDays) : null })
  }

  return (
    <div className="expense-modal-backdrop">
      <section className="expense-modal" role="dialog" aria-modal="true" aria-labelledby="income-form-title">
        <div className="expense-modal-heading"><div><p className="dashboard-eyebrow">MONEY COMING IN</p><h2 id="income-form-title">{income ? 'Edit income' : 'Add income'}</h2></div><button className="expense-close" type="button" aria-label="Close form" onClick={onCancel}>&#10005;</button></div>
        <form className="expense-form" onSubmit={handleSubmit}>
          <label className="expense-field expense-field-wide"><span>Source</span><input name="source" value={formData.source} onChange={handleChange} placeholder="e.g. Monthly allowance" maxLength={80} required autoFocus /></label>
          <div className="expense-form-row">
            <label className="expense-field"><span>Amount (&#8377;)</span><input name="amount" type="number" value={formData.amount} onChange={handleChange} placeholder="0.00" min="0.01" step="0.01" required /></label>
            <label className="expense-field"><span>First payment date</span><input name="date" type="date" value={formData.date} onChange={handleChange} required /></label>
          </div>
          <div className="expense-form-row">
            <label className="expense-field"><span>How often?</span><select name="frequency" value={formData.frequency} onChange={handleChange}><option value="ONCE">One time</option><option value="WEEKLY">Weekly (every 7 days)</option><option value="MONTHLY">Monthly (once each month)</option><option value="YEARLY">Yearly (once each year)</option><option value="CUSTOM">Every set number of days</option></select></label>
            {formData.frequency === 'CUSTOM' && <label className="expense-field"><span>Repeat every (days)</span><input name="intervalDays" type="number" value={formData.intervalDays} onChange={handleChange} min="1" max="3650" step="1" required placeholder="e.g. 14" /></label>}
          </div>
          <p className="expense-form-hint">The date above is the first payment. Repeating income is counted each time it is due; yearly income appears in the total only in its scheduled month.</p>
          <div className="expense-form-actions"><button className="expense-cancel" type="button" onClick={onCancel}>Cancel</button><button className="button expense-save" type="submit">{income ? 'Save changes' : 'Add income'}</button></div>
        </form>
      </section>
    </div>
  )
}
