import Reveal from './Reveal.jsx'

const categories = [
  { name: 'Food', amount: '₹1,420', width: '88%', color: 'food-color', icon: '◉' },
  { name: 'Travel', amount: '₹620', width: '42%', color: 'travel-color', icon: '↗' },
  { name: 'Shopping', amount: '₹800', width: '56%', color: 'shop-color', icon: '◇' },
  { name: 'Education', amount: '₹400', width: '30%', color: 'edu-color', icon: '▤' },
  { name: 'Other', amount: '₹0', width: '0%', color: 'other-color', icon: '·' },
]

export default function DashboardPreview() {
  return (
    <section className="product-section section-pad" id="product">
      <div className="section-heading centered product-heading">
        <p className="eyebrow">NO MORE GUESSING</p>
        <h2>See your money.<br /><em>Clearly.</em></h2>
        <p>Your whole month, in one calm place.</p>
      </div>
      <Reveal className="dashboard-window">
        <aside className="dash-sidebar">
          <div className="dash-brand"><span>₹</span> BudgetBuddy</div>
          <small className="dash-nav-label">YOUR MONEY</small>
          <div className="dash-nav-item active"><b>◫</b> Overview</div>
          <div className="dash-nav-item"><b>↗</b> Expenses</div>
          <div className="dash-nav-item"><b>▤</b> Budgets</div>
          <div className="dash-nav-item"><b>◷</b> Insights</div>
          <div className="dash-profile"><span>AS</span><div><strong>Arjun Shah</strong><small>Student account</small></div></div>
        </aside>
        <div className="dash-main">
          <div className="dash-topbar"><div><small>MONDAY, SEPTEMBER 28</small><h3>Hey Arjun, here's your month.</h3></div><span className="dash-month">September 2026⌄</span></div>
          <div className="dash-summary">
            <article className="dash-total"><span>Monthly money</span><strong>₹5,000</strong><small><i /> Your allowance this month</small></article>
            <article><span>Spent</span><strong>₹3,240</strong><div className="dash-mini-bar"><i /></div><small>64.8% of your allowance</small></article>
            <article><span>Remaining</span><strong>₹1,760</strong><small>₹620 available this week</small></article>
          </div>
          <div className="dash-content-grid">
            <article className="dash-panel categories-panel"><div className="dash-panel-title"><span><strong>Where it went</strong><small>Your spending by category</small></span><button type="button" aria-label="More category options">···</button></div><div className="category-list">{categories.map(category => <div className="category-row" key={category.name}><span className={`category-icon ${category.color}`}>{category.icon}</span><span className="category-name">{category.name}<i><b style={{ width: category.width }} /></i></span><strong>{category.amount}</strong></div>)}</div></article>
            <article className="dash-panel week-panel"><div className="dash-panel-title"><span><strong>This week</strong><small>Daily spending</small></span><span className="week-total">₹680</span></div><div className="week-chart"><div className="week-guides"><i /><i /><i /></div><div className="week-bars">{[30, 58, 42, 76, 48, 88, 24].map((height, index) => <span key={index} className={index === 5 ? 'today' : ''} style={{ height: `${height}%` }} />)}</div></div><div className="week-days"><span>M</span><span>T</span><span>W</span><span>T</span><span>F</span><span>S</span><span>S</span></div><div className="dash-insight"><span>✳</span><p><strong>A small win</strong><br />You spent ₹180 less on food this week.</p></div></article>
          </div>
          <div className="dash-bottom-note"><span>◷</span> Your numbers update as you track your spending. <span className="mock-data-label">SAMPLE DATA</span></div>
        </div>
      </Reveal>
    </section>
  )
}
