import { useState } from 'react'
import ExpenseForm from './ExpenseForm.jsx'
import AIExpenseParser from './AIExpenseParser.jsx'
import TransactionImporter from './TransactionImporter.jsx'

export default function SmartExpenseEntry({ initialMode, onSave, onClose }) {
  const [mode, setMode] = useState(initialMode || 'choices')
  const [manualExpense, setManualExpense] = useState(null)

  function editParsedExpense(expense) {
    setManualExpense(expense)
    setMode('manual')
  }

  if (mode === 'manual') {
    return <ExpenseForm key={manualExpense?.description || 'new-expense'} expense={manualExpense} onSave={onSave} onCancel={() => manualExpense ? setMode('ai') : setMode('choices')} />
  }

  if (mode === 'ai') {
    return <div className="expense-modal-backdrop"><section className="smart-expense-modal" role="dialog" aria-modal="true" aria-labelledby="smart-expense-title"><div className="smart-modal-heading"><div><p className="dashboard-eyebrow">TRACKING SHOULD FEEL EASY</p><h2 id="smart-expense-title">Just tell BudgetBuddy what you spent.</h2></div><button className="expense-close" type="button" aria-label="Close expense options" onClick={onClose}>×</button></div><AIExpenseParser onConfirm={onSave} onEdit={editParsedExpense} onBack={() => setMode('choices')} /></section></div>
  }

  if (mode === 'import') {
    return <div className="expense-modal-backdrop"><section className="smart-expense-modal smart-expense-modal-wide" role="dialog" aria-modal="true" aria-labelledby="smart-expense-title"><div className="smart-modal-heading"><div><p className="dashboard-eyebrow">MAKE ROOM FOR THE BIG PICTURE</p><h2 id="smart-expense-title">Add several transactions at once.</h2></div><button className="expense-close" type="button" aria-label="Close expense options" onClick={onClose}>×</button></div><TransactionImporter onImport={onSave} onBack={() => setMode('choices')} /></section></div>
  }

  return <div className="expense-modal-backdrop"><section className="smart-expense-modal" role="dialog" aria-modal="true" aria-labelledby="smart-expense-title"><div className="smart-modal-heading"><div><p className="dashboard-eyebrow">A LITTLE LESS TYPING</p><h2 id="smart-expense-title">How do you want to add your spending?</h2><p>Tracking your spending shouldn’t feel like work.</p></div><button className="expense-close" type="button" aria-label="Close expense options" onClick={onClose}>×</button></div><div className="smart-entry-options">
    <button className="smart-entry-option smart-entry-ai" type="button" onClick={() => setMode('ai')}><span className="smart-option-icon">✦</span><span><strong>AI entry</strong><small>Tell BudgetBuddy naturally</small></span><b>→</b></button>
    <button className="smart-entry-option" type="button" onClick={() => setMode('manual')}><span className="smart-option-icon">＋</span><span><strong>Manual entry</strong><small>Enter the details yourself</small></span><b>→</b></button>
    <button className="smart-entry-option" type="button" onClick={() => setMode('import')}><span className="smart-option-icon">↑</span><span><strong>Import statement</strong><small>Import multiple transactions</small></span><b>→</b></button>
  </div></section></div>
}
