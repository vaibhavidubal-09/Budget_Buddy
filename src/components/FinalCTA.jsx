import { Link } from 'react-router-dom'

export default function FinalCTA() {
  return (
    <section className="final-cta" id="final-cta">
      <div className="cta-decoration cta-deco-one" /><div className="cta-decoration cta-deco-two" />
      <p className="eyebrow">A FRESH MONTH IS A FRESH START</p>
      <h2>Next month can<br /><em>be different.</em></h2>
      <p>Know where your money goes before it's gone.</p>
      <Link className="button cta-button" to="/register">Start my BudgetBuddy journey <span aria-hidden="true">&#8594;</span></Link>
      <small>Built for students. Designed for real life.</small>
    </section>
  )
}
