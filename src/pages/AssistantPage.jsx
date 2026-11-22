import { useEffect, useRef, useState } from 'react'
import ChatComposer from '../components/ChatComposer.jsx'
import ChatMessage from '../components/ChatMessage.jsx'
import DashboardLayout from '../layouts/DashboardLayout.jsx'
import { getAssistantReply } from '../services/chatService.js'

const suggestedQuestions = [
  'Where did I spend the most this month?',
  'How much did I spend on Food & Dining this week?',
  'Summarize my income and spending this year.',
  'How can I save more next month?',
]

function getTime() {
  return new Intl.DateTimeFormat('en-IN', { hour: 'numeric', minute: '2-digit' }).format(new Date())
}

export default function AssistantPage({ theme, onThemeChange, onLogout, profile }) {
  const firstName = profile?.fullName?.trim().split(/\s+/)[0] || 'there'
  const [messages, setMessages] = useState([
    { id: 'welcome', role: 'assistant', text: `Hi ${firstName}! I'm your BudgetBuddy AI assistant. Ask about your weekly, monthly, or yearly income, spending, and budgets.`, time: getTime() },
  ])
  const [input, setInput] = useState('')
  const [isReplying, setIsReplying] = useState(false)
  const messagesEndRef = useRef(null)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [messages, isReplying])

  async function sendMessage(messageText = input) {
    const cleanText = messageText.trim()
    if (!cleanText || isReplying) return
    setInput('')
    setMessages(current => [...current, { id: Date.now(), role: 'user', text: cleanText, time: getTime() }])
    setIsReplying(true)
    try {
      const history = messages.filter(message => message.id !== 'welcome')
      const reply = await getAssistantReply(cleanText, history)
      setMessages(current => [...current, { id: Date.now() + 1, role: 'assistant', text: reply, time: getTime() }])
    } catch {
      setMessages(current => [...current, { id: Date.now() + 1, role: 'assistant', text: 'I could not reach Gemini. Check that the backend is running and the Gemini API key is configured, then try again.', time: getTime() }])
    } finally {
      setIsReplying(false)
    }
  }

  return (
    <DashboardLayout title="AI Assistant" theme={theme} onThemeChange={onThemeChange} onLogout={onLogout} profile={profile}>
      <section className="assistant-page-heading"><div><p className="dashboard-eyebrow">A FRIENDLY WAY TO LEARN YOUR HABITS</p><h1>Your AI money buddy</h1><p>Ask Gemini about your real income, spending, and budgets.</p></div><span className="assistant-demo-pill"><i /> Gemini assistant</span></section>
      <section className="assistant-chat-panel" aria-label="BudgetBuddy chat">
        <header className="assistant-chat-header"><span className="assistant-header-avatar">&#10022;</span><span><strong>BudgetBuddy Assistant</strong><small>Ask about this week, month, or year</small></span><span className="assistant-online"><i /> Ready</span></header>
        <div className="assistant-chat-messages" aria-live="polite">
          {messages.map(message => <ChatMessage key={message.id} message={message} />)}
          {isReplying && <div className="assistant-typing" role="status"><span className="chat-avatar">&#10022;</span><span><i /><i /><i /></span><small>Gemini is thinking...</small></div>}
          <div ref={messagesEndRef} />
        </div>
        {messages.length === 1 && <div className="assistant-suggestions"><span>NOT SURE WHAT TO ASK?</span><div>{suggestedQuestions.map(question => <button type="button" key={question} onClick={() => sendMessage(question)}>{question}<span aria-hidden="true">&#8599;</span></button>)}</div></div>}
        <ChatComposer value={input} onChange={setInput} onSend={() => sendMessage()} isReplying={isReplying} />
      </section>
      <p className="assistant-demo-note">Gemini receives your question, recent chat, and weekly, monthly, and yearly totals by category. Saved transaction descriptions are not sent automatically.</p>
    </DashboardLayout>
  )
}
