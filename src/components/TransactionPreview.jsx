import { useState } from 'react'
import { expenseCategories } from '../data/expenseData.js'

function formatAmount(amount) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(Number(amount) || 0)
}

export default function TransactionPreview({ transactions, onChange, onRemove }) {
  const [editingId, setEditingId] = useState(null)

  function update(transaction, field, value) {
    onChange(transaction.id, { [field]: field === 'amount' ? Number(value) : value })
  }

  return (
    <div className="transaction-preview">
      <div className="transaction-preview-heading"><div><strong>Review imported transactions</strong><small>Check the extracted rows before adding them.</small></div><span>{transactions.length} found</span></div>
      <div className="transaction-preview-table-wrap"><table className="transaction-preview-table"><thead><tr><th>Date</th><th>Description</th><th>Amount</th><th>Category</th><th>Actions</th></tr></thead>
        <tbody>{transactions.map(transaction => <tr key={transaction.id} className={transaction.duplicate ? 'transaction-is-duplicate' : ''}>
          <td>{editingId === transaction.id ? <input aria-label="Transaction date" type="date" value={transaction.date} onChange={event => update(transaction, 'date', event.target.value)} /> : new Date(`${transaction.date}T00:00:00`).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}</td>
          <td>{editingId === transaction.id ? <input aria-label="Transaction description" value={transaction.description} onChange={event => update(transaction, 'description', event.target.value)} /> : <>{transaction.description}{transaction.duplicate && <small className="transaction-duplicate-note">Duplicate: {transaction.reason}</small>}</>}</td>
          <td>{editingId === transaction.id ? <input aria-label="Transaction amount" type="number" min="0.01" step="0.01" value={transaction.amount} onChange={event => update(transaction, 'amount', event.target.value)} /> : formatAmount(transaction.amount)}</td>
          <td>{editingId === transaction.id ? <select aria-label="Transaction category" value={transaction.category} onChange={event => update(transaction, 'category', event.target.value)}>{expenseCategories.map(category => <option key={category}>{category}</option>)}</select> : transaction.category}</td>
          <td><div className="expense-row-actions"><button type="button" aria-label={`${editingId === transaction.id ? 'Done editing' : 'Edit'} ${transaction.description}`} onClick={() => setEditingId(editingId === transaction.id ? null : transaction.id)}>{editingId === transaction.id ? '✓' : '✎'}</button><button type="button" aria-label={`Remove ${transaction.description}`} onClick={() => onRemove(transaction.id)}>×</button></div></td>
        </tr>)}</tbody>
      </table></div>
    </div>
  )
}
