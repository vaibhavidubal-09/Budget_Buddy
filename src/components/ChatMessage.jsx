export default function ChatMessage({ message }) {
  const isAssistant = message.role === 'assistant'
  return (
    <article className={`chat-message ${isAssistant ? 'chat-message-assistant' : 'chat-message-user'}`}>
      {isAssistant && <span className="chat-avatar" aria-hidden="true">&#10022;</span>}
      <div className="chat-message-body"><span className="chat-message-author">{isAssistant ? 'BudgetBuddy' : 'You'}</span><p>{message.text}</p><time>{message.time}</time></div>
      {!isAssistant && <span className="chat-user-avatar" aria-hidden="true">AS</span>}
    </article>
  )
}
