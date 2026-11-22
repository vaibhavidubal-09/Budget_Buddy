import Reveal from './Reveal.jsx'

export default function AIAssistantPreview() {
  return (
    <section className="ai-section section-pad" id="ai-assistant">
      <div className="ai-layout">
        <div className="ai-copy">
          <p className="eyebrow">A BUDDY WHO KNOWS YOUR NUMBERS</p>
          <h2>Your money has a story.<br /><em>Let AI help you read it.</em></h2>
          <p>Ask the questions you might not know how to answer. Get a clear, personal explanation based on your spending.</p>
          <div className="ai-note"><span>✳</span><span><strong>Made to explain, not judge.</strong><small>Helpful insights in plain language.</small></span></div>
        </div>
        <Reveal className="ai-chat">
          <div className="ai-chat-head"><span className="ai-orb">✳</span><span><strong>Your BudgetBuddy</strong><small><i /> Ready to help</small></span><span className="mock-data-label">SAMPLE CHAT</span></div>
          <div className="ai-thread">
            <div className="ai-user-message">Where did I spend most this month?</div>
            <div className="ai-reply"><div className="ai-reply-mark">✳</div><p>You spent the most on <strong>Food &amp; Dining — ₹1,420.</strong> That's 44% of your spending, and ₹320 more than last month.</p></div>
            <div className="ai-user-message second-question">Can I spend ₹500 this weekend?</div>
            <div className="ai-reply"><div className="ai-reply-mark">✳</div><p>You have about <strong>₹1,760 remaining.</strong> You can spend ₹500 and keep ₹1,260 for the rest of the month.</p></div>
          </div>
          <div className="ai-input-mock"><span>Ask about your spending…</span><b>↑</b></div>
          <p className="ai-disclaimer">Illustrative responses · AI connection comes later</p>
        </Reveal>
      </div>
    </section>
  )
}
