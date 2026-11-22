import { useRef, useState } from 'react'
import TransactionPreview from './TransactionPreview.jsx'
import { importTransactions } from '../services/transactionImportService.js'

function formatFileSize(bytes) {
  return bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export default function TransactionImporter({ onImport, onBack }) {
  const inputRef = useRef(null)
  const [file, setFile] = useState(null)
  const [isProcessing, setIsProcessing] = useState(false)
  const [isDragging, setIsDragging] = useState(false)
  const [transactions, setTransactions] = useState([])
  const [error, setError] = useState('')

  async function selectFile(selectedFile) {
    if (!selectedFile) return
    const extension = selectedFile.name.split('.').pop().toLowerCase()
    if (!['csv', 'pdf', 'png', 'jpg', 'jpeg', 'webp'].includes(extension)) {
      setError('Choose a CSV, PDF, or transaction image (PNG, JPG, or WEBP).')
      return
    }
    if (selectedFile.size > 10 * 1024 * 1024) {
      setError('That file is larger than 10 MB. Choose a smaller statement.')
      return
    }
    setError('')
    setFile(selectedFile)
    setTransactions([])
    setIsProcessing(true)
    try {
      const extractedTransactions = await importTransactions(selectedFile)
      setTransactions(extractedTransactions)
    } catch (exception) {
      setError(exception.message || 'We could not prepare a preview for this file. Try choosing it again.')
    } finally {
      setIsProcessing(false)
    }
  }

  function updateTransaction(id, changes) {
    setTransactions(transactions.map(item => item.id === id ? { ...item, ...changes, duplicate: false, reason: null } : item))
  }

  return (
    <div className="smart-import-content">
      <div className="smart-mode-heading"><button type="button" className="smart-back-button" onClick={onBack}>← All options</button><span className="smart-import-badge">↑ IMPORT</span></div>
      <h3>Import transactions</h3><p className="smart-mode-description">Upload your bank or UPI transaction statement and let BudgetBuddy organize your spending.</p>
      {!file && <div className={`statement-dropzone${isDragging ? ' is-dragging' : ''}`} onDragOver={event => { event.preventDefault(); setIsDragging(true) }} onDragLeave={() => setIsDragging(false)} onDrop={event => { event.preventDefault(); setIsDragging(false); selectFile(event.dataTransfer.files[0]) }}>
        <span className="statement-upload-icon">↑</span><strong>Drop your statement or transaction image here</strong><span>or</span><button type="button" onClick={() => inputRef.current?.click()}>Browse files</button><small>CSV is read in your browser. PDF and images are sent to Gemini for extraction. Up to 10 MB.</small>
        <input ref={inputRef} className="statement-file-input" type="file" accept=".csv,.pdf,.png,.jpg,.jpeg,.webp,text/csv,application/pdf,image/png,image/jpeg,image/webp" onChange={event => { selectFile(event.target.files[0]); event.target.value = '' }} />
      </div>}
      {file && <div className="statement-file-info"><span className="statement-file-icon">{file.type.startsWith('image/') ? 'IMG' : file.name.toLowerCase().endsWith('.pdf') ? 'PDF' : 'CSV'}</span><span><strong>{file.name}</strong><small>{file.name.split('.').pop().toUpperCase()} · {formatFileSize(file.size)}</small></span><button type="button" onClick={() => { setFile(null); setTransactions([]) }}>Change file</button></div>}
      {error && <p className="smart-error" role="alert">{error}</p>}
      {isProcessing && <p className="smart-processing" role="status"><span className="smart-spinner" /> Analyzing your transactions...</p>}
      {!isProcessing && transactions.length > 0 && <><TransactionPreview transactions={transactions} onChange={updateTransaction} onRemove={id => setTransactions(transactions.filter(item => item.id !== id))} />
        <div className="smart-modal-actions"><button className="expense-cancel" type="button" onClick={onBack}>Cancel</button><button className="button" type="button" disabled={!transactions.length} onClick={() => onImport(transactions)}>Add transactions to BudgetBuddy</button></div>
      </>}
    </div>
  )
}
