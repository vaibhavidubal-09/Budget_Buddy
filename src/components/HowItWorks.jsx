import Reveal from './Reveal.jsx'

const process = [
  { n: '01', title: 'Add your money', note: 'Start with your monthly allowance.' },
  { n: '02', title: 'Track spending', note: 'Log the big things and the tiny ones.' },
  { n: '03', title: 'Set your limits', note: 'Choose what feels right for each category.' },
  { n: '04', title: 'Understand habits', note: 'See the patterns in your month.' },
  { n: '05', title: 'Make smarter choices', note: 'Use what you learn next month.' },
]

export default function HowItWorks() {
  return (
    <section className="how-section section-pad" id="how-it-works">
      <div className="section-heading"><p className="eyebrow">A CLEARER MONTH, ONE STEP AT A TIME</p><h2>Five small steps.<br /><em>A whole new perspective.</em></h2></div>
      <div className="process-track">{process.map((item, index) => <Reveal key={item.n} delay={index * 75}><article className="process-step"><span className="process-number">{item.n}</span><div className="process-node">{index === process.length - 1 ? '✓' : item.n.slice(-1)}</div><h3>{item.title}</h3><p>{item.note}</p></article></Reveal>)}</div>
    </section>
  )
}
