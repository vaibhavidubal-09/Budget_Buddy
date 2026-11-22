import Reveal from './Reveal.jsx'

const features = [
  { icon: '▣', title: 'Monthly allowance', copy: 'See your allowance and how much of it is left.', tint: 'tint-mint' },
  { icon: '↗', title: 'Daily expenses', copy: 'Keep the small UPI spends from slipping by.', tint: 'tint-peach' },
  { icon: '◎', title: 'Savings goals', copy: 'Put a little aside for what you’re looking forward to.', tint: 'tint-lilac' },
  { icon: '▥', title: 'Spending analytics', copy: 'Spot the categories taking more than you thought.', tint: 'tint-blue' },
  { icon: '♧', title: 'Budget alerts', copy: 'Get a gentle nudge before a category runs low.', tint: 'tint-yellow' },
  { icon: '✳', title: 'AI money buddy', copy: 'Ask questions and understand your habits in plain words.', tint: 'tint-green' },
]

export default function StudentFeatures() {
  return (
    <section className="features-section section-pad" id="features">
      <div className="section-heading centered"><p className="eyebrow">MADE FOR THE WAY STUDENTS LIVE</p><h2>Built around <em>student life.</em></h2><p>Real tools for real allowance, real choices, and real life.</p></div>
      <div className="feature-grid">{features.map((feature, index) => <Reveal key={feature.title} delay={(index % 3) * 80}><article className="feature-item"><span className={`feature-icon ${feature.tint}`}>{feature.icon}</span><h3>{feature.title}</h3><p>{feature.copy}</p></article></Reveal>)}</div>
    </section>
  )
}
