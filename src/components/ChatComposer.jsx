export default function ChatComposer({ value, onChange, onSend, isReplying }) {
  function handleKeyDown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      onSend()
    }
  }

  return (
    <form className="chat-composer" onSubmit={event => { event.preventDefault(); onSend() }}>
      <textarea value={value} onChange={event => onChange(event.target.value)} onKeyDown={handleKeyDown} rows="1" placeholder="Ask about your spending..." aria-label="Message BudgetBuddy" />
      <button className="button" type="submit" disabled={!value.trim() || isReplying} aria-label="Send message">{isReplying ? '...' : 'Send'} <span aria-hidden="true">&#8593;</span></button>
      <small>Enter to send · Shift + Enter for a new line</small>
    </form>
  )
}
