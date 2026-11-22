import { Link } from 'react-router-dom'

const rupee = String.fromCodePoint(8377)

const activity = [
  { title: 'Campus cafe', detail: 'Food & Dining · Today', amount: '- ₹120', icon: '☕', color: 'coffee' },
  { title: 'Auto ride', detail: 'Travel · Yesterday', amount: '- ₹60', icon: '↗', color: 'ride' },
  { title: 'Music plan', detail: 'Subscriptions · Sep 24', amount: '- ₹119', icon: '♫', color: 'music' },
]

export default function HeroStory() {
  return (
    <section className="hero-story" aria-labelledby="hero-title">
      <div className="hero-story-copy">
        <p className="eyebrow"><span className="eyebrow-dot" /> YOUR MONEY, A BRIGHTER TOMORROW</p>
        <h1 id="hero-title"><span>Budget</span><em>Buddy</em><i className="wordmark-spark" aria-hidden="true">✳</i></h1>
        <h2>Spend smarter.<br /><span>Live brighter.</span></h2>
        <p className="hero-lead">Track your expenses, build better habits, and reach your goals—one small step at a time.</p>
        <p className="hero-context">Because a balanced today builds a brighter tomorrow.</p>
        <div className="hero-principles">
          <div><span className="principle-icon icon-track">{rupee}</span><span><strong>Track</strong><small>Every rupee</small></span></div>
          <div><span className="principle-icon icon-plan">◎</span><span><strong>Plan</strong><small>What matters</small></span></div>
          <div><span className="principle-icon icon-grow">↗</span><span><strong>Grow</strong><small>A happier you</small></span></div>
        </div>
        <div className="hero-actions">
          <Link className="button" to="/register">Get started free <span aria-hidden="true">&#8594;</span></Link>
          <a className="text-link" href="#problem"><span className="play-small" aria-hidden="true">▶</span><span>See the story<small>In 60 seconds</small></span></a>
        </div>
        <p className="hero-note"><span className="avatar-stack"><i>A</i><i>M</i><i>R</i></span><span>Made for student life<br /><strong>and real-life money.</strong></span></p>
      </div>

      <div className="hero-scene" aria-label="Student finance app preview with allowance, spending and savings insights">
        <div className="scene-shape shape-lime" /><div className="scene-shape shape-sky" /><div className="scene-scribble scribble-one">little steps, big goals ↗</div>
        <article className="hero-phone">
          <div className="phone-status"><span>9:41</span><span>●●● ▰</span></div>
          <div className="phone-welcome"><span className="phone-avatar">A</span><span><strong>Hi, Aarav!</strong><small>Small steps. Big goals.</small></span><b>♧</b></div>
          <div className="phone-money-card"><div className="phone-money-heading"><span>This month <b>⌄</b></span><span>•••</span></div><strong>{rupee}1,760</strong><small>remaining from {rupee}5,000</small><div className="phone-progress"><i /></div><div className="phone-progress-foot"><span>Spent {rupee}3,240</span><b>65%</b></div></div>
          <div className="phone-categories"><div><i className="cat-food">☕</i><span>Food</span></div><div><i className="cat-travel">✈</i><span>Travel</span></div><div><i className="cat-shop">▣</i><span>Shopping</span></div><div><i className="cat-more">···</i><span>More</span></div></div>
          <div className="phone-recent-head"><strong>Recent transactions</strong><span>See all</span></div>
          <div className="phone-activity">{activity.map(item => <div className="phone-activity-row" key={item.title}><i className={`activity-icon ${item.color}`}>{item.icon}</i><span><strong>{item.title}</strong><small>{item.detail}</small></span><b>{item.amount}</b></div>)}</div>
          <div className="phone-tabs"><span className="tab-active">⌂<small>Home</small></span><span>▥<small>Insights</small></span><span>◎<small>Goals</small></span><span>☰<small>More</small></span></div>
        </article>
        <article className="floating-goal"><span className="goal-icon">✦</span><span><small>SAVING FOR</small><strong>Trip to Manali</strong><i><b /></i><small>{rupee}18,000 of {rupee}40,000 <em>45%</em></small></span></article>
        <article className="floating-chart"><div><span className="chart-icon">↘</span><span><strong>This month, in real time</strong><small>You're doing great</small></span></div><svg viewBox="0 0 190 66" role="img" aria-label="Spending trend moving lower"><path d="M3 12 C24 18, 26 31, 48 27 S75 42, 93 37 S122 48, 140 43 S166 59, 188 51" /><circle cx="188" cy="51" r="4" /></svg><div className="chart-labels"><span>DAY 1</span><span>DAY 15</span><span>DAY 30</span></div></article>
        <article className="floating-ai"><span>✳</span><div><small>AI INSIGHT</small><strong>You're spending 12% less on food this month.</strong></div><b>›</b></article>
        <span className="scene-doodle doodle-arrow" aria-hidden="true">↗</span><span className="scene-doodle doodle-leaf" aria-hidden="true">✳</span>
      </div>
    </section>
  )
}
